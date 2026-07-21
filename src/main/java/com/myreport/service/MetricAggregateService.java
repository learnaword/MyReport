package com.myreport.service;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.myreport.entity.GradEmploymentRecord;
import com.myreport.entity.ReportTemplateNode;
import com.myreport.repository.GradEmploymentRecordRepository;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * 按学校就业数据与模版指标配置做 COUNT / PERCENT 聚合。
 */
@Service
public class MetricAggregateService {

    private static final Logger logger = Logger.getLogger(MetricAggregateService.class);

    private final GradEmploymentRecordRepository recordRepository;

    public MetricAggregateService(GradEmploymentRecordRepository recordRepository) {
        this.recordRepository = recordRepository;
    }

    /**
     * @param schoolId 学校
     * @param metrics  模版 METRIC 节点（Map：id/name/statField/valueFormat 等）
     * @param parentIntroByMetricId 指标 → 最近祖先 TITLE intro（用于 strText）
     * @return nodeId → 指标结果 JSON；算失败的节点不出现在 map 中
     */
    public Map<Long, JSONObject> aggregate(Long schoolId,
                                           List<Map<String, Object>> metrics,
                                           Map<Long, String> parentIntroByMetricId) {
        Map<Long, JSONObject> result = new LinkedHashMap<Long, JSONObject>();
        if (schoolId == null || metrics == null || metrics.isEmpty()) {
            return result;
        }
        List<GradEmploymentRecord> rows = recordRepository.findBySchoolId(schoolId);
        if (rows == null) {
            rows = Collections.emptyList();
        }

        for (Map<String, Object> metric : metrics) {
            if (metric == null) {
                continue;
            }
            Long nodeId = toLong(metric.get("id"));
            String name = metric.get("name") == null ? "" : String.valueOf(metric.get("name"));
            String statField = metric.get("statField") == null ? null : String.valueOf(metric.get("statField"));
            String valueFormat = metric.get("valueFormat") == null
                    ? ReportTemplateNode.FORMAT_COUNT
                    : String.valueOf(metric.get("valueFormat"));
            if (nodeId == null || StringUtils.isBlank(statField)) {
                logger.warn("skip metric without id/statField, name=" + name);
                continue;
            }
            Function<GradEmploymentRecord, String> extractor =
                    GradEmploymentStatFieldCatalog.resolveExtractor(statField);
            if (extractor == null) {
                logger.warn("skip metric unknown statField=" + statField + ", id=" + nodeId);
                continue;
            }

            Map<String, Long> groupCounts = new LinkedHashMap<String, Long>();
            long nonNullTotal = 0L;
            for (GradEmploymentRecord row : rows) {
                String raw = extractor.apply(row);
                if (StringUtils.isBlank(raw)) {
                    continue;
                }
                String key = raw.trim();
                nonNullTotal++;
                Long c = groupCounts.get(key);
                groupCounts.put(key, c == null ? 1L : c + 1L);
            }
            if (nonNullTotal == 0L || groupCounts.isEmpty()) {
                logger.warn("skip metric empty data, statField=" + statField + ", id=" + nodeId);
                continue;
            }

            boolean percent = ReportTemplateNode.FORMAT_PERCENT.equalsIgnoreCase(valueFormat);
            if (percent && nonNullTotal <= 0L) {
                continue;
            }

            List<Map.Entry<String, Long>> entries = new ArrayList<Map.Entry<String, Long>>(groupCounts.entrySet());
            Collections.sort(entries, new Comparator<Map.Entry<String, Long>>() {
                @Override
                public int compare(Map.Entry<String, Long> a, Map.Entry<String, Long> b) {
                    int byCount = Long.compare(b.getValue(), a.getValue());
                    if (byCount != 0) {
                        return byCount;
                    }
                    return a.getKey().compareTo(b.getKey());
                }
            });

            JSONArray tableList = new JSONArray();
            for (Map.Entry<String, Long> e : entries) {
                JSONArray row = new JSONArray();
                row.add(e.getKey());
                if (percent) {
                    BigDecimal pct = BigDecimal.valueOf(e.getValue())
                            .multiply(BigDecimal.valueOf(100))
                            .divide(BigDecimal.valueOf(nonNullTotal), 2, RoundingMode.HALF_UP);
                    row.add(pct.toPlainString() + "%");
                } else {
                    row.add(String.valueOf(e.getValue()));
                }
                tableList.add(row);
            }

            JSONArray headList = new JSONArray();
            JSONArray headRow = new JSONArray();
            headRow.add("名称层");
            headRow.add("数据");
            headList.add(headRow);

            // 与 tableList 同结构，首行放表头（headList[0]）
            JSONArray chartDataList = new JSONArray();
            chartDataList.add(headRow);
            for (int i = 0; i < tableList.size(); i++) {
                chartDataList.add(tableList.getJSONArray(i));
            }

            String intro = parentIntroByMetricId == null ? "" : parentIntroByMetricId.get(nodeId);
            if (intro == null) {
                intro = "";
            }

            JSONObject data = new JSONObject(true);
            data.put("strText", intro);
            data.put("strText2", "");
            data.put("headList", headList);
            data.put("chartDataList", chartDataList);
            data.put("tableList", tableList);
            data.put("cellHeadList", new JSONArray());
            data.put("title", name);
            data.put("cellTableList", new JSONArray());
            result.put(nodeId, data);
        }
        return result;
    }

    public static boolean isKnownStatField(String statField) {
        return GradEmploymentStatFieldCatalog.isKnown(statField);
    }

    private static Long toLong(Object v) {
        if (v == null) {
            return null;
        }
        if (v instanceof Number) {
            return ((Number) v).longValue();
        }
        try {
            return Long.parseLong(String.valueOf(v).trim());
        } catch (Exception e) {
            return null;
        }
    }
}
