package com.myreport.service;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.myreport.entity.ReportTemplateNode;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 模版树 + 指标结果 → 生成引擎 reportJsonArr。
 */
@Component
public class ReportJsonAssembler {

    public static final class AssembleResult {
        private final JSONArray reportJsonArr;
        private final int metricCount;
        private final List<Map<String, Object>> metrics;
        private final Map<Long, String> parentIntroByMetricId;

        public AssembleResult(JSONArray reportJsonArr,
                              int metricCount,
                              List<Map<String, Object>> metrics,
                              Map<Long, String> parentIntroByMetricId) {
            this.reportJsonArr = reportJsonArr;
            this.metricCount = metricCount;
            this.metrics = metrics;
            this.parentIntroByMetricId = parentIntroByMetricId;
        }

        public JSONArray getReportJsonArr() {
            return reportJsonArr;
        }

        public int getMetricCount() {
            return metricCount;
        }

        public List<Map<String, Object>> getMetrics() {
            return metrics;
        }

        public Map<Long, String> getParentIntroByMetricId() {
            return parentIntroByMetricId;
        }
    }

    /**
     * 先收集 METRIC 列表与父 intro，供聚合；再在聚合结果上组装树。
     */
    public void collectMetrics(List<Map<String, Object>> templateNodes,
                               List<Map<String, Object>> outMetrics,
                               Map<Long, String> parentIntroByMetricId) {
        if (templateNodes == null) {
            return;
        }
        walkCollect(templateNodes, "", outMetrics, parentIntroByMetricId);
    }

    @SuppressWarnings("unchecked")
    private void walkCollect(List<Map<String, Object>> nodes,
                             String parentIntro,
                             List<Map<String, Object>> outMetrics,
                             Map<Long, String> parentIntroByMetricId) {
        for (Map<String, Object> node : nodes) {
            if (node == null) {
                continue;
            }
            String nodeType = node.get("nodeType") == null ? "" : String.valueOf(node.get("nodeType"));
            if (ReportTemplateNode.TYPE_TITLE.equals(nodeType)) {
                String intro = node.get("intro") == null ? "" : String.valueOf(node.get("intro"));
                Object childrenObj = node.get("children");
                if (childrenObj instanceof List) {
                    walkCollect((List<Map<String, Object>>) childrenObj, intro, outMetrics, parentIntroByMetricId);
                }
            } else if (ReportTemplateNode.TYPE_METRIC.equals(nodeType)) {
                Long id = toLong(node.get("id"));
                outMetrics.add(node);
                if (id != null) {
                    parentIntroByMetricId.put(id, parentIntro == null ? "" : parentIntro);
                }
            }
        }
    }

    /**
     * 用聚合结果组装引擎树；聚合失败的 METRIC 跳过。
     */
    @SuppressWarnings("unchecked")
    public AssembleResult assemble(List<Map<String, Object>> templateNodes,
                                   Map<Long, JSONObject> metricDataById) {
        List<Map<String, Object>> metrics = new ArrayList<Map<String, Object>>();
        Map<Long, String> intros = new HashMap<Long, String>();
        collectMetrics(templateNodes, metrics, intros);

        JSONArray roots = new JSONArray();
        int used = 0;
        if (templateNodes != null) {
            for (Map<String, Object> node : templateNodes) {
                JSONObject mapped = mapNode(node, metricDataById);
                if (mapped != null) {
                    roots.add(mapped);
                    used += countMetrics(mapped);
                }
            }
        }
        return new AssembleResult(roots, used, metrics, intros);
    }

    @SuppressWarnings("unchecked")
    private JSONObject mapNode(Map<String, Object> node, Map<Long, JSONObject> metricDataById) {
        if (node == null) {
            return null;
        }
        String nodeType = node.get("nodeType") == null ? "" : String.valueOf(node.get("nodeType"));
        String name = node.get("name") == null ? "" : String.valueOf(node.get("name"));

        if (ReportTemplateNode.TYPE_TITLE.equals(nodeType)) {
            JSONObject out = new JSONObject(true);
            out.put("nTreeType", 0);
            out.put("strTitle", name);
            String intro = node.get("intro") == null ? "" : String.valueOf(node.get("intro"));
            out.put("strContent", intro);
            out.put("strNote", "");
            out.put("strTips", "");
            JSONArray children = new JSONArray();
            Object childrenObj = node.get("children");
            if (childrenObj instanceof List) {
                for (Map<String, Object> child : (List<Map<String, Object>>) childrenObj) {
                    JSONObject mapped = mapNode(child, metricDataById);
                    if (mapped != null) {
                        children.add(mapped);
                    }
                }
            }
            out.put("children", children);
            return out;
        }

        if (ReportTemplateNode.TYPE_METRIC.equals(nodeType)) {
            Long id = toLong(node.get("id"));
            JSONObject data = id == null || metricDataById == null ? null : metricDataById.get(id);
            if (data == null) {
                return null;
            }
            String displayType = node.get("displayType") == null
                    ? ReportTemplateNode.DISPLAY_TABLE
                    : String.valueOf(node.get("displayType"));
            int strType = ReportTemplateNode.DISPLAY_CHART.equalsIgnoreCase(displayType) ? 2 : 1;

            JSONObject out = new JSONObject(true);
            out.put("nTreeType", 1);
            out.put("nType", 0);
            out.put("strTitle", name);
            out.put("strData", data.toJSONString());
            JSONArray showList = new JSONArray();
            JSONObject showItem = new JSONObject(true);
            showItem.put("strType", strType);
            showItem.put("bShow", true);
            showItem.put("bPreant", true);
            showList.add(showItem);
            out.put("strShowList", showList);
            out.put("children", new JSONArray());
            return out;
        }
        return null;
    }

    private int countMetrics(JSONObject node) {
        if (node == null) {
            return 0;
        }
        int n = 0;
        Integer treeType = node.getInteger("nTreeType");
        if (treeType != null && treeType == 1) {
            n = 1;
        }
        JSONArray children = node.getJSONArray("children");
        if (children != null) {
            for (int i = 0; i < children.size(); i++) {
                n += countMetrics(children.getJSONObject(i));
            }
        }
        return n;
    }

    private static Long toLong(Object v) {
        if (v == null) {
            return null;
        }
        if (v instanceof Number) {
            return ((Number) v).longValue();
        }
        try {
            String s = String.valueOf(v).trim();
            if (StringUtils.isBlank(s)) {
                return null;
            }
            return Long.parseLong(s);
        } catch (Exception e) {
            return null;
        }
    }
}
