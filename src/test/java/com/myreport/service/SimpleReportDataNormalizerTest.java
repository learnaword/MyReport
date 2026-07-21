package com.myreport.service;

import com.alibaba.fastjson.JSONObject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SimpleReportDataNormalizerTest {

    private final SimpleReportDataNormalizer normalizer = new SimpleReportDataNormalizer();

    @Test
    void normalize_labelsValues() {
        JSONObject raw = new JSONObject();
        raw.put("labels", java.util.Arrays.asList("A", "B"));
        raw.put("values", java.util.Arrays.asList(1, 2));
        JSONObject out = normalizer.normalize(raw, "测试");
        assertEquals("测试", out.getString("title"));
        assertEquals(2, out.getJSONArray("tableList").size());
        assertEquals("A", out.getJSONArray("tableList").getJSONArray(0).getString(0));
        assertEquals("1", out.getJSONArray("tableList").getJSONArray(0).getString(1));
        assertTrue(out.getJSONArray("chartDataList").size() >= 3);
    }

    @Test
    void normalize_wrappedCodeData() {
        JSONObject data = new JSONObject();
        data.put("labels", java.util.Arrays.asList("X"));
        data.put("values", java.util.Arrays.asList(9));
        JSONObject raw = new JSONObject();
        raw.put("code", 0);
        raw.put("data", data);
        JSONObject out = normalizer.normalize(raw, "t");
        assertEquals(1, out.getJSONArray("tableList").size());
    }

    @Test
    void normalize_rejectsMismatch() {
        JSONObject raw = new JSONObject();
        raw.put("labels", java.util.Arrays.asList("A"));
        raw.put("values", java.util.Arrays.asList(1, 2));
        assertThrows(IllegalArgumentException.class, new org.junit.jupiter.api.function.Executable() {
            @Override
            public void execute() {
                normalizer.normalize(raw, "t");
            }
        });
    }

    @Test
    void errorPlaceholder_hasRow() {
        JSONObject out = normalizer.errorPlaceholder("块", "timeout");
        assertEquals(1, out.getJSONArray("tableList").size());
        assertTrue(out.getJSONArray("tableList").getJSONArray(0).getString(1).contains("timeout"));
    }
}
