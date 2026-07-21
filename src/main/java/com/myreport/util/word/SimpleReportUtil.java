package com.myreport.util.word;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.myreport.framework.redis.RedisFileStateUtil;
import com.myreport.framework.redis.RedisTemplate;
import com.myreport.util.Constant;
import com.myreport.util.FileUtil;
import com.myreport.util.word.common.*;
import com.myreport.vo.CreateReportVO;
import com.spire.doc.*;
import com.spire.doc.documents.*;
import com.spire.doc.fields.DocPicture;
import com.spire.doc.fields.Footnote;
import com.spire.doc.fields.TableOfContent;
import com.spire.doc.fields.TextRange;
import com.spire.doc.formatting.ParagraphFormat;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import java.awt.*;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SimpleReportUtil {
    private static Logger logger = Logger.getLogger(SimpleReportUtil.class);
    private static String strFileSvrPre = FileUtil.fileConfig.getFileSvrPrePath();
    private final static String SUB_FOLDER = "report/report/";
    private final static String strBasePathPre = "report";


    public static void createReport(JSONArray reportJsonArr, JSONObject overallSetting, CreateReportVO createReportVO) {
        Integer reportId = createReportVO.getReportId();
        String strDownloaderKey = String.format(Constant.RedisKey.REPORT_LIST, reportId);
        try {
            //文件后缀唯一表示符
            overallSetting.put("mergeFileToken", System.currentTimeMillis());
            overallSetting.put("strBasePathPre", "report");
            //创建临时文件夹
            makeTempAndReportDir(overallSetting);
            new Thread(() -> createSingleReport(reportJsonArr, overallSetting, createReportVO)).start();
        } catch (Exception e) {
            ExceptionUtil.collectProcessInformation(overallSetting, e, "word生成");
            RedisTemplate.delete(String.format(Constant.RedisKey.REPORT_LOCK, reportId));
            RedisFileStateUtil.fileUpdateProgress(strDownloaderKey, TimeUtil.getReportCreateTime(), TimeUtil.getConsumeTime(overallSetting), 0, "", 4);
            com.myreport.service.ManagedReportGenerateSync.onFailure(reportId, e.getMessage());
        }
    }

    /**
     * 生成单个报告
     */
    private static void createSingleReport(JSONArray reportJsonArr, JSONObject overallSetting, CreateReportVO createReportVO) {
        //文件后缀唯一表示符
        String mergeFileToken = overallSetting.getString("mergeFileToken");
        //临时文件
        String basePath = CommonUtil.getBasePath(overallSetting);
        Integer reportId = createReportVO.getReportId();
        String reportName = createReportVO.getReportName();
        String tempFilePath = basePath + "temp/temReport" + mergeFileToken + ".docx";
        String reportFileName = reportName + mergeFileToken + ".docx";
        String finalFilePath = basePath + "report/" + reportFileName;
        //添加进度条
        addFileProcess(createReportVO, finalFilePath, overallSetting);
        String strDownloaderKey = String.format(Constant.RedisKey.REPORT_LIST, reportId);
        Document document = null;
        URL templateUrl = SimpleReportUtil.class.getClassLoader().getResource(WordContant.template.SIMPLE_REPORT_TEMPLATE);
        //生成报告
        try (FileInputStream fis = new FileInputStream(new File(templateUrl.toURI()))) {
            document = new Document(fis);
            doCreateReport(document, reportJsonArr, overallSetting, createReportVO);
            document.saveToFile(tempFilePath);
            document.dispose();
            WordUtil.reword(tempFilePath, finalFilePath, overallSetting);
            RedisFileStateUtil.fileUpdateProgress(strDownloaderKey, TimeUtil.getReportCreateTime(), TimeUtil.getConsumeTime(overallSetting), 0, "", 2);
            com.myreport.service.ManagedReportGenerateSync.onSuccess(reportId, finalFilePath);
            com.myreport.service.SimpleReportGenerateSync.onSuccess(reportId, finalFilePath);
        } catch (Exception e) {
            ExceptionUtil.collectProcessInformation(overallSetting, e, "word生成");
            RedisFileStateUtil.fileUpdateProgress(strDownloaderKey, TimeUtil.getReportCreateTime(), TimeUtil.getConsumeTime(overallSetting), 0, "", 4);
            com.myreport.service.ManagedReportGenerateSync.onFailure(reportId, e.getMessage());
            com.myreport.service.SimpleReportGenerateSync.onFailure(reportId, e.getMessage());
        } finally {
            RedisTemplate.delete(String.format(Constant.RedisKey.REPORT_LOCK, reportId));
            deleteTemFile(mergeFileToken, overallSetting);
            decrReportCreateCount();
            if (document != null) {
                document.close();
            }
        }
    }

    /**
     * 根据模板生成报告
     *
     * @param document
     * @param reportJsonArr 数据
     */
    public static void doCreateReport(Document document, JSONArray reportJsonArr, JSONObject overallSetting, CreateReportVO createReportVO) throws Exception {
        //设置封面
        //addConverImage(document, overallSetting);
        //添加正文
        addContents(document, reportJsonArr, overallSetting, 1, createReportVO);
        //添加底图
        //addBackCoverImage(document, overallSetting);
        //添加目录
        //addTableOfContents(document, overallSetting);
    }

    /**
     * 设置封面
     */
    private static void addConverImage(Document document, JSONObject overallSetting) {
        String strCoverImg = overallSetting.getString("strCoverImg");
        if (StringUtils.isEmpty(strCoverImg)) {
            return;
        }
        //分节
        Section section = document.getLastSection();
        insertFloatingImage(section, strCoverImg, overallSetting, "封面");
    }

    /**
     * 添加正文
     */
    private static void addContents(Document document, JSONArray reportJsonArr, JSONObject overallSetting, int level, CreateReportVO createReportVO) {
        //正文分节
        Section contentSection = document.addSection();
        //添加页眉
        addHeader(contentSection, overallSetting);
        //正文内容
        processNodeArray(contentSection, reportJsonArr, overallSetting, level, createReportVO);
        //添加页脚
        addFooter(contentSection, PageNumberStyle.Arabic);
    }

    /**
     * 设置底图
     */
    private static void addBackCoverImage(Document document, JSONObject overallSetting) {
        String strBackCoverImage = overallSetting.getString("strBackCoverImg");
        if (StringUtils.isEmpty(strBackCoverImage)) {
            return;
        }
        //分节
        Section section = document.addSection();
        insertFloatingImage(section, strBackCoverImage, overallSetting, "底图");
    }

    /**
     * 处理报告的章节节点
     *
     * @param contentSection Word文档
     * @param nodeArray      当前层级的JSONArray
     * @param level          当前标题层级（1=一级标题，2=二级...）
     */
    private static void processNodeArray(Section contentSection, JSONArray nodeArray, JSONObject overallSetting, int level, CreateReportVO createReportVO) {
        for (int i = 0; i < nodeArray.size(); i++) {
            Map<String, Integer> countMap = new HashMap<>();
            countMap.put("chartCount", 0);
            countMap.put("tableCount", 0);
            JSONObject itemJson = nodeArray.getJSONObject(i);
            String strTitle = itemJson.getString("strTitle");
            String strContent = itemJson.getString("strContent");
            String strNote = itemJson.getString("strNote");
            String strTips = itemJson.getString("strTips");
            Integer nTreeType = itemJson.getInteger("nTreeType");

            if (nTreeType == 0 && strTitle != null && !strTitle.trim().isEmpty()) {
                setMultiLevelTitle(contentSection, strTitle, strTips, level, overallSetting);
            }

            if (strContent != null && !strContent.trim().isEmpty()) {
                insertParagraph(contentSection, strContent, Color.BLACK, overallSetting);
            }

            if (nTreeType == 1) {
                insertData(contentSection, itemJson, overallSetting, countMap, i);
            }

            if (strNote != null && !strNote.trim().isEmpty()) {
                insertNote(contentSection, strNote);
            }

            JSONArray childNode = itemJson.getJSONArray("children");
            if (childNode != null && !childNode.isEmpty()) {
                doChildprocessNodeArray(contentSection, childNode, overallSetting, level + 1, i, countMap, createReportVO);
            }

            String strBackCoverImage = overallSetting.getString("strBackCoverImg");
            if (!(i == nodeArray.size() - 1 && StringUtils.isEmpty(strBackCoverImage))) {
                contentSection.addParagraph().appendBreak(BreakType.Page_Break);
            }
        }
    }

    private static void doChildprocessNodeArray(Section contentSection, JSONArray nodeArray, JSONObject overallSetting, int level, int passage, Map<String, Integer> countMap, CreateReportVO createReportVO) {
        for (int i = 0; i < nodeArray.size(); i++) {
            JSONObject itemJson = nodeArray.getJSONObject(i);
            String strTitle = itemJson.getString("strTitle");
            String strContent = itemJson.getString("strContent");
            String strNote = itemJson.getString("strNote");
            String strTips = itemJson.getString("strTips");
            Integer nTreeType = itemJson.getInteger("nTreeType");

            if (nTreeType == 0 && strTitle != null && !strTitle.trim().isEmpty()) {
                setMultiLevelTitle(contentSection, strTitle, strTips, level, overallSetting);
            }

            if (strContent != null && !strContent.trim().isEmpty()) {
                insertParagraph(contentSection, strContent, Color.BLACK, overallSetting);
            }

            if (nTreeType == 1) {
                insertData(contentSection, itemJson, overallSetting, countMap, passage);
            }

            if (strNote != null && !strNote.trim().isEmpty()) {
                insertNote(contentSection, strNote);
            }

            // 递归子节点
            JSONArray childNode = itemJson.getJSONArray("children");
            if (childNode != null && !childNode.isEmpty()) {
                doChildprocessNodeArray(contentSection, childNode, overallSetting, level + 1, passage, countMap, createReportVO);
            }
        }
    }

    /**
     * 插入图表
     */
    private static void insertData(Section section, JSONObject itemJson, JSONObject overallSetting, Map<String, Integer> countMap, int passage) {
        String strDownloaderKey = overallSetting.getString("strDownloaderKey");
        String strData = itemJson.getString("strData");
        JSONObject chartData = JSONObject.parseObject(strData);
        insertChartText(section, chartData, strData, overallSetting);
        //设置标题前缀
        setTitlePre(itemJson, overallSetting, countMap, passage);
        TemplateUtil.insertChartOrTable(section, itemJson, chartData, overallSetting);
        RedisFileStateUtil.fileUpdateProgress(strDownloaderKey, "", "", 1, "", 1);
    }

    /**
     * 插入图表文本：优先由统计分析师 Agent（qwen-plus）基于 strData 生成，失败则回退 strText。
     */
    private static void insertChartText(Section section, JSONObject dataObject, String strData, JSONObject overallSetting) {
        String strText = com.myreport.service.ChartStatAnalystAgent.analyze(strData);
        if (StringUtils.isBlank(strText) && dataObject != null) {
            strText = dataObject.getString("strText");
        }
        if (StringUtils.isNotBlank(strText)) {
            insertParagraph(section, strText.trim(), Color.BLACK, overallSetting);
        }
    }

    /**
     * 添加目录
     */
    private static void addTableOfContents(Document doc, JSONObject overallSetting) {
        String strCoverImage = overallSetting.getString("strCoverImg");
        Section section, tocSection;
        if (StringUtils.isEmpty(strCoverImage)) {
            tocSection = doc.getSections().get(0);
            section = doc.getSections().get(0);
        } else {
            section = doc.addSection();
            doc.getSections().insert(1, section);
            tocSection = doc.getSections().get(1);
        }
        //添加水印
        String strWatermarkImg = overallSetting.getString("strWatermarkImg");
        if (StringUtils.isNotEmpty(strWatermarkImg)) {
            addWatermarkImg(doc, strWatermarkImg, overallSetting);
        }
        //添加目录图片
        addTableOfContentsImage(tocSection, overallSetting, "目录图片");
        TableOfContent table = new TableOfContent(doc, "{\\o \"1-3\" \\h \\z \\u}");
        Paragraph paragraph = section.addParagraph();
        paragraph.getStyle().getCharacterFormat().setFontSize(12f);
        paragraph.appendTOC(1, 3);
        paragraph.getItems().add(table);
        paragraph.appendFieldMark(FieldMarkType.Field_Separator);
        paragraph.appendFieldMark(FieldMarkType.Field_End);
        try {
            doc.updateTableOfContents();
        } catch (Exception e) {
            ExceptionUtil.collectProcessInformation(overallSetting, e, "目录生成");
        }
        // 清除目录节页眉页脚，防止继承
        HeaderFooter tocHeader = tocSection.getHeadersFooters().getHeader();
        HeaderFooter tocFooter = tocSection.getHeadersFooters().getFooter();
        tocHeader.setLinkToPrevious(false);
        tocFooter.setLinkToPrevious(false);
        tocHeader.getChildObjects().clear();
        tocFooter.getChildObjects().clear();
        // 设置目录样式字体大小
        Style toc1Style = doc.getStyles().findByName("TOC1");
        if (toc1Style != null) {
            toc1Style.getCharacterFormat().setFontSize(12f);
        }
        Style toc2Style = doc.getStyles().findByName("TOC2");
        if (toc2Style != null) {
            toc2Style.getCharacterFormat().setFontSize(12f);
        }
        Style toc3Style = doc.getStyles().findByName("TOC3");
        if (toc3Style != null) {
            toc3Style.getCharacterFormat().setFontSize(12f);
        }
        String strHeader = overallSetting.getString("strHeader");
        String strHeaderImage = overallSetting.getString("strHeadImg");
        // 添加页眉图片或文字
        if (StringUtils.isNotEmpty(strHeaderImage)) {
            Paragraph headerImagePara = tocHeader.addParagraph();
            headerImagePara.applyStyle("报告页眉");
            try (InputStream imgInputStream = getImageInputStream(strHeaderImage)) {
                DocPicture headerImage = headerImagePara.appendPicture(imgInputStream);
                headerImage.setWidth(WordContant.IMAGE_FULL_WIDTH);
                headerImage.setHeight(20f);
            } catch (IOException e) {
                ExceptionUtil.collectProcessInformation(overallSetting, e, "目录页眉图");
            }
        } else if (StringUtils.isNotEmpty(strHeader)) {
            Paragraph headerTextPara = tocHeader.addParagraph();
            headerTextPara.appendText(strHeader);
            headerTextPara.getFormat().setHorizontalAlignment(HorizontalAlignment.Left);
            headerTextPara.getStyle().getCharacterFormat().setFontSize(12f);
        }
        // 设置目录页脚，罗马数字页码
        Paragraph footerParagraph = tocFooter.addParagraph();
        footerParagraph.applyStyle("报告页脚");
        footerParagraph.appendField("PAGE", FieldType.Field_Page);
        footerParagraph.getFormat().setHorizontalAlignment(HorizontalAlignment.Center);
        tocSection.getPageSetup().setPageNumberStyle(PageNumberStyle.Roman_Upper);
        tocSection.getPageSetup().setRestartPageNumbering(true);
        tocSection.getPageSetup().setPageStartingNumber(1);
        tocSection.getPageSetup().setDifferentFirstPageHeaderFooter(false);
        for (Object sec : doc.getSections()) {
            PageSetup itemPageSetup = ((Section) sec).getPageSetup();
            itemPageSetup.setGridType(GridPitchType.Chars_And_Line);
            itemPageSetup.setLinesPerPage(40);
        }
    }

    /**
     * 添加文本
     */
    public static void insertParagraph(Section section, String text, Color fontColor, JSONObject overallSetting) {
        if (StringUtils.isEmpty(text)) {
            return;
        }
        String[] parts = text.split("\n");
        for (String part : parts) {
            Paragraph para = section.addParagraph();
            para.getFormat().setHorizontalAlignment(HorizontalAlignment.Justify);
            // 先匹配花括号
            Pattern curlyPattern = Pattern.compile("\\{([^}]+)\\}");
            Matcher curlyMatcher = curlyPattern.matcher(part);
            int lastEnd = 0;
            while (curlyMatcher.find()) {
                // 处理花括号前的部分
                if (curlyMatcher.start() > lastEnd) {
                    String beforeText = part.substring(lastEnd, curlyMatcher.start());
                    TextUtil.appendWithNumberStyle(para, beforeText, "宋体", 12f, fontColor, false);
                }
                // 处理花括号内的部分
                String redText = curlyMatcher.group(1);
                try {
                    Map<String, String> fontConfig = TextUtil.getTextConfig(redText);
                    String configText = TextUtil.getOrDefaultIfBlank(fontConfig.get("text"), "");
                    Float fontSize = Float.valueOf(TextUtil.getOrDefaultIfBlank(fontConfig.get("size"), "12"));
                    String fontFamily = TextUtil.getOrDefaultIfBlank(fontConfig.get("font"), "宋体");
                    String color = TextUtil.getOrDefaultIfBlank(fontConfig.get("color"), "#0070C0");
                    Boolean isBold = "true".equals(fontConfig.getOrDefault("bold", "false"));
                    TextUtil.appendWithNumberStyle(para, configText, fontFamily, fontSize, Color.decode(color), isBold);
                    lastEnd = curlyMatcher.end();
                } catch (Exception e) {
                    ExceptionUtil.warning(section, "文本配置错误：" + redText);
                    ExceptionUtil.collectProcessInformation(overallSetting, e, "文本配置：" + redText);
                }
            }
            // 处理括号后的部分
            if (lastEnd < part.length()) {
                String afterText = part.substring(lastEnd);
                TextUtil.appendWithNumberStyle(para, afterText, "宋体", 12f, fontColor, false);
            }
            // 可选：应用段落样式
            para.applyStyle("报告正文");
        }
    }

    /**
     * 添加题注
     *
     * @param section Word文档对象
     * @param note    批准
     */
    private static void insertNote(Section section, String note) {
        Paragraph para = section.addParagraph();
        Pattern pattern = Pattern.compile("(\\d+\\.\\d+%?|\\d+%?|[-])");
        Matcher matcher = pattern.matcher(note);
        int lastEnd = 0;
        String strColor = "0070C0";
        TextRange run;
        while (matcher.find()) {
            // 非数字部分
            if (matcher.start() > lastEnd) {
                String text = note.substring(lastEnd, matcher.start());
                run = para.appendText(text);
                run.getCharacterFormat().setFontName("宋体 (中文正文)");
                run.getCharacterFormat().setFontSize(9f);
                run.getCharacterFormat().setTextColor(Color.decode("#" + strColor));
            }
            // 数字部分（用 Times New Roman）
            run = para.appendText(matcher.group());
            run.getCharacterFormat().setFontName("Times New Roman");
            run.getCharacterFormat().setTextColor(Color.decode("#" + strColor));
            lastEnd = matcher.end();
        }
        // 最后剩余部分
        if (lastEnd < note.length()) {
            String text = note.substring(lastEnd);
            run = para.appendText(text);
            run.getCharacterFormat().setFontName("宋体 (中文正文)");
            run.getCharacterFormat().setFontSize(9f);
            run.getCharacterFormat().setTextColor(Color.decode("#" + strColor));
        }
        para.applyStyle("报告图表标注");
    }

    /**
     * 添加图片页眉
     */
    private static void addHeader(Section headerSection, JSONObject overallSetting) {
        String strHeader = overallSetting.getString("strHeader");
        String strHeaderImage = overallSetting.getString("strHeadImg");

        if (StringUtils.isEmpty(strHeaderImage) && StringUtils.isEmpty(strHeader)) {
            return;
        }
        try {
            HeaderFooter header = headerSection.getHeadersFooters().getHeader();
            header.setLinkToPrevious(false);
            header.getChildObjects().clear();
            // 添加页眉图片或文字
            if (StringUtils.isNotEmpty(strHeaderImage)) {
                Paragraph headerImagePara = header.addParagraph();
                headerImagePara.applyStyle("报告页眉");
                try (InputStream imgInputStream = getImageInputStream(strHeaderImage)) {
                    DocPicture headerImage = headerImagePara.appendPicture(imgInputStream);
                    headerImage.setWidth(WordContant.IMAGE_FULL_WIDTH);
                    headerImage.setHeight(20f);
                } catch (IOException e) {
                    ExceptionUtil.collectProcessInformation(overallSetting, e, "页眉图");
                }
            } else if (StringUtils.isNotEmpty(strHeader)) {
                Paragraph headerTextPara = header.addParagraph();
                headerTextPara.appendText(strHeader);
                headerTextPara.getFormat().setHorizontalAlignment(HorizontalAlignment.Left);
                headerTextPara.getStyle().getCharacterFormat().setFontSize(12f);
            }
        } catch (Exception e) {
            ExceptionUtil.collectProcessInformation(overallSetting, e, "页眉图");
        }
    }

    /**
     * 添加页脚
     */
    private static void addFooter(Section section, PageNumberStyle style) {
        HeaderFooter footer = section.getHeadersFooters().getFooter();
        footer.setLinkToPrevious(false);
        footer.getChildObjects().clear();
        Paragraph footerParagraph = footer.addParagraph();
        footerParagraph.applyStyle("报告页脚");
        footerParagraph.appendField("PAGE", FieldType.Field_Page);
        footerParagraph.getFormat().setHorizontalAlignment(HorizontalAlignment.Center);
        section.getPageSetup().setRestartPageNumbering(true);
        section.getPageSetup().setPageStartingNumber(1);
    }

    /**
     * 设置多级标题
     *
     * @param contentSection Word文档对象
     * @param title          标题内容
     * @param level          标题级别（1 表示一级标题，2 表示二级标题，3 表示三级标题）
     */
    private static void setMultiLevelTitle(Section contentSection, String title, String strTips, int level, JSONObject overallSetting) {
        if (contentSection == null || StringUtils.isEmpty(title)) return;
        if (level == 1) {
            doSetMultiLevelTitle(contentSection, title, "报告一级", strTips, level, overallSetting);
        } else if (level == 2) {
            doSetMultiLevelTitle(contentSection, title, "报告二级", strTips, level, overallSetting);
        } else if (level == 3) {
            doSetMultiLevelTitle(contentSection, title, "报告三级", strTips, level, overallSetting);
        } else {
            doSetMultiLevelTitle(contentSection, title, "报告四级", strTips, level, overallSetting);
        }
    }

    /**
     * 设置多级标题
     */
    private static void doSetMultiLevelTitle(Section section, String title, String styleName, String strTips, int level, JSONObject overallSetting) {
        Paragraph titleParagraph = section.addParagraph();
        TextRange titleText = titleParagraph.appendText(title);
        if (level == 1) {
            setMultiLevelTitleBackground(titleParagraph, styleName, overallSetting);
        } else {
            titleParagraph.applyStyle(styleName);
        }
        JSONObject titleSetting = overallSetting.getJSONObject("strTitleSetting");
        String strColor = titleSetting != null ? titleSetting.getString("strColor") : "";
        if (!StringUtils.isEmpty(strColor) && level != 1) {
            titleText.getCharacterFormat().setTextColor(Color.decode("#" + strColor));
        }
        // 添加脚注（说明）
        if (!StringUtils.isEmpty(strTips)) {
            Footnote footnote = titleParagraph.appendFootnote(FootnoteType.Footnote);
            Paragraph footnotePara = footnote.getTextBody().addParagraph();
            footnotePara.appendText(strTips);
            footnotePara.applyStyle("报告题注");
        }
    }

    /**
     * 一级标题设置背景颜色
     */
    private static void setMultiLevelTitleBackground(Paragraph titleParagraph, String styleName, JSONObject overallSetting) {
        String strGlobalOneTitleColor = overallSetting.containsKey("strGlobalOneTitleColor") ? overallSetting.getString("strGlobalOneTitleColor") : "";
        if (StringUtils.isEmpty(strGlobalOneTitleColor) || "#FFFFFF".equals(strGlobalOneTitleColor)) {
            titleParagraph.applyStyle(styleName);
            return;
        }
        titleParagraph.applyStyle("报告一级背景标题");
        ParagraphFormat paraFormat = titleParagraph.getFormat();
        paraFormat.getBorders().getTop().setColor(Color.decode(strGlobalOneTitleColor));
        paraFormat.getBorders().getBottom().setColor(Color.decode(strGlobalOneTitleColor));
        paraFormat.getBorders().getLeft().setColor(Color.decode(strGlobalOneTitleColor));
        paraFormat.getBorders().getRight().setColor(Color.decode(strGlobalOneTitleColor));
        paraFormat.setBackColor(Color.decode(strGlobalOneTitleColor));
    }

    /**
     * 设置标题前缀
     */
    private static void setTitlePre(JSONObject itemJson, JSONObject overallSetting, Map<String, Integer> countMap, int passage) {
        setStaticTitlePre(itemJson, overallSetting, countMap, passage);
    }

    /**
     * 设置静态指标标题前缀
     */
    private static void setStaticTitlePre(JSONObject itemJson, JSONObject overallSetting, Map<String, Integer> countMap, int passage) {
        Integer nStartIndex = overallSetting.containsKey("nStartIndex") ? overallSetting.getInteger("nStartIndex") : 1;
        String displayType = itemJson.getString("displayType");
        if (displayType.equals("TABLE")) {
            countMap.merge("tableCount", 1, Integer::sum);
            if (passage >= nStartIndex) {
                overallSetting.put("strTitlePre", "表" + (passage - nStartIndex + 1) + "-" + countMap.get("tableCount") + "  ");
            } else {
                overallSetting.put("strTitlePre", "表" + countMap.get("tableCount") + "  ");
            }
        } else {
            countMap.merge("chartCount", 1, Integer::sum);
            if (passage >= nStartIndex) {
                overallSetting.put("strTitlePre", "图" + (passage - nStartIndex + 1) + "-" + countMap.get("chartCount") + "  ");
            } else {
                overallSetting.put("strTitlePre", "图" + countMap.get("chartCount") + "  ");
            }
        }
    }


    /**
     * 添加浮动图片
     */
    public static void insertFloatingImage(Section contentSection, String imgPath, JSONObject overallSetting, String text) {
        if (StringUtils.isEmpty(imgPath)) {
            return;
        }
        try (InputStream imageStream = getImageInputStream(imgPath)) {
            Paragraph para = contentSection.addParagraph();
            DocPicture picture = para.appendPicture(imageStream);
            picture.setTextWrappingStyle(TextWrappingStyle.Behind);
            picture.setTextWrappingType(TextWrappingType.Both);
            picture.setVerticalOrigin(VerticalOrigin.Top_Margin_Area);
            picture.setHorizontalAlignment(ShapeHorizontalAlignment.Center);
            picture.setWidth(611);
            picture.setHeight(842);
        } catch (Exception e) {
            ExceptionUtil.collectProcessInformation(overallSetting, e, text);
        }
    }

    /**
     * 获取图片InputStream
     */
    public static InputStream getImageInputStream(String imagePath) throws IOException {
        if (StringUtils.isBlank(imagePath)) {
            throw new IOException("image path empty");
        }
        // 模版封面/底图：走上传目录本地读，避免拼错 FileUtil 根路径，也避免对本机 HTTP 自取图失败
        File managed = com.myreport.config.ReportTemplateUploadConfig.resolveManagedImageFile(imagePath);
        if (managed != null) {
            if (!managed.isFile()) {
                throw new IOException("managed image not found: " + managed.getAbsolutePath()
                        + " (ref=" + imagePath + ")");
            }
            return new FileInputStream(managed);
        }
        if (imagePath.startsWith("http")) {
            URL url = new URL(encodeChineseInUrl(imagePath));
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestProperty("User-Agent", "Mozilla/5.0");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            int code = conn.getResponseCode();
            if (code >= 400) {
                throw new IOException("HTTP error: " + code + " for URL: " + imagePath);
            }
            return conn.getInputStream();
        } else {
            return new FileInputStream(strFileSvrPre + imagePath);
        }
    }

    private static String encodeChineseInUrl(String urlStr) throws MalformedURLException, UnsupportedEncodingException {
        URL url = new URL(urlStr);
        String protocol = url.getProtocol();
        String host = url.getHost();
        int port = url.getPort();
        String path = url.getPath();
        String query = url.getQuery();
        // 对 path 部分进行逐段编码
        String[] pathSegments = path.split("/");
        StringBuilder encodedPath = new StringBuilder();
        for (String segment : pathSegments) {
            if (!segment.isEmpty()) {
                encodedPath.append("/").append(URLEncoder.encode(segment, "UTF-8").replace("+", "%20")); // 保留空格为%20而不是+
            }
        }
        String finalUrl = protocol + "://" + host;
        if (port != -1) finalUrl += ":" + port;
        finalUrl += encodedPath.toString();
        if (query != null) finalUrl += "?" + query;
        return finalUrl;
    }


    /**
     * 创建temp和report文件夹
     */
    private static void makeTempAndReportDir(JSONObject overallSetting) {
        String basePath = CommonUtil.getBasePath(overallSetting);
        // 创建 temp 和 report 两个子目录
        File tempDir = new File(basePath + "temp/");
        File reportDir = new File(basePath + "report/");

        if (!tempDir.exists()) {
            tempDir.mkdirs();
        }
        if (!reportDir.exists()) {
            reportDir.mkdirs();
        }
    }

    /**
     * 添加水印图片
     */
    private static void addWatermarkImg(Document doc, String strWatermarkImg, JSONObject overallSetting) {
        try (InputStream imgStream = getImageInputStream(strWatermarkImg)) {
            // 设置图片水印
            PictureWatermark picWatermark = new PictureWatermark();
            picWatermark.setPicture(imgStream);
            picWatermark.setScaling(100);
            picWatermark.isWashout(false);
            doc.setWatermark(picWatermark);
        } catch (Exception e) {
            ExceptionUtil.collectProcessInformation(overallSetting, e, "水印图片");
            return;
        }
    }

    /**
     * 添加目录图片
     */
    public static void addTableOfContentsImage(Section section, JSONObject overallSetting, String text) {
        String imagePath = overallSetting.containsKey("strTocTopImg") ? overallSetting.getString("strTocTopImg") : "C:\\Users\\mark_\\Desktop\\92.png";
        if (StringUtils.isEmpty(imagePath)) {
            return;
        }
        Paragraph imgPara = section.addParagraph();
        imgPara.getFormat().setFirstLineIndent(0);
        imgPara.getFormat().setLeftIndent(0);
        imgPara.getFormat().setRightIndent(0);
        imgPara.getFormat().setHorizontalAlignment(HorizontalAlignment.Center);
        imgPara.getFormat().setAfterSpacing(18);
        try (InputStream imgStream = getImageInputStream(imagePath)) {
            DocPicture picture = imgPara.appendPicture(imgStream);
            picture.setTextWrappingType(TextWrappingType.Both);
            picture.setVerticalOrigin(VerticalOrigin.Top_Margin_Area);
            picture.setHorizontalAlignment(ShapeHorizontalAlignment.Center);
            Float imgWidthPx = picture.getWidth();
            int pageWidth = WordContant.IMAGE_FULL_WIDTH;
            float ratio = (float) pageWidth / imgWidthPx;
            int imgHeightPx = (int) (picture.getHeight() * ratio);
            picture.setWidth(pageWidth);
            picture.setHeight(imgHeightPx);
        } catch (Exception e) {
            ExceptionUtil.collectProcessInformation(overallSetting, e, text);
        }
    }

    /**
     * 添加进度条
     */
    private static void addFileProcess(CreateReportVO createReportVO, String finalFilePath, JSONObject overallSetting) {
        addFileProcess(createReportVO, finalFilePath, 1, overallSetting);
    }

    /**
     * 添加进度条
     */
    private static void addFileProcess(CreateReportVO createReportVO, String finalFilePath, Integer reportSize, JSONObject overallSetting) {
        String strRedisKey = String.format(Constant.RedisKey.REPORT_LIST, createReportVO.getReportId());
        Integer metrics = overallSetting.getInteger("nMetricsCount");
        int nMetricsCount = (metrics == null ? 1 : metrics) * reportSize;
        RedisFileStateUtil.fileProgress(strRedisKey, finalFilePath, "", 0, nMetricsCount, "", 1);
        overallSetting.put("strDownloaderKey", strRedisKey);
    }

    /**
     * 删除临时文件
     */
    private static void deleteTemFile(String mergeFileToken, JSONObject overallSetting) {
        String basePath = CommonUtil.getBasePath(overallSetting);
        String[] filePaths = {basePath + "temp/temp_file_" + mergeFileToken + ".docx", basePath + "temp/temReport" + mergeFileToken + ".docx"};
        for (String path : filePaths) {
            File file = new File(path);
            if (file.exists()) {
                boolean deleted = file.delete();
                if (!deleted) {
                    ExceptionUtil.collectProcessInformation(overallSetting, "删除失败: " + path);
                }
            }
        }
    }

    /**
     * 生成报告数量减去1
     */
    public static void decrReportCreateCount() {
        String serverId = "1";
        String redisRunningCountKey = String.format(Constant.RedisKey.REPORT_CREATE_EXECUTOR_COUNT, Integer.parseInt(serverId));
        RedisTemplate.decr(redisRunningCountKey);
    }
}