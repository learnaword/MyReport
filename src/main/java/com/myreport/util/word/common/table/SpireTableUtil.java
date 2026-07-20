package com.myreport.util.word.common.table;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.spire.doc.*;
import com.spire.doc.documents.*;
import com.spire.doc.fields.TextRange;
import org.apache.commons.lang.StringUtils;

import java.awt.*;
import java.math.BigDecimal;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SpireTableUtil {

    /**
     * 创建表带样式
     *
     * @param section
     * @param jsonData 数据
     */
    public static void createTable(Section section, JSONObject jsonData) {
        createTable(section, new JSONObject(), jsonData, new JSONObject());
    }

    /**
     * 创建表带样式
     *
     * @param section
     * @param jsonData 数据
     */
    public static void createTable(Section section, JSONObject showItem, JSONObject jsonData, JSONObject overallSetting) {
        //表数据
        JSONArray tableList = jsonData.getJSONArray("tableList");
        JSONArray headList = jsonData.getJSONArray("headList");
        JSONArray cellHeadList = jsonData.getJSONArray("cellHeadList");
        JSONArray cellTableList = jsonData.getJSONArray("cellTableList");
        //设置标题
        setTitle(section, jsonData, overallSetting);
        //创建按表格
        Table table = section.addTable(true);
        table.resetCells(headList.size() + tableList.size(), headList.getJSONArray(0).size());
        //处理表头
        setTableHead(table, headList, cellHeadList, overallSetting);
        //设置表体
        setTableData(table, headList, tableList, cellTableList, showItem, overallSetting);
        //设置表格宽度
        setTableWidth(table, headList);
    }

    /**
     * 设置表格宽度
     */
    private static void setTableWidth(Table table, JSONArray headList) {
        int colSize = headList.getJSONArray(0).size();
        if (colSize == 2 || colSize == 3) {
            table.autoFit(AutoFitBehaviorType.Auto_Fit_To_Window);
            PreferredWidth percentageWidth = new PreferredWidth(WidthType.Percentage, (short) 100);
            table.setPreferredWidth(percentageWidth);
        } else {
            table.setPreferredWidth(PreferredWidth.getAuto());
            table.autoFit(AutoFitBehaviorType.Auto_Fit_To_Window);
        }
    }

    /**
     * 设置表头
     *
     * @param table        表
     * @param headList     表头数据
     * @param cellHeadList 合并表头数据
     */
    private static void setTableHead(Table table, JSONArray headList, JSONArray cellHeadList, JSONObject overallSetting) {
        //处理表头数据
        doTableHeadData(table, headList, overallSetting);
        //合并header
        megerTableCellHead(table, cellHeadList);
    }

    /**
     * 设置表体
     *
     * @param table         表
     * @param headList      表头数据
     * @param tableList     表体数据
     * @param cellTableList 合并表体数据
     */
    private static void setTableData(Table table, JSONArray headList, JSONArray tableList, JSONArray cellTableList, JSONObject showItem, JSONObject overallSetting) {
        //处理表格数据
        doTableData(table, headList, tableList, showItem, overallSetting);
        //合并表体
        megerTableCellData(table, cellTableList, headList);
    }

    /**
     * 合并表体
     */
    private static void megerTableCellData(Table table, JSONArray cellTableList, JSONArray headList) {
        // 根据 cellHeadList 进行合并
        for (int i = 0; i < cellTableList.size(); i++) {
            JSONObject cellHead = cellTableList.getJSONObject(i);
            int row = cellHead.getIntValue("row") + headList.size();
            int col = cellHead.getIntValue("col");
            int colspan = cellHead.getIntValue("colspan");
            int rowspan = cellHead.getIntValue("rowspan");

            if (colspan > 0 && rowspan > 0) {
                if (rowspan > 0 && colspan > 0) {
                    mergeCellsCrossMerge(table, row, col, row + rowspan, col + colspan);
                }
            } else {
                if (colspan > 0) {
                    mergeCellsHorizontal(table, row, col, col + colspan);
                }
                if (rowspan > 0) {
                    mergeCellsVertically(table, col, row, row + rowspan);
                }
            }
        }
    }

    /**
     * 设置表体数据
     *
     * @param table     表
     * @param headList  表头数据
     * @param tableList 表体数据
     */
    private static void doTableData(Table table, JSONArray headList, JSONArray tableList, JSONObject showItem, JSONObject overallSetting) {
        Boolean isLastBold = showItem.containsKey("isLastBold") ? showItem.getBooleanValue("isLastBold") : false;
        Boolean isBold = false;
        // 处理表格数据
        for (int i = 0; i < tableList.size(); i++) {
            JSONArray rowArray = tableList.getJSONArray(i);
            TableRow row = table.getRows().get(i + headList.size());
            setTableDataRowStyle(row);
            if (i == tableList.size() - 1) {
                isBold = isLastBold;
            }
            for (int j = 0; j < rowArray.size(); j++) {
                TableCell cell = row.getCells().get(j);
                String cellValue = rowArray.getString(j);
                setTableDataCellStyle(cell, cellValue, isBold, overallSetting);
            }
        }
    }

    /**
     * 设置表格数据行样式（行高）
     */
    private static void setTableDataRowStyle(TableRow row) {
        row.setHeight(17.3f);
        row.setHeightType(TableRowHeightType.At_Least);
        row.getRowFormat().getBorders().setBorderType(BorderStyle.Single);
        row.getRowFormat().getBorders().setLineWidth(3.0f);
        row.getRowFormat().getBorders().setColor(Color.white);
    }

    /**
     * @param cell      单元格对象
     * @param cellValue 单元格内容
     */
    private static void setTableDataCellStyle(TableCell cell, String cellValue, Boolean isBold, JSONObject overallSetting) {
        // 设置单元格背景色（淡蓝色）
        String strTableDataBackColor = overallSetting.containsKey("strTableDataBackColor") ? overallSetting.getString("strTableDataBackColor") : "C0D7F1";
        cell.getCellFormat().setBackColor(Color.decode("#" + strTableDataBackColor));
        cell.getCellFormat().getBorders().setBorderType(BorderStyle.Single);
        cell.getCellFormat().getBorders().setLineWidth(3.0f);
        cell.getCellFormat().getBorders().setColor(Color.white);
        Paragraph para = cell.addParagraph();
        // 字体颜色
        String strTableDataFontColor = overallSetting.containsKey("strTableDataFontColor") ? overallSetting.getString("strTableDataFontColor") : "000000";
        appendWithNumberStyle(para, cellValue, "宋体", 10.5f, Color.decode("#" + strTableDataFontColor), isBold);
        para.applyStyle("报告表体正文");
        cell.getParagraphs().get(0).getFormat().setWordWrap(false);
        cell.getCellFormat().setVerticalAlignment(VerticalAlignment.Middle);
        //数字列不换行
        if (isDoubleValue(cellValue)) {
            cell.getCellFormat().setTextWrap(false);
        } else {
            cell.getCellFormat().setTextWrap(true);
        }
    }

    /**
     * 处理表头数据
     *
     * @param table    Spire 表格对象
     * @param headList 表头数据（JSONArray）
     */
    private static void doTableHeadData(Table table, JSONArray headList, JSONObject overallSetting) {
        for (int i = 0; i < headList.size(); i++) {
            JSONArray rowArray = headList.getJSONArray(i);
            TableRow row = table.getRows().get(i);
            setTableHeadRowStyle(row);
            for (int j = 0; j < rowArray.size(); j++) {
                TableCell cell = row.getCells().get(j);
                String cellValue = rowArray.getString(j);
                setTableHeadCellStyle(cell, cellValue, overallSetting);
            }
        }
    }

    /**
     * 设置表头行样式
     *
     * @param row Spire 表格的 TableRow
     */
    private static void setTableHeadRowStyle(TableRow row) {
        row.isHeader(true);
        row.setHeight(17.3f);
        row.getRowFormat().getBorders().setBorderType(BorderStyle.Single);
        row.getRowFormat().getBorders().setLineWidth(3.0f);
        row.getRowFormat().getBorders().setColor(Color.white);
    }

    /**
     * 设置表头单元格样式
     *
     * @param cell      表格单元格
     * @param cellValue 单元格文本
     */
    private static void setTableHeadCellStyle(TableCell cell, String cellValue, JSONObject overallSetting) {
        // 设置单元格背景色（十六进制颜色码）
        String strTableHeadBackColor = overallSetting.containsKey("strTableHeadBackColor") ? overallSetting.getString("strTableHeadBackColor") : "0F6FC6";
        cell.getCellFormat().setBackColor(Color.decode("#" + strTableHeadBackColor));
        cell.getCellFormat().setVerticalAlignment(VerticalAlignment.Middle);
        cell.getCellFormat().getBorders().setBorderType(BorderStyle.Single);
        cell.getCellFormat().getBorders().setLineWidth(3.0f);
        cell.getCellFormat().getBorders().setColor(Color.white);
        Paragraph paragraph = cell.addParagraph();
        String strTableHeadFontColor = overallSetting.containsKey("strTableHeadFontColor") ? overallSetting.getString("strTableHeadFontColor") : "FFFFFF";
        appendWithNumberStyle(paragraph, cellValue, "宋体", 10.5f, Color.decode("#" + strTableHeadFontColor), true);
        paragraph.getFormat().setHorizontalAlignment(HorizontalAlignment.Center);
        paragraph.applyStyle("报告表头正文");
    }

    /**
     * 合并表头
     */
    private static void megerTableCellHead(Table table, JSONArray cellHeadList) {
        // 根据 cellHeadList 进行合并
        for (int i = 0; i < cellHeadList.size(); i++) {
            JSONObject cellHead = cellHeadList.getJSONObject(i);
            int row = cellHead.getIntValue("row");
            int col = cellHead.getIntValue("col");
            int colspan = cellHead.getIntValue("colspan");
            int rowspan = cellHead.getIntValue("rowspan");
            if (colspan > 0 && rowspan > 0) {
                mergeCellsCrossMerge(table, row, col, row + rowspan, col + colspan);
            } else {
                if (colspan > 0) {
                    mergeCellsHorizontal(table, row, col, col + colspan);
                }

                if (rowspan > 0) {
                    mergeCellsVertically(table, col, row, row + rowspan);
                }
            }
        }
    }

    // 完美的四合一合并方法
    private static void mergeCellsCrossMerge(Table table, int startRow, int startCol, int endRow, int endCol) {
        // 主单元格
        TableCell mainCell = table.getRows().get(startRow).getCells().get(startCol);
        mainCell.getCellFormat().setHorizontalMerge(CellMerge.Start);
        mainCell.getCellFormat().setVerticalMerge(CellMerge.Start);

        // 同一行的其他列（横向合并）
        for (int col = startCol + 1; col <= endCol; col++) {
            TableCell cell = table.getRows().get(startRow).getCells().get(col);
            cell.getCellFormat().setHorizontalMerge(CellMerge.Continue);
            cell.getCellFormat().setVerticalMerge(CellMerge.Start);
            cell.getParagraphs().clear();
        }

        // 同一列的其他行（纵向合并）
        for (int row = startRow + 1; row <= endRow; row++) {
            TableCell cell = table.getRows().get(row).getCells().get(startCol);
            cell.getCellFormat().setHorizontalMerge(CellMerge.Start);
            cell.getCellFormat().setVerticalMerge(CellMerge.Continue);
            cell.getParagraphs().clear();
        }

        // 右下角区域（同时横向 + 纵向合并）
        for (int row = startRow + 1; row <= endRow; row++) {
            for (int col = startCol + 1; col <= endCol; col++) {
                TableCell cell = table.getRows().get(row).getCells().get(col);
                cell.getCellFormat().setHorizontalMerge(CellMerge.Continue);
                cell.getCellFormat().setVerticalMerge(CellMerge.Continue);
                cell.getParagraphs().clear();
            }
        }
    }

    /**
     * 竖向合并
     *
     * @param table 表
     */
    private static void mergeCellsVertically(Table table, int colIndex, int fromRowIndex, int toRowIndex) {
        for (int i = fromRowIndex; i <= toRowIndex; i++) {
            TableCell cell = table.getRows().get(i).getCells().get(colIndex);
            if (i == fromRowIndex) {
                cell.getCellFormat().setVerticalMerge(CellMerge.Start);
            } else {
                cell.getCellFormat().setVerticalMerge(CellMerge.Continue);
                cell.getParagraphs().clear();
            }
        }
    }

    /**
     * 横向合并
     *
     * @param table 表
     */
    private static void mergeCellsHorizontal(Table table, int rowIndex, int fromCellIndex, int toCellIndex) {
        TableRow row = table.getRows().get(rowIndex);
        for (int i = fromCellIndex; i <= toCellIndex; i++) {
            TableCell cell = row.getCells().get(i);
            if (i == fromCellIndex) {
                cell.getCellFormat().setHorizontalMerge(CellMerge.Start);
            } else {
                cell.getCellFormat().setHorizontalMerge(CellMerge.Continue);
                cell.getParagraphs().clear();
            }
        }
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
     * 检查是否是数字
     *
     * @param valueItem
     */
    private static boolean isDoubleValue(Object valueItem) {
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
        return false;
    }


    /**
     * 按数字拆分文本并设置样式
     *
     * @param para 段落
     * @param text 文本
     */
    private static void appendWithNumberStyle(Paragraph para, String text, String fontFamily, Float fontSize, Color fontColor, Boolean isBold) {
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

}
