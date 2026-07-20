package com.myreport.util.word.common;

import com.spire.doc.documents.Paragraph;
import com.spire.doc.fields.TextRange;
import org.apache.commons.lang.StringUtils;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TextUtil {

    /**
     * 获取文字配置Map
     * {#"font:宋体,color:#000000,size:12,bold:true"文字}
     */
    public static Map<String, String> getTextConfig(String redText) {
        Map<String, String> result = new HashMap<>();
        // 默认配置
        result.put("size", "12");
        result.put("font", "宋体");
        result.put("color", "#0070C0");
        result.put("bold", "true");
        // 配置的字体和颜色
        String strFontFamily = getFontFamily(redText);
        redText = redText.replaceFirst("\"黑体\"", "").replaceFirst("'黑体'", "");
        result.put("font", strFontFamily);
        if (isWarning(redText)) {
            redText = redText.replaceFirst("!", "").replaceFirst("！", "");
            result.put("color", "#FF0000");
        }
        // 复杂指令解析
        if (redText.startsWith("#")) {
            redText = redText.substring(1).trim();
            // 找到第一个双引号对
            if (!redText.startsWith("\"")) {
                return result;
            }
            int endQuote = redText.indexOf("\"", 1);
            if (endQuote == -1) {
                return result;
            }
            String stylePart = redText.substring(1, endQuote);
            // 解析样式
            if (!stylePart.isEmpty()) {
                String[] pairs = stylePart.split(",");
                for (String pair : pairs) {
                    String[] kv = pair.split(":");
                    if (kv.length == 2) {
                        result.put(kv[0].trim(), kv[1].trim());
                    }
                }
                redText = redText.substring(endQuote + 1);
            }
        }
        result.put("text", redText);
        return result;
    }

    /**
     * 按数字拆分文本并设置样式
     *
     * @param para 段落
     * @param text 文本
     */
    public static void appendWithNumberStyle(Paragraph para, String text, String fontFamily, Float fontSize, Color fontColor, Boolean isBold) {
        if (StringUtils.isEmpty(text)) {
            return;
        }
        Pattern numPattern = Pattern.compile("(\\d+\\.\\d+%?|\\d+%?)");
        Matcher matcher = numPattern.matcher(text);
        int lastEnd = 0;
        while (matcher.find()) {
            // 非数字
            if (matcher.start() > lastEnd) {
                String nonNumeric = text.substring(lastEnd, matcher.start());
                TextRange run = para.appendText(nonNumeric);
                run.getCharacterFormat().setFontName(fontFamily);
                run.getCharacterFormat().setFontSize(fontSize);
                run.getCharacterFormat().setTextColor(fontColor);
                run.getCharacterFormat().setBold(isBold);
            }
            // 数字
            String number = matcher.group();
            TextRange numRun = para.appendText(number);
            numRun.getCharacterFormat().setFontName("Times New Roman");
            numRun.getCharacterFormat().setFontSize(fontSize);
            numRun.getCharacterFormat().setTextColor(fontColor);
            numRun.getCharacterFormat().setBold(isBold);
            lastEnd = matcher.end();
        }
        // 剩余非数字
        if (lastEnd < text.length()) {
            String remaining = text.substring(lastEnd);
            TextRange run = para.appendText(remaining);
            run.getCharacterFormat().setFontName(fontFamily);
            run.getCharacterFormat().setFontSize(fontSize);
            run.getCharacterFormat().setTextColor(fontColor);
            run.getCharacterFormat().setBold(isBold);
        }
    }

    /**
     * 获取字体
     */
    private static String getFontFamily(String redText) {
        if (redText != null && (redText.contains("\"黑体\"") || redText.contains("'黑体'"))) {
            return "黑体";
        }
        return "宋体";
    }

    /**
     * 是否加红色
     */
    private static boolean isWarning(String text) {
        if ((text != null && !text.isEmpty()) && (text.charAt(0) == '!' || text.charAt(0) == '！')) {
            return Boolean.TRUE;
        } else {
            return Boolean.FALSE;
        }
    }

    /**
     * 如果值为空，或不存在返回默认值
     */
    public static String getOrDefaultIfBlank(String value, String defaultValue) {
        if (value == null || value.trim().isEmpty()) {
            return defaultValue;
        }
        return value;
    }
}
