package com.myreport.util.word.common;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.myreport.util.word.SpireReportUtil;
import com.myreport.util.word.WordContant;
import com.myreport.util.word.common.table.SpireTableUtil;
import com.spire.doc.Section;
import com.spire.doc.ShapeHorizontalAlignment;
import com.spire.doc.documents.HorizontalAlignment;
import com.spire.doc.documents.Paragraph;
import com.spire.doc.documents.TextWrappingType;
import com.spire.doc.documents.VerticalOrigin;
import com.spire.doc.fields.DocPicture;
import com.myreport.util.FileUtil;
import org.apache.commons.lang.StringUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xddf.usermodel.chart.XDDFChartData;
import org.apache.poi.xddf.usermodel.chart.XDDFDataSource;
import org.apache.poi.xddf.usermodel.chart.XDDFDataSourcesFactory;
import org.apache.poi.xddf.usermodel.chart.XDDFNumericalDataSource;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xwpf.usermodel.XWPFChart;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlObject;
import org.openxmlformats.schemas.drawingml.x2006.chart.*;

import java.io.*;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TemplateUtil {

    private static String strFileSvrPre = FileUtil.fileConfig.getFileSvrPrePath();

    /**
     * 添加图表
     */
    public static void insertChartOrTable(Section section, JSONObject itemJson, JSONObject dataObject, JSONObject overallSetting) {
        String disPlayType = itemJson.getString("displayType");
        switch (disPlayType) {
            case "TABLE":
                createTable(section, itemJson, dataObject, overallSetting);
                break;
            case "CHART":
                insertTempalteChart(section, itemJson, dataObject, overallSetting);
                break;
        }
    }

    /**
     * 创建表带样式
     *
     * @param section
     * @param jsonData 数据
     */
    public static void createTable(Section section, JSONObject showItem, JSONObject jsonData, JSONObject overallSetting) {
        if (!validateTableData(section, overallSetting, jsonData)) {
            return;
        }
        SpireTableUtil.createTable(section, showItem, jsonData, overallSetting);
    }

    /**
     * 添加图片
     */
    private static void insertImage(Section section, JSONObject imageObject, JSONObject dataObject, JSONObject overallSetting) {
        String strTitle = dataObject.containsKey("title") ? dataObject.getString("title") : "";
        imageObject.put("strTitle", strTitle);
        if (imageObject != null && imageObject.containsKey("strUrl") && !imageObject.getString("strUrl").isEmpty()) {
            overallSetting.put("imageWidth", WordContant.IMAGE_FULL_WIDTH);
            try {
                addImage(section, imageObject, overallSetting, "指标（" + strTitle + "）：图");
            } catch (IOException e) {
                ExceptionUtil.warning(section, "获取图片异常！！");
                ExceptionUtil.collectProcessInformation(overallSetting, e, "指标（" + strTitle + "）：图");
                WordUtil.setTitle(section, strTitle, overallSetting);
            }
        }
    }


    /**
     * 添加图片
     */
    public static void addImage(Section section, JSONObject imageObject, JSONObject overallSetting, String text) throws IOException {
        String imagePath = imageObject.getString("strUrl");
        String strTitle = imageObject.getString("strTitle");

        Paragraph para = section.addParagraph();
        para.getFormat().setHorizontalAlignment(HorizontalAlignment.Center);
        para.applyStyle("报告正文无缩进");

        try (InputStream imgStream = getImageInputStream(imagePath)) {
            DocPicture picture = para.appendPicture(imgStream);
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
            return;
        }
        WordUtil.setTitle(section, strTitle, overallSetting);
    }


    private static Boolean validateTableData(Section section, JSONObject overallSetting, JSONObject jsonData) {
        if (jsonData.isEmpty() || jsonData.containsKey("errorInfo")) {
            String errorInfo = "该表数据为空";
            if (jsonData.containsKey("errorInfo")) {
                errorInfo += "，" + jsonData.getString("errorInfo");
            }
            ExceptionUtil.warning(section, errorInfo);
            return false;
        }

        if (!jsonData.containsKey("tableList") || !jsonData.containsKey("headList") || !jsonData.containsKey("cellHeadList") || !jsonData.containsKey("cellTableList")) {
            String errorInfo = "表数据结构不正确";
            if (jsonData.containsKey("errorInfo")) {
                errorInfo += "，" + jsonData.getString("errorInfo");
            }
            ExceptionUtil.warning(section, errorInfo);
            ExceptionUtil.collectProcessInformation(overallSetting, "指标（）：表：" + errorInfo);
            WordUtil.setTitle(section, jsonData, overallSetting);
            return false;
        }

        JSONArray tableList = jsonData.getJSONArray("tableList");
        if (tableList.isEmpty()) {
            ExceptionUtil.warning(section, "该表数据为空！！");
            String strTitle = jsonData.containsKey("title") ? jsonData.getString("title") : "";
            ExceptionUtil.collectProcessInformation(overallSetting, "指标（" + strTitle + "）：表：" + "该表数据为空！！");
            return false;
        }

        return true;
    }


    /**
     * 添加模板图表
     */
    private static void insertTempalteChart(Section section, JSONObject showItem, JSONObject dataObject, JSONObject overallSetting) {
        JSONArray dataArr = dataObject.getJSONArray("chartDataList");
        String strTitle = dataObject.containsKey("title") ? dataObject.getString("title") : "";
        overallSetting.put("insertChartTitle", strTitle);
        setChartexample(showItem);
        doInsertTempalteChart(section, showItem, dataArr, overallSetting);
        WordUtil.setTitle(section, dataObject, overallSetting);
    }
    private static void setChartexample(JSONObject showItem) {
        String chartStyle = showItem.getString("chartStyle");
        if(chartStyle.equals("PIE")){
            showItem.put("lChartexample", 37);
        }else if(chartStyle.equals("BAR")){
            showItem.put("lChartexample", 23);
        }else{
            showItem.put("lChartexample", 3);
        }
    }

    /**
     * 添加模板图表
     */
    private static void doInsertTempalteChart(Section section, JSONObject showItem, JSONArray dataArr, JSONObject overallSetting) {
        //检测图表配置
        if (!validateChartConfig(section, showItem, overallSetting)) {
            return;
        }
        //获取图的配置信息
        Integer lChartexample = showItem.getInteger("lChartexample");
        String templateJson = WordContant.relationshipMap.get(lChartexample);
        JSONObject temlateObject = JSONObject.parseObject(templateJson);
        String templateFileName = getTemplateByDataSize(section, temlateObject, lChartexample, dataArr, overallSetting);
        if (StringUtils.isEmpty(templateFileName)) {
            return;
        }
        JSONArray templateConfig = temlateObject.getJSONArray("config");
        String strTitle = overallSetting.getString("insertChartTitle");
        String templateName = temlateObject.getString("name");
        Integer chartType = temlateObject.getInteger("type");
        try {
            if (chartType == WordContant.chartType.STATIC_CHART) {
                WordUtil.merge(section, templateFileName, overallSetting);
                return;
            }
            //检测图表数据
            if (!validateChart(section, dataArr, templateFileName, showItem, overallSetting)) {
                return;
            }
            //组合模板
            if (chartType == WordContant.chartType.COMBINATION_CHART) {
                fillCombinationTemplateData(templateConfig, templateFileName, dataArr, overallSetting);
                WordUtil.merge(section, overallSetting);
                return;
            }
            //散点图，字母饼图，复合条饼图特殊处理
            if (chartType == WordContant.chartType.SANDIAN_CHART) {
                fillBubbleChartTemplateData(templateFileName, dataArr, overallSetting);
                WordUtil.merge(section, overallSetting);
                return;
            }
            if (WordContant.BUBBLE_3D_CHART_TYPES.contains(lChartexample)) {
                Integer size = temlateObject.getInteger("size");
                fillBubble3DChartTemplateData(templateFileName, dataArr, overallSetting, size);
                WordUtil.merge(section, overallSetting);
                return;
            }
            //给模板填充数据
            if (!templateConfig.isEmpty()) {
                fillTemplateData(temlateObject, templateFileName, dataArr, overallSetting);
            } else {
                fillTemplateDynamicData(templateFileName, dataArr, overallSetting);
            }
        } catch (Exception e) {
            ExceptionUtil.collectProcessInformation(overallSetting, e, "指标（" + strTitle + "）:" + templateName + "：" + "数据填充");
            return;
        }
        //word合并
        WordUtil.merge(section, overallSetting);
    }

    /**
     * 获取模板，根据数据长度
     */
    private static String getTemplateByDataSize(Section section, JSONObject templateConfig, Integer chartId, JSONArray dataArr, JSONObject overallSetting) {
        Boolean isRowDynamic = templateConfig.containsKey("isRowDynamic") ? templateConfig.getBooleanValue("isRowDynamic") : false;
        Boolean isColDynamic = templateConfig.containsKey("isColDynamic") ? templateConfig.getBooleanValue("isColDynamic") : false;
        String templateFilePath = templateConfig.getString("template");
        String templateFileName = templateConfig.getString("name");
        if (isRowDynamic) {
            if (dataArr != null && dataArr.size() > 0) {
                templateFilePath = chartId + "/" + chartId + String.format("%03d", dataArr.size()) + ".docx";
                //检验模板是否存在
                URL templateUrl = SpireReportUtil.class.getClassLoader().getResource("word/template/" + templateFilePath);
                if (templateUrl == null) {
                    String chartTitle = overallSetting.getString("insertChartTitle");
                    ExceptionUtil.warning(section, templateFileName + "（" + templateFilePath + "）：数据无法匹配到模板，可能是指标配置错误！");
                    ExceptionUtil.collectProcessInformation(overallSetting, "指标（" + chartTitle + "）：" + templateFileName + "（" + templateFilePath + "）：数据无法匹配到模板，可能是指标配置错误！");
                    return "";
                }
            }
        }
        if (isColDynamic) {
            if (dataArr != null && dataArr.size() > 0) {
                templateFilePath = chartId + "/" + chartId + String.format("%03d", dataArr.getJSONArray(0).size()) + ".docx";
                //检验模板是否存在
                URL templateUrl = SpireReportUtil.class.getClassLoader().getResource("word/template/" + templateFilePath);
                if (templateUrl == null) {
                    String chartTitle = overallSetting.getString("insertChartTitle");
                    ExceptionUtil.warning(section, templateFileName + "（" + templateFilePath + "）：数据无法匹配到模板，可能是指标配置错误！");
                    ExceptionUtil.collectProcessInformation(overallSetting, "指标（" + chartTitle + "）：" + templateFileName + "（" + templateFilePath + "）：数据无法匹配到模板，可能是指标配置错误！");
                    return "";
                }
            }
        }
        return templateFilePath;
    }

    /**
     * 给模板填充数据
     */
    public static void fillTemplateData(JSONObject temlateObject, String templateFilePath, JSONArray jsonArrData, JSONObject overallSetting) throws Exception {
        String basePath = CommonUtil.getBasePath(overallSetting);
        URL templateUrl = SpireReportUtil.class.getClassLoader().getResource("word/template/" + templateFilePath);
        XWPFDocument doc = new XWPFDocument(new FileInputStream(new File(templateUrl.toURI())));
        JSONArray charRelation = temlateObject.getJSONArray("config");
        // 获取文档中的所有图表
        List<XWPFChart> charts = doc.getCharts();
        XWPFChart chart = charts.get(0);
        int dataSize = jsonArrData.size();
        HeightUtil.updateChartHeight(chart, temlateObject, jsonArrData, dataSize);
        CoordinateUtil.updateChartCoordinate(chart, temlateObject, jsonArrData);
        XSSFWorkbook workbook = new XSSFWorkbook();
        DataFormat format = workbook.createDataFormat();
        CellStyle percentageStyle = workbook.createCellStyle();
        percentageStyle.setDataFormat(format.getFormat("0.00%"));
        XSSFSheet sheet = workbook.createSheet("Sheet1");
        // 把 JSON 数据写入 Excel 表格
        for (int j = 0; j < dataSize; j++) {
            JSONArray data = jsonArrData.getJSONArray(j);
            XSSFRow row = sheet.createRow(j);
            for (int k = 0; k < data.size(); k++) {
                String strItem = data.getString(k);
                Cell itemCell = row.createCell(k);
                setCell(strItem, itemCell, percentageStyle);
            }
        }
        chart.setWorkbook(workbook);
        List<XDDFChartData> chartDatas = chart.getChartSeries();
        for (int i = 0; i < chartDatas.size(); i++) {
            XDDFChartData chartData = chartDatas.get(i);
            List<XDDFChartData.Series> seriesList = chartData.getSeries();
            // 根据映射关系更新每个系列数据
            for (int j = 0; j < seriesList.size(); j++) {
                XDDFChartData.Series series = seriesList.get(j);
                JSONObject relation = charRelation.getJSONArray(i).getJSONObject(j);
                Integer catCol = relation.getInteger("category");
                Integer valCol = relation.getInteger("values");
                Integer extCol = relation.getInteger("extCol");
                Integer seriesType = relation.getInteger("type");
                Boolean updateTx = relation.getBooleanValue("updateTx");
                String valColText = jsonArrData.getJSONArray(1).getString(valCol);
                XDDFDataSource<String> cat = XDDFDataSourcesFactory.fromStringCellRange(sheet, new CellRangeAddress(1, dataSize - 1, catCol, catCol));
                XDDFNumericalDataSource<Double> val = XDDFDataSourcesFactory.fromNumericCellRange(sheet, new CellRangeAddress(1, dataSize - 1, valCol, valCol));
                if (StringUtils.isNotEmpty(valColText) && valColText.contains("%")) {
                    val.setFormatCode("0.00%");
                } else if (StringUtils.isNotEmpty(valColText) && isDecimal(valColText)) {
                    val.setFormatCode("0.00");
                } else {
                    val.setFormatCode("0");
                }
                series.replaceData(cat, val);
                //更新表头
                if (updateTx) {
                    String headerText = jsonArrData.getJSONArray(0).getString(valCol);
                    if (seriesType == WordContant.seriesType.LINE_SERIES) {
                        updateLineTx(chart, valCol, j, headerText);
                    } else if (seriesType == WordContant.seriesType.PIE_SERIES) {
                        updatePieTx(chart, valCol, j, headerText);
                    } else if (seriesType == WordContant.seriesType.BAR_SERIES) {
                        updateBarTx(chart, valCol, j, headerText);
                    } else if (seriesType == WordContant.seriesType.BAR_3D_SERIES) {
                        updateBar3DTx(chart, valCol, j, headerText);
                    }
                }
                //更新辅助列
                if (extCol != -1) {
                    if (seriesType == WordContant.seriesType.LINE_SERIES) {
                        updateLineExt(chart, j, extCol, jsonArrData);
                    } else if (seriesType == WordContant.seriesType.PIE_SERIES) {
                        updatePieExt(chart, j, extCol, jsonArrData);
                    } else if (seriesType == WordContant.seriesType.BAR_SERIES) {
                        updateBarExt(chart, j, extCol, jsonArrData);
                    } else if (seriesType == WordContant.seriesType.BAR_3D_SERIES) {
                        updateDoughnutExt(chart, j, extCol, jsonArrData);
                    }
                }
            }
            chart.plot(chartData);
        }
        String mergeFileToken = overallSetting.getString("mergeFileToken");
        String outFilePath = basePath + "temp/temp_file_" + mergeFileToken + ".docx";
        doc.write(new FileOutputStream(outFilePath));
        doc.close();
    }

    public static void fillTemplateDynamicData(String templateFilePath, JSONArray jsonArrData, JSONObject overallSetting) throws Exception {
        String basePath = CommonUtil.getBasePath(overallSetting);
        URL templateUrl = SpireReportUtil.class.getClassLoader().getResource("word/template/" + templateFilePath);
        XWPFDocument doc = new XWPFDocument(new FileInputStream(new File(templateUrl.toURI())));
        // 获取文档中的所有图表
        List<XWPFChart> charts = doc.getCharts();
        XWPFChart chart = charts.get(0);
        XSSFWorkbook workbook = new XSSFWorkbook();
        DataFormat format = workbook.createDataFormat();
        CellStyle percentageStyle = workbook.createCellStyle();
        percentageStyle.setDataFormat(format.getFormat("0.00%"));
        XSSFSheet sheet = workbook.createSheet("Sheet1");
        int dataSize = jsonArrData.size();
        // 把 JSON 数据写入 Excel 表格
        for (int j = 0; j < dataSize; j++) {
            JSONArray data = jsonArrData.getJSONArray(j);
            XSSFRow row = sheet.createRow(j);
            for (int k = 0; k < data.size(); k++) {
                String strItem = data.getString(k);
                Cell itemCell = row.createCell(k);
                setCell(strItem, itemCell, percentageStyle);
            }
        }
        chart.setWorkbook(workbook);
        List<XDDFChartData> chartDatas = chart.getChartSeries();
        int dataColSize = jsonArrData.getJSONArray(0).size() - 1;
        XDDFChartData chartData = chartDatas.get(0);
        int seriesSize = chartData.getSeries().size();
        for (int i = dataColSize; i < seriesSize; i++) {
            chartData.removeSeries(dataColSize);
        }
        for (int i = 1; i <= dataColSize; i++) {
            if (chartData.getSeries().size() < i) {
                continue;
            }
            XDDFChartData.Series series = chartData.getSeries().get(i - 1);
            String valColText = jsonArrData.getJSONArray(1).getString(i);
            XDDFDataSource<String> cat = XDDFDataSourcesFactory.fromStringCellRange(sheet, new CellRangeAddress(1, dataSize - 1, 0, 0));
            XDDFNumericalDataSource<Double> val = XDDFDataSourcesFactory.fromNumericCellRange(sheet, new CellRangeAddress(1, dataSize - 1, i, i));
            if (StringUtils.isNotEmpty(valColText) && valColText.contains("%")) {
                val.setFormatCode("0.00%");
            } else if (StringUtils.isNotEmpty(valColText) && isDecimal(valColText)) {
                val.setFormatCode("0.00");
            } else {
                val.setFormatCode("0");
            }
            series.replaceData(cat, val);
            String headerText = jsonArrData.getJSONArray(0).getString(i);
            updateBarTx(chart, i, i - 1, headerText);
        }
        chart.plot(chartData);
        String mergeFileToken = overallSetting.getString("mergeFileToken");
        String outFilePath = basePath + "temp/temp_file_" + mergeFileToken + ".docx";
        doc.write(new FileOutputStream(outFilePath));
        doc.close();
    }

    /**
     * 给组合模板填充数据
     */
    private static void fillCombinationTemplateData(JSONArray templateConfig, String templateFilePath, JSONArray dataArr, JSONObject overallSetting) throws Exception {
        String basePath = CommonUtil.getBasePath(overallSetting);
        URL templateUrl = SpireReportUtil.class.getClassLoader().getResource("word/template/" + templateFilePath);
        XWPFDocument doc = new XWPFDocument(new FileInputStream(new File(templateUrl.toURI())));
        List<XWPFChart> charts = doc.getCharts();
        for (int m = 0; m < charts.size(); m++) {
            JSONArray templateConfigItem = templateConfig.getJSONArray(m);
            XWPFChart chart = charts.get(m);
            JSONObject chartConfigItem = templateConfigItem.getJSONArray(0).getJSONObject(0);
            String fileDataType = chartConfigItem.getString("fillDataType");
            if (fileDataType.equals("normal")) {
                doFillCombinationNormalTemplateData(chart, templateConfigItem, dataArr, overallSetting);
            } else {
            }
        }
        String mergeFileToken = overallSetting.getString("mergeFileToken");
        String outFilePath = basePath + "temp/temp_file_" + mergeFileToken + ".docx";
        doc.write(new FileOutputStream(outFilePath));
        doc.close();
    }

    /**
     * 普通组合填充数据
     */
    private static void doFillCombinationNormalTemplateData(XWPFChart chart, JSONArray templateConfigItem, JSONArray jsonArrData, JSONObject overallSetting) throws XmlException {
        List<Integer> insertIndexList = getCategoryAndValueIndexList(templateConfigItem);
        // 获取文档中的所有图表
        int dataSize = jsonArrData.size();
        XSSFWorkbook workbook = new XSSFWorkbook();
        DataFormat format = workbook.createDataFormat();
        CellStyle percentageStyle = workbook.createCellStyle();
        percentageStyle.setDataFormat(format.getFormat("0.00%"));
        XSSFSheet sheet = workbook.createSheet("Sheet1");
        int rowNum = 0;
        // 把 JSON 数据写入 Excel 表格
        for (int j = 0; j < dataSize; j++) {
            JSONArray data = jsonArrData.getJSONArray(j);
            XSSFRow row = sheet.createRow(j);
            int cellNum = 0;
            Boolean isRemoveRow = true;
            for (int k = 0; k < data.size(); k++) {
                if (insertIndexList.contains(k)) {
                    String strItem = data.getString(k);
                    if (StringUtils.isNotEmpty(strItem)) {
                        Cell itemCell = row.createCell(cellNum);
                        setCell(strItem, itemCell, percentageStyle);
                        cellNum++;
                        isRemoveRow = false;
                    }
                }
            }
            if (isRemoveRow) {
                sheet.removeRow(row);
            } else {
                rowNum++;
            }
        }
        chart.setWorkbook(workbook);
        List<XDDFChartData> chartDatas = chart.getChartSeries();
        for (int i = 0; i < chartDatas.size(); i++) {
            XDDFChartData chartData = chartDatas.get(i);
            List<XDDFChartData.Series> seriesList = chartData.getSeries();
            // 根据映射关系更新每个系列数据
            for (int j = 0; j < seriesList.size(); j++) {
                XDDFChartData.Series series = seriesList.get(j);
                JSONObject relation = templateConfigItem.getJSONArray(i).getJSONObject(j);
                Integer catCol = relation.getInteger("category");
                Integer valCol = relation.getInteger("values");
                Integer extCol = relation.getInteger("extCol");
                Integer seriesType = relation.getInteger("type");
                Boolean updateTx = relation.getBooleanValue("updateTx");
                String valColText = jsonArrData.getJSONArray(1).getString(valCol);
                XDDFDataSource<String> cat = XDDFDataSourcesFactory.fromStringCellRange(sheet, new CellRangeAddress(1, rowNum - 1, catCol, catCol));
                XDDFNumericalDataSource<Double> val = XDDFDataSourcesFactory.fromNumericCellRange(sheet, new CellRangeAddress(1, rowNum - 1, valCol, valCol));
                if (StringUtils.isNotEmpty(valColText) && valColText.contains("%")) {
                    val.setFormatCode("0.00%");
                } else if (StringUtils.isNotEmpty(valColText) && isDecimal(valColText)) {
                    val.setFormatCode("0.00");
                } else {
                    val.setFormatCode("0");
                }
                series.replaceData(cat, val);
                //更新表头
                if (updateTx) {
                    String headerText = jsonArrData.getJSONArray(0).getString(valCol);
                    if (seriesType == WordContant.seriesType.LINE_SERIES) {
                        updateLineTx(chart, valCol, j, headerText);
                    } else if (seriesType == WordContant.seriesType.PIE_SERIES) {
                        updatePieTx(chart, valCol, j, headerText);
                    } else if (seriesType == WordContant.seriesType.BAR_SERIES) {
                        updateBarTx(chart, valCol, j, headerText);
                    } else if (seriesType == WordContant.seriesType.BAR_3D_SERIES) {
                        updateBar3DTx(chart, valCol, j, headerText);
                    }
                }
                //更新辅助列
                if (extCol != -1) {
                    if (seriesType == WordContant.seriesType.LINE_SERIES) {
                        updateLineExt(chart, j, extCol, jsonArrData);
                    } else if (seriesType == WordContant.seriesType.PIE_SERIES) {
                        updatePieExt(chart, j, extCol, jsonArrData);
                    } else if (seriesType == WordContant.seriesType.BAR_SERIES) {
                        updateBarExt(chart, j, extCol, jsonArrData);
                    } else if (seriesType == WordContant.seriesType.BAR_3D_SERIES) {
                        updateDoughnutExt(chart, j, extCol, jsonArrData);
                    }
                }
            }
            chart.plot(chartData);
        }
    }

    /**
     * 给Bubble3DChart填充数据
     */
    private static void fillBubble3DChartTemplateData(String templateFilePath, JSONArray jsonArrData, JSONObject overallSetting, int count) throws Exception {
        String basePath = CommonUtil.getBasePath(overallSetting);
        URL templateUrl = SpireReportUtil.class.getClassLoader().getResource("word/template/" + templateFilePath);
        XWPFDocument doc = new XWPFDocument(new FileInputStream(new File(templateUrl.toURI())));
        // 获取文档中的所有图表
        List<XWPFChart> charts = doc.getCharts();
        XWPFChart chart = charts.get(0);
        chart.getWorkbook();
        XSSFWorkbook workbook = new XSSFWorkbook();
        DataFormat format = workbook.createDataFormat();
        CellStyle percentageStyle = workbook.createCellStyle();
        percentageStyle.setDataFormat(format.getFormat("0.00%"));
        XSSFSheet sheet = workbook.createSheet("Sheet1");
        int dataSize = jsonArrData.size();
        // 把 JSON 数据写入 Excel 表格
        for (int j = 0; j < dataSize; j++) {
            JSONArray data = jsonArrData.getJSONArray(j);
            XSSFRow row = sheet.createRow(j);
            for (int k = 0; k < data.size(); k++) {
                String strItem = data.getString(k);
                Cell itemCell = row.createCell(k);
                setCell(strItem, itemCell, percentageStyle);
            }
        }
        chart.setWorkbook(workbook);
        count = Math.min(dataSize - 1, count);
        CTChart ctChart = chart.getCTChart();
        CTPlotArea area = ctChart.getPlotArea();
        CTOfPieChart[] ofPieCharts = area.getOfPieChartArray();
        ofPieCharts[0].getSplitPos().setVal(count - 1);
        CTPieSer ctPieSer = ofPieCharts[0].getSerArray(0);
        CTAxDataSource ctAxDataSource = ctPieSer.getCat();
        CTNumDataSource ctNumDataSource = ctPieSer.getVal();
        String newCtAxDataSourceStr = "<c:strRef xmlns:c=\"http://schemas.openxmlformats.org/drawingml/2006/chart\" xmlns:a=\"http://schemas.openxmlformats.org/drawingml/2006/main\" xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\" xmlns:c16r2=\"http://schemas.microsoft.com/office/drawing/2015/06/chart\">\n" + "  <c:f>Sheet1!$A$2:$A$" + (count + 1) + "</c:f>\n" + "  <c:strCache>\n" + "    <c:ptCount val=\"" + count + "\"/>\n";
        for (int i = 0; i < count; i++) {
            newCtAxDataSourceStr += "    <c:pt idx=\"" + i + "\">\n" + "      <c:v>" + jsonArrData.getJSONArray(i + 1).get(0) + "</c:v>\n" + "    </c:pt>\n";
        }
        newCtAxDataSourceStr += "  </c:strCache>\n" + "</c:strRef>";
        XmlObject newCtAxDataSourceStrXml = XmlObject.Factory.parse(newCtAxDataSourceStr);
        ctAxDataSource.set(newCtAxDataSourceStrXml);
        String newCtNumDataSourceStr = "<c:numRef xmlns:c=\"http://schemas.openxmlformats.org/drawingml/2006/chart\" xmlns:a=\"http://schemas.openxmlformats.org/drawingml/2006/main\" xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\" xmlns:c16r2=\"http://schemas.microsoft.com/office/drawing/2015/06/chart\">\n" + "  <c:f>Sheet1!$B$2:$B$" + (count + 1) + "</c:f>\n" + "  <c:numCache>\n" + "    <c:formatCode>0.00%</c:formatCode>\n" + "    <c:ptCount val=\"" + count + "\"/>\n";
        for (int i = 0; i < count; i++) {
            newCtNumDataSourceStr += "    <c:pt idx=\"" + i + "\">\n" + "      <c:v>" + getDoubleValue(jsonArrData.getJSONArray(i + 1).get(1), overallSetting) + "</c:v>\n" + "    </c:pt>\n";
        }
        newCtNumDataSourceStr += "  </c:numCache>\n" + "</c:numRef>";
        XmlObject newCtNumDataSourceXml = XmlObject.Factory.parse(newCtNumDataSourceStr);
        ctNumDataSource.set(newCtNumDataSourceXml);

        String mergeFileToken = overallSetting.getString("mergeFileToken");
        String outFilePath = basePath + "temp/temp_file_" + mergeFileToken + ".docx";
        doc.write(new FileOutputStream(outFilePath));
        doc.close();
    }

    /**
     * 给BubbleChart填充数据
     */
    private static void fillBubbleChartTemplateData(String templateFilePath, JSONArray jsonArrData, JSONObject overallSetting) throws Exception {
        String basePath = CommonUtil.getBasePath(overallSetting);
        URL templateUrl = SpireReportUtil.class.getClassLoader().getResource("word/template/" + templateFilePath);
        XWPFDocument doc = new XWPFDocument(new FileInputStream(new File(templateUrl.toURI())));
        // 获取文档中的所有图表
        List<XWPFChart> charts = doc.getCharts();
        XWPFChart chart = charts.get(0);
        chart.getWorkbook();
        XSSFWorkbook workbook = new XSSFWorkbook();
        DataFormat format = workbook.createDataFormat();
        CellStyle percentageStyle = workbook.createCellStyle();
        percentageStyle.setDataFormat(format.getFormat("0.00%"));
        XSSFSheet sheet = workbook.createSheet("Sheet1");
        int dataSize = jsonArrData.size();
        // 把 JSON 数据写入 Excel 表格
        for (int j = 0; j < dataSize; j++) {
            JSONArray data = jsonArrData.getJSONArray(j);
            XSSFRow row = sheet.createRow(j);
            for (int k = 0; k < data.size(); k++) {
                String strItem = data.getString(k);
                Cell itemCell = row.createCell(k);
                setCell(strItem, itemCell, percentageStyle);
            }
        }
        chart.setWorkbook(workbook);
        CTChart ctChart = chart.getCTChart();
        CTBubbleSer ctBubbleSer = ctChart.getPlotArea().getBubbleChartArray(0).getSerArray(0);
        CTAxDataSource xVal = ctBubbleSer.getXVal();
        CTNumDataSource yVal = ctBubbleSer.getYVal();

        String newXValStr = "<c:strRef xmlns:c=\"http://schemas.openxmlformats.org/drawingml/2006/chart\" xmlns:a=\"http://schemas.openxmlformats.org/drawingml/2006/main\" xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\" xmlns:c16r2=\"http://schemas.microsoft.com/office/drawing/2015/06/chart\">\n" + "  <c:f>Sheet1!$A$2:$A$6</c:f>\n" + "  <c:strCache>\n" + "    <c:ptCount val=\"5\"/>\n";
        String newYValStr = "<c:numRef xmlns:c=\"http://schemas.openxmlformats.org/drawingml/2006/chart\" xmlns:a=\"http://schemas.openxmlformats.org/drawingml/2006/main\" xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\" xmlns:c16r2=\"http://schemas.microsoft.com/office/drawing/2015/06/chart\">\n" + "  <c:f>Sheet1!$B$2:$B$6</c:f>\n" + "  <c:numCache>\n" + "    <c:formatCode>0.00%</c:formatCode>\n" + "    <c:ptCount val=\"5\"/>\n";
        int limit = Math.min(dataSize - 1, 5);
        for (int i = 0; i < limit; i++) {
            newXValStr += "    <c:pt idx=\"" + i + "\">\n" + "      <c:v>" + jsonArrData.getJSONArray(i + 1).get(0) + "</c:v>\n" + "    </c:pt>\n";
            newYValStr += "    <c:pt idx=\"" + i + "\">\n" + "      <c:v>" + getDoubleValue(jsonArrData.getJSONArray(i + 1).get(1), overallSetting) + "</c:v>\n" + "    </c:pt>\n";
        }
        newXValStr += "  </c:strCache>\n" + "</c:strRef>";
        newYValStr += "  </c:numCache>\n" + "</c:numRef>";
        XmlObject newXValXml = XmlObject.Factory.parse(newXValStr);
        xVal.set(newXValXml);
        XmlObject newYValXml = XmlObject.Factory.parse(newYValStr);
        yVal.set(newYValXml);
        String mergeFileToken = overallSetting.getString("mergeFileToken");
        String outFilePath = basePath + "temp/temp_file_" + mergeFileToken + ".docx";
        doc.write(new FileOutputStream(outFilePath));
        doc.close();
    }

    /**
     * 获取图片InputStream
     */
    public static InputStream getImageInputStream(String imagePath) throws IOException {
        if (StringUtils.isBlank(imagePath)) {
            throw new IOException("image path empty");
        }
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
     * 检测图表配置
     */
    private static boolean validateChartConfig(Section section, JSONObject showItem, JSONObject overallSetting) {
        String chartTitle = overallSetting.getString("insertChartTitle");
        if (!showItem.containsKey("lChartexample")) {
            ExceptionUtil.warning(section, "没有配置图表ID！！");
            ExceptionUtil.collectProcessInformation(overallSetting, "指标（" + chartTitle + "）：" + "没有配置图表ID！！");
            return false;
        }
        Integer lChartexample = showItem.getInteger("lChartexample");
        if (!WordContant.relationshipMap.containsKey(lChartexample)) {
            ExceptionUtil.warning(section, "图表模板不存在！！");
            ExceptionUtil.collectProcessInformation(overallSetting, "指标（" + chartTitle + "）：" + "图表模板不存在！！");
            return false;
        }
        return true;
    }

    private static boolean validateChart(Section section, JSONArray dataArr, String templateFilePath, JSONObject showItem, JSONObject overallSetting) {
        Integer lChartexample = showItem.getInteger("lChartexample");
        //获取图的配置信息
        String templateJson = WordContant.relationshipMap.get(lChartexample);
        JSONObject temlateObject = JSONObject.parseObject(templateJson);
        String templateFileName = temlateObject.getString("word/template");
        JSONArray templateConfig = temlateObject.getJSONArray("config");
        Integer chartType = temlateObject.getInteger("type");
        String templateName = temlateObject.getString("name");
        overallSetting.put("insertTemplateName", templateName);
        String chartTitle = overallSetting.getString("insertChartTitle");
        if (dataArr == null || dataArr.size() < 2) {
            ExceptionUtil.warning(section, templateName + "：该指标数据为空！！");
            ExceptionUtil.collectProcessInformation(overallSetting, "指标（" + chartTitle + "）：" + templateName + "：" + "指标数据为空");
            return false;
        }
        int colSize = dataArr.getJSONArray(0).size();
        if (colSize < 2) {
            ExceptionUtil.warning(section, templateName + "：该指标缺少数据列！！");
            ExceptionUtil.collectProcessInformation(overallSetting, "指标（" + chartTitle + "）：" + templateName + "：" + "缺少数据列");
            return false;
        }
        if (chartType == WordContant.chartType.COMBINATION_CHART) {
            for (int i = 0; i < templateConfig.size(); i++) {
                JSONArray templateConfigItem = templateConfig.getJSONArray(i);
                if (!templateConfigItem.isEmpty()) {
                    if (!validateNormalData(section, templateConfigItem, templateFileName, dataArr, overallSetting)) {
                        return false;
                    }
                } else {
                    if (!checkColDataType(section, dataArr, 1, overallSetting)) {
                        return false;
                    }
                }
            }
        } else {
            if (!templateConfig.isEmpty()) {
                return validateNormalData(section, templateConfig, templateFileName, dataArr, overallSetting);
            } else {
                return checkColDataType(section, dataArr, 1, overallSetting);
            }
        }
        return true;
    }

    /**
     * 数据列是否存在和数据格式是否准确
     *
     * @param templateConfig
     * @param templateFileName
     * @param dataArr
     * @param overallSetting
     */
    private static Boolean validateNormalData(Section section, JSONArray templateConfig, String templateFileName, JSONArray dataArr, JSONObject overallSetting) {
        int configSize = templateConfig.size();
        for (int i = 0; i < configSize; i++) {
            JSONArray chartItemArr = templateConfig.getJSONArray(i);
            for (int j = 0; j < chartItemArr.size(); j++) {
                JSONObject configItem = chartItemArr.getJSONObject(j);
                Integer valCol = configItem.getInteger("values");
                Integer extCol = configItem.getInteger("extCol");
                if (configItem.containsKey("valueIndex")) {
                    valCol = configItem.getInteger("valueIndex");
                }
                String valueType = "";
                if (configItem.containsKey("valueType")) {
                    valueType = configItem.getString("valueType");
                }
                if (valueType.equals("string")) {
                    return true;
                }
                JSONArray firstRowData = dataArr.getJSONArray(1);
                if (firstRowData.size() < valCol + 1) {
                    String chartTitle = overallSetting.getString("insertChartTitle");
                    String templateName = overallSetting.getString("insertTemplateName");
                    ExceptionUtil.warning(section, "指标（" + chartTitle + "）：" + templateName + "：" + "缺少数据列");
                    ExceptionUtil.collectProcessInformation(overallSetting, "指标（" + chartTitle + "）：" + templateName + "：" + "缺少数据列");
                    return false;
                }
                if (extCol != -1 && firstRowData.size() < extCol + 1) {
                    String chartTitle = overallSetting.getString("insertChartTitle");
                    String templateName = overallSetting.getString("insertTemplateName");
                    ExceptionUtil.warning(section, "指标（" + chartTitle + "）：" + templateName + "：" + "缺少辅助列");
                    ExceptionUtil.collectProcessInformation(overallSetting, "指标（" + chartTitle + "）：" + templateName + "：" + "缺少辅助列");
                    return false;
                }
                if (!checkColDataType(section, dataArr, valCol, overallSetting)) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * 检查数据列格式
     *
     * @param dataArr
     * @param valCol
     */
    private static Boolean checkColDataType(Section section, JSONArray dataArr, Integer valCol, JSONObject overallSetting) {
        for (int i = 1; i < dataArr.size(); i++) {
            Object itemObject = dataArr.getJSONArray(i).get(valCol);
            if (!checkDoubleValue(section, itemObject, overallSetting)) {
                return false;
            }
        }
        return true;
    }

    /**
     * 检查是否是数字
     *
     * @param valueItem
     * @param overallSetting
     */
    private static boolean checkDoubleValue(Section section, Object valueItem, JSONObject overallSetting) {
        if (valueItem == null || (valueItem instanceof String && ((String) valueItem).trim().isEmpty())) {
            return true;
        }

        if (valueItem instanceof Number) {
            return true;
        } else if (valueItem instanceof String) {
            try {
                String valueStr = ((String) valueItem).replace("%", "").trim();
                if (StringUtils.isEmpty(valueStr)) {
                    return true;
                }
                new BigDecimal(valueStr);
                return true;
            } catch (Exception e) {
            }
        }
        // 非数字或非法字符串
        String chartTitle = overallSetting.getString("insertChartTitle");
        String templateName = overallSetting.getString("insertTemplateName");
        String info = "指标（" + chartTitle + "）：" + templateName + "：" + "value必须是数字类型 (Number/String)，当前值为： " + valueItem;
        ExceptionUtil.warning(section, info);
        ExceptionUtil.collectProcessInformation(overallSetting, info);
        return false;
    }

    /**
     * 转换为double
     */
    private static Double getDoubleValue(Object valueItem, JSONObject overallSetting) {
        if (valueItem == null || (valueItem instanceof String && ((String) valueItem).trim().isEmpty())) {
            return Double.NaN;
        }
        if (valueItem instanceof Double) {
            return (Double) valueItem;
        } else if (valueItem instanceof Integer) {
            return ((Integer) valueItem).doubleValue();
        } else if (valueItem instanceof Long) {
            return ((Long) valueItem).doubleValue();
        } else if (valueItem instanceof Float) {
            return ((Float) valueItem).doubleValue();
        } else if (valueItem instanceof BigDecimal) {
            return ((BigDecimal) valueItem).doubleValue();
        } else if (valueItem instanceof String) {
            String valueStr = (String) valueItem;
            if (valueStr.contains("%")) {
                valueStr = valueStr.replace("%", "").trim();
                BigDecimal percent = new BigDecimal(valueStr).divide(new BigDecimal(100));
                return percent.doubleValue();
            } else {
                return Double.parseDouble(valueStr);
            }
        } else {
            String chartTitle = overallSetting.getString("insertChartTitle");
            String templateName = overallSetting.getString("insertTemplateName");
            ExceptionUtil.collectProcessInformation(overallSetting, "指标（" + chartTitle + "）" + templateName + "：" + "valueItem 必须是数字类型 (Double, Integer, Long, Float, BigDecimal) 或 String 类型: " + valueItem);
        }
        return Double.NaN;
    }

    /**
     * 设置单元格数据和数据格式
     */
    private static void setCell(String strItem, Cell itemCell, CellStyle percentageStyle) {
        if (strItem == null || strItem.trim().isEmpty()) {
            itemCell.setCellValue("");
            return;
        }
        String value = strItem.trim();
        // 处理百分比：优先判断
        if (value.contains("%")) {
            try {
                value = value.replace("%", "").trim();
                BigDecimal percent = new BigDecimal(value).divide(new BigDecimal(100));
                itemCell.setCellValue(percent.doubleValue());
                itemCell.setCellStyle(percentageStyle);
                return;
            } catch (NumberFormatException e) {
            }
        }
        try {
            int intValue = Integer.parseInt(value);
            itemCell.setCellValue(intValue);
            return;
        } catch (NumberFormatException ignored) {
        }
        try {
            BigDecimal decimalValue = new BigDecimal(value);
            itemCell.setCellValue(decimalValue.doubleValue());
            return;
        } catch (NumberFormatException ignored) {
        }
        itemCell.setCellValue(strItem);
    }


    private static void updateBarTx(XWPFChart chart, Integer valCol, int serIndex, String headerText) throws XmlException {
        CTChart ctChart = chart.getCTChart();
        CTBarSer ser = ctChart.getPlotArea().getBarChartArray(0).getSerArray(serIndex);
        CTSerTx CTSerTx = ser.getTx();
        String newCtSerTx = "<c:strRef xmlns:c=\"http://schemas.openxmlformats.org/drawingml/2006/chart\" xmlns:a=\"http://schemas.openxmlformats.org/drawingml/2006/main\" xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\" xmlns:c16r2=\"http://schemas.microsoft.com/office/drawing/2015/06/chart\">";
        newCtSerTx += getCtxRangeFormula(valCol + 1);
        newCtSerTx += "  <c:strCache>\n" + "    <c:ptCount val=\"1\"/>\n" + "    <c:pt idx=\"0\">\n" + "      <c:v>" + headerText + "</c:v>\n" + "    </c:pt>\n" + "  </c:strCache>\n" + "</c:strRef>";
        XmlObject newCtSerTxXml = XmlObject.Factory.parse(newCtSerTx);
        CTSerTx.set(newCtSerTxXml);
    }

    private static void updateBar3DTx(XWPFChart chart, Integer valCol, int serIndex, String headerText) throws XmlException {
        CTChart ctChart = chart.getCTChart();
        CTBarSer ser = ctChart.getPlotArea().getBar3DChartArray(0).getSerArray(serIndex);
        CTSerTx CTSerTx = ser.getTx();
        String newCtSerTx = "<c:strRef xmlns:c=\"http://schemas.openxmlformats.org/drawingml/2006/chart\" xmlns:a=\"http://schemas.openxmlformats.org/drawingml/2006/main\" xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\" xmlns:c16r2=\"http://schemas.microsoft.com/office/drawing/2015/06/chart\">";
        newCtSerTx += getCtxRangeFormula(valCol + 1);
        newCtSerTx += "  <c:strCache>\n" + "    <c:ptCount val=\"1\"/>\n" + "    <c:pt idx=\"0\">\n" + "      <c:v>" + headerText + "</c:v>\n" + "    </c:pt>\n" + "  </c:strCache>\n" + "</c:strRef>";
        XmlObject newCtSerTxXml = XmlObject.Factory.parse(newCtSerTx);
        CTSerTx.set(newCtSerTxXml);
    }

    private static void updatePieTx(XWPFChart chart, int valCol, int serIndex, String txt) throws XmlException {
        CTChart ctChart = chart.getCTChart();
        CTPieSer ser = ctChart.getPlotArea().getPieChartArray(0).getSerArray(serIndex);
        CTSerTx CTSerTx = ser.getTx();
        String newCtSerTx = "<c:strRef xmlns:c=\"http://schemas.openxmlformats.org/drawingml/2006/chart\" xmlns:a=\"http://schemas.openxmlformats.org/drawingml/2006/main\" xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\" xmlns:c16r2=\"http://schemas.microsoft.com/office/drawing/2015/06/chart\">";
        newCtSerTx += getCtxRangeFormula(valCol + 1);
        newCtSerTx += "  <c:strCache>\n" + "    <c:ptCount val=\"1\"/>\n" + "    <c:pt idx=\"0\">\n" + "      <c:v>" + txt + "</c:v>\n" + "    </c:pt>\n" + "  </c:strCache>\n" + "</c:strRef>";
        XmlObject newCtSerTxXml = XmlObject.Factory.parse(newCtSerTx);
        CTSerTx.set(newCtSerTxXml);
    }

    private static void updateLineTx(XWPFChart chart, int valCol, int serIndex, String txt) throws XmlException {
        CTChart ctChart = chart.getCTChart();
        CTLineSer ser = ctChart.getPlotArea().getLineChartArray(0).getSerArray(serIndex);
        CTSerTx CTSerTx = ser.getTx();
        String newCtSerTx = "<c:strRef xmlns:c=\"http://schemas.openxmlformats.org/drawingml/2006/chart\" xmlns:a=\"http://schemas.openxmlformats.org/drawingml/2006/main\" xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\" xmlns:c16r2=\"http://schemas.microsoft.com/office/drawing/2015/06/chart\">";
        newCtSerTx += getCtxRangeFormula(valCol + 1);
        newCtSerTx += "  <c:strCache>\n" + "    <c:ptCount val=\"1\"/>\n" + "    <c:pt idx=\"0\">\n" + "      <c:v>" + txt + "</c:v>\n" + "    </c:pt>\n" + "  </c:strCache>\n" + "</c:strRef>";
        XmlObject newCtSerTxXml = XmlObject.Factory.parse(newCtSerTx);
        CTSerTx.set(newCtSerTxXml);
    }

    private static void updatePieExt(XWPFChart chart, int serIndex, int extCol, JSONArray dataArr) throws XmlException {
        int dataSize = dataArr.size();
        CTChart ctChart = chart.getCTChart();
        String ctExtensionStr = "";
        CTPieSer ser = ctChart.getPlotArea().getPieChartArray(0).getSerArray(serIndex);
        CTExtensionList ctExtensionList = ser.getExtLst();
        List<CTExtension> extensions = ctExtensionList.getExtList();
        ctExtensionStr = extensions.get(0).toString();
        String newDatalabelsRangeXml = "<c15:datalabelsRange xmlns:c15=\"http://schemas.microsoft.com/office/drawing/2012/chart\" " + "                   xmlns:c=\"http://schemas.openxmlformats.org/drawingml/2006/chart\">";
        newDatalabelsRangeXml += getDatalabelsRangeFormula(extCol + 1, dataSize);
        String newDlblRangeCacheXml = "<c15:dlblRangeCache xmlns:c15=\"http://schemas.microsoft.com/office/drawing/2012/chart\" " + "                   xmlns:c=\"http://schemas.openxmlformats.org/drawingml/2006/chart\">";
        newDlblRangeCacheXml += "<c:ptCount val=\"" + (dataSize - 1) + "\"/>";
        for (int m = 1; m < dataSize; m++) {
            JSONArray row = dataArr.getJSONArray(m);
            String value = row.getString(extCol);
            newDlblRangeCacheXml += "<c:pt idx=\"" + (m - 1) + "\"><c:v>" + value + "</c:v></c:pt>";
        }
        newDlblRangeCacheXml += "</c15:dlblRangeCache>";
        newDatalabelsRangeXml += newDlblRangeCacheXml + "</c15:datalabelsRange>";
        XmlObject newDatalabelsRange = XmlObject.Factory.parse(newDatalabelsRangeXml);
        XmlObject xmlFragment = XmlObject.Factory.parse(ctExtensionStr);
        XmlObject[] oldDlblRangeCache = xmlFragment.selectPath("declare namespace c15='http://schemas.microsoft.com/office/drawing/2012/chart'" + ".//c15:datalabelsRange");
        oldDlblRangeCache[0].set(newDatalabelsRange);
        extensions.remove(0);
        CTExtension ctExtension = ctExtensionList.insertNewExt(0);
        ctExtension.set(xmlFragment);
    }

    private static void updateLineExt(XWPFChart chart, int serIndex, int extCol, JSONArray dataArr) throws XmlException {
        int dataSize = dataArr.size();
        CTChart ctChart = chart.getCTChart();
        String ctExtensionStr = "";
        CTLineSer ser = ctChart.getPlotArea().getLineChartArray(0).getSerArray(serIndex);
        CTExtensionList ctExtensionList = ser.getExtLst();
        List<CTExtension> extensions = ctExtensionList.getExtList();
        ctExtensionStr = extensions.get(0).toString();
        String newDatalabelsRangeXml = "<c15:datalabelsRange xmlns:c15=\"http://schemas.microsoft.com/office/drawing/2012/chart\" " + "                   xmlns:c=\"http://schemas.openxmlformats.org/drawingml/2006/chart\">";
        newDatalabelsRangeXml += getDatalabelsRangeFormula(extCol + 1, dataSize);
        String newDlblRangeCacheXml = "<c15:dlblRangeCache xmlns:c15=\"http://schemas.microsoft.com/office/drawing/2012/chart\" " + "                   xmlns:c=\"http://schemas.openxmlformats.org/drawingml/2006/chart\">";
        newDlblRangeCacheXml += "<c:ptCount val=\"" + (dataSize - 1) + "\"/>";
        for (int m = 1; m < dataSize; m++) {
            JSONArray row = dataArr.getJSONArray(m);
            String value = row.getString(extCol);
            newDlblRangeCacheXml += "<c:pt idx=\"" + (m - 1) + "\"><c:v>" + value + "</c:v></c:pt>";
        }
        newDlblRangeCacheXml += "</c15:dlblRangeCache>";
        newDatalabelsRangeXml += newDlblRangeCacheXml + "</c15:datalabelsRange>";
        XmlObject newDatalabelsRange = XmlObject.Factory.parse(newDatalabelsRangeXml);
        XmlObject xmlFragment = XmlObject.Factory.parse(ctExtensionStr);
        XmlObject[] oldDlblRangeCache = xmlFragment.selectPath("declare namespace c15='http://schemas.microsoft.com/office/drawing/2012/chart'" + ".//c15:datalabelsRange");
        oldDlblRangeCache[0].set(newDatalabelsRange);
        extensions.remove(0);
        CTExtension ctExtension = ctExtensionList.insertNewExt(0);
        ctExtension.set(xmlFragment);
    }

    private static void updateBarExt(XWPFChart chart, int serIndex, int extCol, JSONArray dataArr) throws XmlException {
        int dataSize = dataArr.size();
        CTChart ctChart = chart.getCTChart();
        String ctExtensionStr = "";
        CTBarSer ser = ctChart.getPlotArea().getBarChartArray(0).getSerArray(serIndex);
        CTExtensionList ctExtensionList = ser.getExtLst();
        List<CTExtension> extensions = ctExtensionList.getExtList();
        ctExtensionStr = extensions.get(0).toString();
        String newDatalabelsRangeXml = "<c15:datalabelsRange xmlns:c15=\"http://schemas.microsoft.com/office/drawing/2012/chart\" " + "                   xmlns:c=\"http://schemas.openxmlformats.org/drawingml/2006/chart\">";
        newDatalabelsRangeXml += getDatalabelsRangeFormula(extCol + 1, dataSize);
        String newDlblRangeCacheXml = "<c15:dlblRangeCache xmlns:c15=\"http://schemas.microsoft.com/office/drawing/2012/chart\" " + "                   xmlns:c=\"http://schemas.openxmlformats.org/drawingml/2006/chart\">";
        newDlblRangeCacheXml += "<c:ptCount val=\"" + (dataSize - 1) + "\"/>";
        for (int m = 1; m < dataSize; m++) {
            JSONArray row = dataArr.getJSONArray(m);
            String value = row.getString(extCol);
            newDlblRangeCacheXml += "<c:pt idx=\"" + (m - 1) + "\"><c:v>" + value + "</c:v></c:pt>";
        }
        newDlblRangeCacheXml += "</c15:dlblRangeCache>";
        newDatalabelsRangeXml += newDlblRangeCacheXml + "</c15:datalabelsRange>";
        XmlObject newDatalabelsRange = XmlObject.Factory.parse(newDatalabelsRangeXml);
        XmlObject xmlFragment = XmlObject.Factory.parse(ctExtensionStr);
        XmlObject[] oldDlblRangeCache = xmlFragment.selectPath("declare namespace c15='http://schemas.microsoft.com/office/drawing/2012/chart'" + ".//c15:datalabelsRange");
        oldDlblRangeCache[0].set(newDatalabelsRange);
        extensions.remove(0);
        CTExtension ctExtension = ctExtensionList.insertNewExt(0);
        ctExtension.set(xmlFragment);
    }

    private static void updateDoughnutExt(XWPFChart chart, int serIndex, int extCol, JSONArray dataArr) throws XmlException {
        int dataSize = dataArr.size();
        CTChart ctChart = chart.getCTChart();
        String ctExtensionStr = "";
        CTPieSer ser = ctChart.getPlotArea().getDoughnutChartArray(0).getSerArray(serIndex);
        CTExtensionList ctExtensionList = ser.getExtLst();
        List<CTExtension> extensions = ctExtensionList.getExtList();
        ctExtensionStr = extensions.get(0).toString();
        String newDatalabelsRangeXml = "<c15:datalabelsRange xmlns:c15=\"http://schemas.microsoft.com/office/drawing/2012/chart\" " + "                   xmlns:c=\"http://schemas.openxmlformats.org/drawingml/2006/chart\">";
        newDatalabelsRangeXml += getDatalabelsRangeFormula(extCol + 1, dataSize);
        String newDlblRangeCacheXml = "<c15:dlblRangeCache xmlns:c15=\"http://schemas.microsoft.com/office/drawing/2012/chart\" " + "                   xmlns:c=\"http://schemas.openxmlformats.org/drawingml/2006/chart\">";
        newDlblRangeCacheXml += "<c:ptCount val=\"" + (dataSize - 1) + "\"/>";
        for (int m = 1; m < dataSize; m++) {
            JSONArray row = dataArr.getJSONArray(m);
            String value = row.getString(extCol);
            newDlblRangeCacheXml += "<c:pt idx=\"" + (m - 1) + "\"><c:v>" + value + "</c:v></c:pt>";
        }
        newDlblRangeCacheXml += "</c15:dlblRangeCache>";
        newDatalabelsRangeXml += newDlblRangeCacheXml + "</c15:datalabelsRange>";
        XmlObject newDatalabelsRange = XmlObject.Factory.parse(newDatalabelsRangeXml);
        XmlObject xmlFragment = XmlObject.Factory.parse(ctExtensionStr);
        XmlObject[] oldDlblRangeCache = xmlFragment.selectPath("declare namespace c15='http://schemas.microsoft.com/office/drawing/2012/chart'" + ".//c15:datalabelsRange");
        oldDlblRangeCache[0].set(newDatalabelsRange);
        extensions.remove(0);
        CTExtension ctExtension = ctExtensionList.insertNewExt(0);
        ctExtension.set(xmlFragment);
    }

    // 设置辅助列范围
    private static String getCtxRangeFormula(int colIndex) {
        String colLetter = getExcelColumnLetter(colIndex);
        return "<c:f>Sheet1!$" + colLetter + "$1</c:f>";
    }

    /**
     * 获取辅助列范围
     */
    private static String getDatalabelsRangeFormula(int colIndex, int range) {
        String colLetter = getExcelColumnLetter(colIndex);
        return "<c15:f>Sheet1!$" + colLetter + "$2:$" + colLetter + "$" + range + "</c15:f>";
    }

    /**
     * 列号转 Excel列字母方法
     */
    private static String getExcelColumnLetter(int colIndex) {
        StringBuilder sb = new StringBuilder();
        while (colIndex > 0) {
            int rem = (colIndex - 1) % 26;
            sb.insert(0, (char) (rem + 'A'));
            colIndex = (colIndex - 1) / 26;
        }
        return sb.toString();
    }


    /**
     * 判断是否是小数
     */
    public static boolean isDecimal(String valColText) {
        if (valColText == null || valColText.trim().isEmpty()) {
            return false;
        }
        try {
            BigDecimal bd = new BigDecimal(valColText);
            return bd.scale() > 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private static List<Integer> getCategoryAndValueIndexList(JSONArray templateConfigItem) {
        List<Integer> indexList = new ArrayList<>();
        for (int j = 0; j < templateConfigItem.size(); j++) {
            JSONArray innerInnerArr = templateConfigItem.getJSONArray(j);
            for (int k = 0; k < innerInnerArr.size(); k++) {
                JSONObject obj = innerInnerArr.getJSONObject(k);
                if (obj.containsKey("categoryIndex")) {
                    Integer categoryIndex = obj.getIntValue("categoryIndex");
                    if (!indexList.contains(categoryIndex)) {
                        indexList.add(categoryIndex);
                    }
                }
                if (obj.containsKey("valueIndex")) {
                    Integer valueIndex = obj.getIntValue("valueIndex");
                    if (!indexList.contains(valueIndex)) {
                        indexList.add(valueIndex);
                    }
                }
            }
        }
        Collections.sort(indexList);
        return indexList;
    }
}
