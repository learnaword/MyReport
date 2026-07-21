package com.myreport.service;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.myreport.entity.SimpleReportBlock;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Component;

/**
 * 上游 JSON → 引擎 strData 结构。
 */
@Component
public class SimpleReportDataNormalizer {

    public JSONObject normalize(JSONObject raw, String title) {
        JSONObject data = unwrap(raw);
        JSONArray labels = data.getJSONArray("labels");
        JSONArray values = data.getJSONArray("values");
        if (labels == null || values == null) {
            throw new IllegalArgumentException("缺少 labels/values");
        }
        if (labels.size() == 0 || labels.size() != values.size()) {
            throw new IllegalArgumentException("labels/values 须等长且非空");
        }

        JSONArray head = new JSONArray();
        JSONArray headRow = new JSONArray();
        headRow.add("名称");
        headRow.add("数值");
        head.add(headRow);

        JSONArray table = new JSONArray();
        JSONArray chart = new JSONArray();
        chart.add(headRow);

        for (int i = 0; i < labels.size(); i++) {
            String label = labels.getString(i);
            if (label == null) {
                label = "";
            }
            String value = formatValue(values.get(i));
            JSONArray row = new JSONArray();
            row.add(label);
            row.add(value);
            table.add(row);
            chart.add(row);
        }

        JSONObject out = new JSONObject(true);
        out.put("strText", "");
        out.put("strText2", "");
        out.put("headList", head);
        out.put("tableList", table);
        out.put("chartDataList", chart);
        out.put("cellHeadList", new JSONArray());
        out.put("cellTableList", new JSONArray());
        out.put("title", StringUtils.defaultString(title));
        return out;
    }

    public JSONObject errorPlaceholder(String title, String reason) {
        JSONArray head = new JSONArray();
        JSONArray headRow = new JSONArray();
        headRow.add("名称");
        headRow.add("数值");
        head.add(headRow);

        JSONArray table = new JSONArray();
        JSONArray row = new JSONArray();
        row.add("错误");
        row.add("拉取失败: " + StringUtils.defaultString(reason));
        table.add(row);

        JSONArray chart = new JSONArray();
        chart.add(headRow);
        chart.add(row);

        JSONObject out = new JSONObject(true);
        out.put("strText", "本区块数据拉取失败，已占位继续。");
        out.put("strText2", "");
        out.put("headList", head);
        out.put("tableList", table);
        out.put("chartDataList", chart);
        out.put("cellHeadList", new JSONArray());
        out.put("cellTableList", new JSONArray());
        out.put("title", StringUtils.defaultString(title));
        return out;
    }

    private JSONObject unwrap(JSONObject raw) {
        if (raw == null) {
            throw new IllegalArgumentException("空 JSON");
        }
        if (raw.containsKey("labels") && raw.containsKey("values")) {
            return raw;
        }
        if (raw.containsKey("code")) {
            Integer code = raw.getInteger("code");
            if (code != null && code != 0) {
                throw new IllegalArgumentException("业务 code=" + code
                        + (raw.getString("message") == null ? "" : (": " + raw.getString("message"))));
            }
            Object data = raw.get("data");
            if (data instanceof JSONObject) {
                return (JSONObject) data;
            }
            throw new IllegalArgumentException("data 须为含 labels/values 的对象");
        }
        throw new IllegalArgumentException("响应须含 labels/values");
    }

    private static String formatValue(Object v) {
        if (v == null) {
            return "";
        }
        if (v instanceof Number) {
            Number n = (Number) v;
            if (n.doubleValue() == n.longValue()) {
                return String.valueOf(n.longValue());
            }
            return String.valueOf(n);
        }
        String s = String.valueOf(v).trim();
        if (StringUtils.isBlank(s)) {
            throw new IllegalArgumentException("values 含空值");
        }
        return s;
    }

    public static String toDisplayType(String renderStyle) {
        if (SimpleReportBlock.STYLE_TABLE.equalsIgnoreCase(renderStyle)) {
            return "TABLE";
        }
        return "CHART";
    }

    public static String toChartStyle(String renderStyle) {
        if (SimpleReportBlock.STYLE_BAR.equalsIgnoreCase(renderStyle)) {
            return "BAR";
        }
        if (SimpleReportBlock.STYLE_PIE.equalsIgnoreCase(renderStyle)) {
            return "PIE";
        }
        if (SimpleReportBlock.STYLE_LINE.equalsIgnoreCase(renderStyle)) {
            return "LINE";
        }
        return null;
    }
}
