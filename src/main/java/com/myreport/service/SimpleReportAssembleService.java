package com.myreport.service;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.myreport.entity.ReportTemplateNode;
import com.myreport.entity.SimpleReportBlock;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Component;

/**
 * 简化报告区块 → reportJsonArr（扁平 TITLE + METRIC）。
 */
@Component
public class SimpleReportAssembleService {

    public static final class AssembleResult {
        private final JSONArray reportJsonArr;
        private final int metricCount;
        private final int warnCount;
        private final int successCount;

        public AssembleResult(JSONArray reportJsonArr, int metricCount, int warnCount, int successCount) {
            this.reportJsonArr = reportJsonArr;
            this.metricCount = metricCount;
            this.warnCount = warnCount;
            this.successCount = successCount;
        }

        public JSONArray getReportJsonArr() {
            return reportJsonArr;
        }

        public int getMetricCount() {
            return metricCount;
        }

        public int getWarnCount() {
            return warnCount;
        }

        public int getSuccessCount() {
            return successCount;
        }
    }

    private final SimpleReportHttpFetcher httpFetcher;
    private final SimpleReportDataNormalizer normalizer;

    public SimpleReportAssembleService(SimpleReportHttpFetcher httpFetcher,
                                       SimpleReportDataNormalizer normalizer) {
        this.httpFetcher = httpFetcher;
        this.normalizer = normalizer;
    }

    public AssembleResult assembleFromSnapshot(JSONObject snapshot) {
        String reportName = snapshot.getString("name");
        if (StringUtils.isBlank(reportName)) {
            reportName = "简化报告";
        }
        JSONArray blocks = snapshot.getJSONArray("blocks");
        if (blocks == null || blocks.isEmpty()) {
            throw new IllegalArgumentException("快照无区块");
        }

        JSONObject titleNode = new JSONObject(true);
        titleNode.put("nTreeType", 0);
        titleNode.put("strTitle", reportName);
        titleNode.put("strContent", "");
        titleNode.put("strNote", "");
        titleNode.put("strTips", "");
        JSONArray children = new JSONArray();

        int warn = 0;
        int success = 0;
        for (int i = 0; i < blocks.size(); i++) {
            JSONObject block = blocks.getJSONObject(i);
            if (block == null) {
                continue;
            }
            String blockTitle = block.getString("title");
            if (StringUtils.isBlank(blockTitle)) {
                blockTitle = "区块" + (i + 1);
            }
            String renderStyle = block.getString("renderStyle");
            JSONObject strData;
            boolean ok = true;
            try {
                JSONObject raw = httpFetcher.fetch(
                        block.getString("httpMethod"),
                        block.getString("url"),
                        block.getString("queryJson"),
                        block.getString("bodyJson"));
                strData = normalizer.normalize(raw, blockTitle);
                success++;
            } catch (Exception e) {
                ok = false;
                warn++;
                strData = normalizer.errorPlaceholder(blockTitle, e.getMessage());
                renderStyle = SimpleReportBlock.STYLE_TABLE;
            }

            String displayType = SimpleReportDataNormalizer.toDisplayType(renderStyle);
            String chartStyle = SimpleReportDataNormalizer.toChartStyle(renderStyle);
            if (!ok) {
                displayType = ReportTemplateNode.DISPLAY_TABLE;
                chartStyle = null;
            }

            JSONObject metric = new JSONObject(true);
            metric.put("nTreeType", 1);
            metric.put("nType", 0);
            metric.put("strTitle", blockTitle);
            metric.put("strData", strData.toJSONString());
            metric.put("displayType", displayType);
            metric.put("chartStyle", chartStyle);
            metric.put("children", new JSONArray());
            children.add(metric);
        }

        if (children.isEmpty()) {
            throw new IllegalArgumentException("无可生成的区块");
        }
        if (success == 0) {
            throw new IllegalArgumentException("全部数据区块拉取失败");
        }

        titleNode.put("children", children);
        JSONArray roots = new JSONArray();
        roots.add(titleNode);
        return new AssembleResult(roots, children.size(), warn, success);
    }
}
