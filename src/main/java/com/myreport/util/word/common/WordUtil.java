package com.myreport.util.word.common;

import com.alibaba.fastjson.JSONObject;
import com.myreport.util.word.SpireReportUtil;
import com.spire.doc.Body;
import com.spire.doc.DocumentObject;
import com.spire.doc.Section;
import com.spire.doc.documents.*;
import com.spire.doc.fields.TextBox;
import com.spire.doc.fields.TextRange;
import org.apache.commons.lang.StringUtils;
import org.apache.poi.xwpf.usermodel.IBodyElement;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTable;

import java.awt.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WordUtil {


    /**
     * 添加文本框
     */
    public static void insertTextbox(Section section, JSONObject overallSetting) {
        String strCoverText = overallSetting.getString("strCoverText");
        if (StringUtils.isEmpty(strCoverText)) {
            return;
        }
        Paragraph para = section.addParagraph();
        TextBox tb = createTextBox(para, 494.4f, 141.3f, overallSetting);
        String strReplaceText = strCoverText;
        //获取文字颜色、字体、大小配置
        Pattern curlyPattern = Pattern.compile("\\{([^}]+)\\}");
        Matcher curlyMatcher = curlyPattern.matcher(strReplaceText);
        if (curlyMatcher.find()) {
            strReplaceText = curlyMatcher.group(1);
        }
        Map<String, String> fontConfig = TextUtil.getTextConfig(strReplaceText);
        String strConfigText = TextUtil.getOrDefaultIfBlank(fontConfig.get("text"), "");
        Float fontSize = Float.valueOf(TextUtil.getOrDefaultIfBlank(fontConfig.get("size"), "12"));
        String fontFamily = TextUtil.getOrDefaultIfBlank(fontConfig.get("font"), "宋体");
        String color = TextUtil.getOrDefaultIfBlank(fontConfig.get("color"), "#0070C0");
        Boolean isBold = "true".equals(fontConfig.getOrDefault("bold", "false"));
        String[] parts = strConfigText.split("\\n");
        for (String part : parts) {
            Paragraph textPara = tb.getBody().addParagraph();
            TextUtil.appendWithNumberStyle(textPara, part, fontFamily, fontSize, Color.decode(color), false);
            textPara.getFormat().setHorizontalAlignment(HorizontalAlignment.Center);
            textPara.getFormat().setTextAlignment(TextAlignment.Center);
            textPara.applyStyle("封面文字");
        }
    }

    /**
     * 创建并配置文本框
     */
    private static TextBox createTextBox(Paragraph para, float width, float height, JSONObject overallSetting) {
        TextBox tb = para.appendTextBox(width, height);
        //垂直坐标
        float verticalPosition = 0F;
        if (overallSetting.containsKey("verticalPosition")) {
            String vp = overallSetting.getString("verticalPosition");
            if (vp != null && vp.trim().length() > 0) {
                verticalPosition = Float.parseFloat(vp);
            }
        }
        //水平坐标
        float horizontalPosition = 0F;
        if (overallSetting.containsKey("horizontalPosition")) {
            String hp = overallSetting.getString("horizontalPosition");
            if (hp != null && hp.trim().length() > 0) {
                horizontalPosition = Float.parseFloat(hp);
            }
        }
        tb.getFormat().setVerticalOrigin(VerticalOrigin.Page);
        tb.getFormat().setHorizontalOrigin(HorizontalOrigin.Page);
        tb.getFormat().setVerticalPosition(verticalPosition);
        tb.getFormat().setHorizontalPosition(horizontalPosition);
        tb.getFormat().setTextWrappingStyle(TextWrappingStyle.None);
        tb.getFormat().setNoLine(true);
        // 透明背景
        tb.setFillColor(null);
        return tb;
    }

    /**
     * 设置表格标题
     *
     * @param section
     * @param chartJson 数据
     */
    public static void setTitle(Section section, JSONObject chartJson, JSONObject overallSetting) {
        String strTitle = chartJson.containsKey("title") ? chartJson.getString("title") : "";
        setTitle(section, strTitle, overallSetting);
    }

    /**
     * 设置标题
     *
     * @param section
     * @param strTitle 数据
     */
    public static void setTitle(Section section, String strTitle, JSONObject overallSetting) {
        if (overallSetting == null || !overallSetting.containsKey("strTitlePre")) {
            return;
        }
        strTitle = overallSetting.getString("strTitlePre") + strTitle;
        Paragraph para = section.addParagraph();
        para.applyStyle("报告图表标题");
        para.getFormat().setHorizontalAlignment(HorizontalAlignment.Center);
        Pattern pattern = Pattern.compile("(\\d+|[-])");
        Matcher matcher = pattern.matcher(strTitle);
        int lastEnd = 0;
        String strColor = overallSetting.containsKey("strTitleColor") ? overallSetting.getString("strTitleColor") : "0070C0";
        TextRange run;
        while (matcher.find()) {
            // 非数字部分
            if (matcher.start() > lastEnd) {
                String text = strTitle.substring(lastEnd, matcher.start());
                run = para.appendText(text);
                run.getCharacterFormat().setFontName("黑体");
                run.getCharacterFormat().setFontSize(10.5f);
                run.getCharacterFormat().setBold(true);
                run.getCharacterFormat().setTextColor(Color.decode("#" + strColor));
            }
            // 数字部分（用 Times New Roman）
            run = para.appendText(matcher.group());
            run.getCharacterFormat().setFontName("Times New Roman");
            run.getCharacterFormat().setFontSize(10.5f);
            run.getCharacterFormat().setBold(true);
            run.getCharacterFormat().setTextColor(Color.decode("#" + strColor));
            lastEnd = matcher.end();
        }
        // 最后剩余部分
        if (lastEnd < strTitle.length()) {
            String text = strTitle.substring(lastEnd);
            run = para.appendText(text);
            run.getCharacterFormat().setFontName("黑体");
            run.getCharacterFormat().setFontSize(10.5f);
            run.getCharacterFormat().setBold(true);
            run.getCharacterFormat().setTextColor(Color.decode("#" + strColor));
        }
    }

    /**
     * 合并word
     */
    public static void merge(Section section, JSONObject overallSetting) {
        Long mergeFileToken = overallSetting.getLong("mergeFileToken");
        String basePath = CommonUtil.getBasePath(overallSetting);
        String templateFilePath = basePath + "temp/temp_file_" + mergeFileToken + ".docx";
        com.spire.doc.Document document2 = new com.spire.doc.Document(templateFilePath);
        for (Object sectionObj : (Iterable) document2.getSections()) {
            Section sec = (Section) sectionObj;
            for (Object docObj : (Iterable) sec.getBody().getChildObjects()) {
                DocumentObject obj = (DocumentObject) docObj;
                Body body = section.getBody();
                body.getChildObjects().add(obj.deepClone());
            }
        }
        document2.dispose();
        document2.close();
    }

    /**
     * 合并指定文件
     */
    public static void merge(Section section, String templateFilePath, JSONObject overallSetting) throws Exception {
        URL templateUrl = SpireReportUtil.class.getClassLoader().getResource("word/template/" + templateFilePath);
        com.spire.doc.Document document2 = new com.spire.doc.Document(new FileInputStream(new File(templateUrl.toURI())));
        for (Object sectionObj : (Iterable) document2.getSections()) {
            Section sec = (Section) sectionObj;
            for (Object docObj : (Iterable) sec.getBody().getChildObjects()) {
                DocumentObject obj = (DocumentObject) docObj;
                Body body = section.getBody();
                body.getChildObjects().add(obj.deepClone());
            }
        }
        document2.dispose();
        document2.close();
    }


    /**
     * 去除水印
     */
    public static void reword(String inputFilePath, String outputFilePath) {
        reword(inputFilePath, outputFilePath, new JSONObject());
    }

    /**
     * 去除水印
     */
    public static void reword(String inputFilePath, String outputFilePath, JSONObject overallSetting) {
        try (FileInputStream fis = new FileInputStream(inputFilePath); XWPFDocument document = new XWPFDocument(fis); FileOutputStream fos = new FileOutputStream(outputFilePath)) {
            if (document.getBodyElements().size() > 0) {
                document.removeBodyElement(0);
                String strCoverImage = overallSetting.containsKey("strCoverImg") ? overallSetting.getString("strCoverImg") : "";
                if (StringUtils.isEmpty(strCoverImage)) {
                    removeLeadingBlankPages(document);
                }
            }
            document.write(fos);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void removeLeadingBlankPages(XWPFDocument document) {
        List<IBodyElement> elements = document.getBodyElements();
        for (int i = 0; i < elements.size(); ) {
            IBodyElement element = elements.get(i);
            boolean shouldRemove = false;

            if (element instanceof XWPFParagraph) {
                XWPFParagraph para = (XWPFParagraph) element;
                String text = para.getText();

                boolean isBlank = (text == null || text.trim().isEmpty());
                boolean isPageBreak = para.isPageBreak();

                if (isBlank || isPageBreak) {
                    shouldRemove = true;
                }
            } else if (element instanceof XWPFTable) {
                // 空表格也删
                XWPFTable table = (XWPFTable) element;
                shouldRemove = table.getRows().isEmpty();
            }

            if (shouldRemove) {
                document.removeBodyElement(i);
            } else {
                break;
            }
        }
    }
}
