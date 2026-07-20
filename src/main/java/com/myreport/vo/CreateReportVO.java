package com.myreport.vo;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import java.util.HashMap;
import java.util.Map;

/**
 * 创建报告请求参数。
 */
public class CreateReportVO implements Cloneable {

    /** 报告 ID */
    private Integer reportId;

    /** 报告名称 */
    private String reportName;

    /** 学校 ID */
    private Long lSchoolId;

    /** 报告内容节点 JSON */
    private JSONArray reportJsonArr;

    /** 全局配置 */
    private JSONObject overallSetting;

    /** 扩展参数（兼容原 CDO 动态字段） */
    private Map<String, Object> attributes = new HashMap<String, Object>();

    public Integer getReportId() {
        return reportId;
    }

    public void setReportId(Integer reportId) {
        this.reportId = reportId;
    }

    public String getReportName() {
        return reportName;
    }

    public void setReportName(String reportName) {
        this.reportName = reportName;
    }

    public Long getLSchoolId() {
        return lSchoolId;
    }

    public void setLSchoolId(Long lSchoolId) {
        this.lSchoolId = lSchoolId;
    }

    public JSONArray getReportJsonArr() {
        return reportJsonArr;
    }

    public void setReportJsonArr(JSONArray reportJsonArr) {
        this.reportJsonArr = reportJsonArr;
    }

    public JSONObject getOverallSetting() {
        return overallSetting;
    }

    public void setOverallSetting(JSONObject overallSetting) {
        this.overallSetting = overallSetting;
    }

    public Map<String, Object> getAttributes() {
        return attributes;
    }

    public void setAttributes(Map<String, Object> attributes) {
        this.attributes = attributes != null ? attributes : new HashMap<String, Object>();
    }

    public void set(String key, Object value) {
        if ("reportId".equals(key)) {
            this.reportId = value == null ? null : Integer.valueOf(String.valueOf(value));
            return;
        }
        if ("reportName".equals(key)) {
            this.reportName = value == null ? null : String.valueOf(value);
            return;
        }
        if ("lSchoolId".equals(key)) {
            this.lSchoolId = value == null ? null : Long.valueOf(String.valueOf(value));
            return;
        }
        attributes.put(key, value);
    }

    public Object get(String key) {
        if ("reportId".equals(key)) {
            return reportId;
        }
        if ("reportName".equals(key)) {
            return reportName;
        }
        if ("lSchoolId".equals(key)) {
            return lSchoolId;
        }
        return attributes.get(key);
    }

    public boolean exists(String key) {
        if ("reportId".equals(key)) {
            return reportId != null;
        }
        if ("reportName".equals(key)) {
            return reportName != null;
        }
        if ("lSchoolId".equals(key)) {
            return lSchoolId != null;
        }
        return attributes.containsKey(key) && attributes.get(key) != null;
    }

    public void removeField(String key) {
        if ("reportId".equals(key)) {
            reportId = null;
            return;
        }
        if ("reportName".equals(key)) {
            reportName = null;
            return;
        }
        if ("lSchoolId".equals(key)) {
            lSchoolId = null;
            return;
        }
        attributes.remove(key);
    }

    public Integer getIntegerValue(String key) {
        Object value = get(key);
        if (value == null) {
            return null;
        }
        if (value instanceof Integer) {
            return (Integer) value;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return Integer.valueOf(String.valueOf(value));
    }

    public Long getLongValue(String key) {
        Object value = get(key);
        if (value == null) {
            return null;
        }
        if (value instanceof Long) {
            return (Long) value;
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return Long.valueOf(String.valueOf(value));
    }

    public String getStringValue(String key) {
        Object value = get(key);
        return value == null ? null : String.valueOf(value);
    }

    public void setStringValue(String key, String value) {
        set(key, value);
    }

    public void setIntegerValue(String key, Integer value) {
        set(key, value);
    }

    public void setLongValue(String key, Long value) {
        set(key, value);
    }

    @Override
    public CreateReportVO clone() {
        CreateReportVO copy = new CreateReportVO();
        copy.reportId = this.reportId;
        copy.reportName = this.reportName;
        copy.lSchoolId = this.lSchoolId;
        copy.reportJsonArr = this.reportJsonArr;
        copy.overallSetting = this.overallSetting;
        copy.attributes = new HashMap<String, Object>(this.attributes);
        return copy;
    }
}
