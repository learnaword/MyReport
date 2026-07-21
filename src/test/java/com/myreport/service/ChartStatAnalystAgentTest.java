package com.myreport.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ChartStatAnalystAgentTest {

    @Test
    void truncateToMaxChars_nullOrBlank_returnsNull() {
        assertNull(ChartStatAnalystAgent.truncateToMaxChars(null, 200));
        assertNull(ChartStatAnalystAgent.truncateToMaxChars("   ", 200));
    }

    @Test
    void truncateToMaxChars_withinLimit_keepsText() {
        assertEquals("简短分析", ChartStatAnalystAgent.truncateToMaxChars("  简短分析  ", 200));
    }

    @Test
    void truncateToMaxChars_overLimit_cutsToMax() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 250; i++) {
            sb.append("字");
        }
        String cut = ChartStatAnalystAgent.truncateToMaxChars(sb.toString(), 200);
        assertEquals(200, cut.length());
    }
}
