package com.myreport.util.word.common.table;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.apache.poi.xwpf.usermodel.*;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.*;

import java.math.BigInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PoiTableUtil {
    /**
     * 创建表带样式
     *
     * @param document
     * @param jsonData 数据
     */
    public static void createTable(XWPFDocument document, JSONObject jsonData) {
        //表数据
        JSONArray tableList = jsonData.getJSONArray("tableList");
        JSONArray headList = jsonData.getJSONArray("headList");
        JSONArray cellHeadList = jsonData.getJSONArray("cellHeadList");
        JSONArray cellTableList = jsonData.getJSONArray("cellTableList");
        //设置标题
        setTitle(document, jsonData);
        //创建按表格
        XWPFTable table = document.createTable(headList.size() + tableList.size(), headList.getJSONArray(0).size());
        //设置100%，表格会溢出
        table.setWidth("99%");
        setTableBorder(table);
        //处理表头
        setTableHead(document, table, headList, cellHeadList);
        //设置表体
        setTableData(document, table, headList, tableList, cellTableList);
    }

    /**
     * 设置表格标题
     *
     * @param document
     * @param chartJson 数据
     */
    public static void setTitle(XWPFDocument document, JSONObject chartJson) {
        String strTitle = chartJson.containsKey("title") ? chartJson.getString("title") : "";
        setTitle(document, strTitle);
    }

    /**
     * 设置标题
     *
     * @param document
     * @param strTitle 数据
     */
    public static void setTitle(XWPFDocument document, String strTitle) {
        XWPFParagraph titleParagraph = document.createParagraph();
        // 设置标题水平居中
        XWPFStyle titleStyle = document.getStyles().getStyleWithName("报告图表标题");
        titleParagraph.setStyle(titleStyle.getStyleId());

        titleParagraph.setAlignment(ParagraphAlignment.CENTER);
        Pattern pattern = Pattern.compile("(\\d+)");
        Matcher matcher = pattern.matcher(strTitle);

        int lastEnd = 0;
        XWPFRun run = titleParagraph.createRun();
        run.setBold(true);
        CTRPr ctrPr = run.getCTR().addNewRPr();
        ctrPr.addNewSzCs().setVal(new BigInteger("21"));
        ctrPr.addNewSz().setVal(new BigInteger("21"));
        run.setFontFamily("黑体");
        String strColor = "0070C0";
        run.setColor(strColor);
        while (matcher.find()) {
            // 添加非数字文本
            if (matcher.start() > lastEnd) {
                run.setText(strTitle.substring(lastEnd, matcher.start()));
                run = titleParagraph.createRun();
                run.setFontFamily("黑体");
                run.setColor(strColor);
                ctrPr = run.getCTR().addNewRPr();
                ctrPr.addNewSzCs().setVal(new BigInteger("21"));
                ctrPr.addNewSz().setVal(new BigInteger("21"));
                run.setBold(true);
            }

            // 添加数字文本并应用特殊样式
            run.setText(matcher.group());
            run.setFontFamily("Times New Roma");
            run.setColor(strColor);
            ctrPr = run.getCTR().addNewRPr();
            ctrPr.addNewSzCs().setVal(new BigInteger("21"));
            ctrPr.addNewSz().setVal(new BigInteger("21"));
            run.setBold(true);

            lastEnd = matcher.end();
            if (lastEnd < strTitle.length()) {
                run = titleParagraph.createRun();
                run.setFontFamily("黑体");
                run.setColor(strColor);
                ctrPr = run.getCTR().addNewRPr();
                ctrPr.addNewSzCs().setVal(new BigInteger("21"));
                ctrPr.addNewSz().setVal(new BigInteger("21"));
                run.setBold(true);
            }

        }
        // 添加剩余的非数字文本
        if (lastEnd < strTitle.length()) {
            run.setText(strTitle.substring(lastEnd));
        }
    }

    /**
     * 设置表样式
     *
     * @param table 表
     */
    private static void setTableBorder(XWPFTable table) {
        table.setTopBorder(XWPFTable.XWPFBorderType.SINGLE, 20, 0, "FFFFFF");
        table.setBottomBorder(XWPFTable.XWPFBorderType.SINGLE, 20, 0, "FFFFFF");
        table.setLeftBorder(XWPFTable.XWPFBorderType.SINGLE, 20, 0, "FFFFFF");
        table.setRightBorder(XWPFTable.XWPFBorderType.SINGLE, 20, 0, "FFFFFF");
        table.setInsideHBorder(XWPFTable.XWPFBorderType.SINGLE, 20, 0, "FFFFFF");
        table.setInsideVBorder(XWPFTable.XWPFBorderType.SINGLE, 20, 0, "FFFFFF");
    }

    /**
     * 设置表头
     *
     * @param table        表
     * @param headList     表头数据
     * @param cellHeadList 合并表头数据
     */
    private static void setTableHead(XWPFDocument document, XWPFTable table, JSONArray headList, JSONArray
            cellHeadList) {
        //处理表头数据
        doTableHeadData(document, table, headList);
        //合并header
        megerTableCellHead(table, cellHeadList);
    }

    /**
     * 处理表头数据
     *
     * @param table    表
     * @param headList 表头数据
     */
    private static void doTableHeadData(XWPFDocument document, XWPFTable table, JSONArray headList) {
        for (int i = 0; i < headList.size(); i++) {
            JSONArray rowArray = headList.getJSONArray(i);
            XWPFTableRow row = table.getRow(i);
            setTableHeadRowStyle(row);
            for (int j = 0; j < rowArray.size(); j++) {
                XWPFTableCell cell = row.getCell(j);
                String cellValue = rowArray.getString(j);
                setTableHeadCellStyle(document, cell, cellValue);
            }
        }
    }

    /**
     * 设置表头行样式
     *
     * @param row 表格
     */
    private static void setTableHeadRowStyle(XWPFTableRow row) {
        CTTrPr trPr = row.getCtRow().isSetTrPr() ? row.getCtRow().getTrPr() : row.getCtRow().addNewTrPr();
        CTHeight height = trPr.addNewTrHeight();
        height.setVal(new BigInteger("340"));
        height.setHRule(STHeightRule.Enum.forString("atLeast"));
        //换页的时候，重新加载表头
        row.setRepeatHeader(true);
    }

    /**
     * 设置表头列样式
     *
     * @param cell      表格
     * @param cellValue 表格的值
     */
    private static void setTableHeadCellStyle(XWPFDocument document, XWPFTableCell cell, String cellValue) {
        // 设置表头背景色
        cell.setColor("0F6FC6");
        cell.setVerticalAlignment(XWPFTableCell.XWPFVertAlign.CENTER);

        //设置文本样式
        XWPFParagraph paragraph = cell.getParagraphs().get(0);
        XWPFStyle style = document.getStyles().getStyleWithName("报告表头正文");
        paragraph.setStyle(style.getStyleId());

        //设置内容
        XWPFRun run = paragraph.createRun();
        run.setText(cellValue);
    }

    /**
     * 合并表头
     */
    private static void megerTableCellHead(XWPFTable table, JSONArray cellHeadList) {
        // 根据 cellHeadList 进行合并
        for (int i = 0; i < cellHeadList.size(); i++) {
            JSONObject cellHead = cellHeadList.getJSONObject(i);
            int row = cellHead.getIntValue("row");
            int col = cellHead.getIntValue("col");
            int colspan = cellHead.getIntValue("colspan");
            int rowspan = cellHead.getIntValue("rowspan");

            if (colspan > 0 && rowspan > 0) {
                mergeCellsCrossMerge(table, row, col, rowspan, colspan);
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
     * 横向合并
     *
     * @param table    表
     * @param row      行号
     * @param fromCell 起始列
     * @param toCell   终止列
     */
    private static void mergeCellsHorizontal(XWPFTable table, int row, int fromCell, int toCell) {
        for (int cellIndex = fromCell; cellIndex <= toCell; cellIndex++) {
            XWPFTableCell cell = table.getRow(row).getCell(cellIndex);
            if (cellIndex == fromCell) {
                cell.getCTTc().addNewTcPr().addNewHMerge().setVal(STMerge.RESTART);
            } else {
                cell.getCTTc().addNewTcPr().addNewHMerge().setVal(STMerge.CONTINUE);
                cell.getCTTc().getPArray(0).removeR(0);
            }
        }
    }

    /**
     * 竖向合并
     *
     * @param table   表
     * @param col     列号
     * @param fromRow 起始行
     * @param toRow   终止行
     */
    private static void mergeCellsVertically(XWPFTable table, int col, int fromRow, int toRow) {
        for (int rowIndex = fromRow; rowIndex <= toRow; rowIndex++) {
            XWPFTableCell cell = table.getRow(rowIndex).getCell(col);
            if (rowIndex == fromRow) {
                cell.getCTTc().addNewTcPr().addNewVMerge().setVal(STMerge.RESTART);
            } else {
                cell.getCTTc().addNewTcPr().addNewVMerge().setVal(STMerge.CONTINUE);
                cell.getCTTc().getPArray(0).removeR(0);
            }
        }
    }

    // 完美的四合一合并方法
    private static void mergeCellsCrossMerge(XWPFTable table, int startRow, int startCol, int endRow, int endCol) {
        // 第一步：处理主单元格（合并起始点）
        XWPFTableCell mainCell = table.getRow(startRow).getCell(startCol);
        mainCell.getCTTc().addNewTcPr().addNewHMerge().setVal(STMerge.RESTART);
        mainCell.getCTTc().getTcPr().addNewVMerge().setVal(STMerge.RESTART);

        // 第二步：处理同一行的其他列（横向合并）
        for (int col = startCol + 1; col <= endCol; col++) {
            XWPFTableCell cell = table.getRow(startRow).getCell(col);
            cell.getCTTc().addNewTcPr().addNewHMerge().setVal(STMerge.CONTINUE);
            cell.getCTTc().getTcPr().addNewVMerge().setVal(STMerge.RESTART);
            cell.getCTTc().getPArray(0).removeR(0);
        }

        // 第三步：处理同一列的其他行（纵向合并）
        for (int row = startRow + 1; row <= endRow; row++) {
            XWPFTableCell cell = table.getRow(row).getCell(startCol);
            cell.getCTTc().addNewTcPr().addNewHMerge().setVal(STMerge.RESTART);
            cell.getCTTc().getTcPr().addNewVMerge().setVal(STMerge.CONTINUE);
            cell.getCTTc().getPArray(0).removeR(0);
        }

        // 第四步：处理右下角区域（同时横向和纵向合并）
        for (int row = startRow + 1; row <= endRow; row++) {
            for (int col = startCol + 1; col <= endCol; col++) {
                XWPFTableCell cell = table.getRow(row).getCell(col);
                cell.getCTTc().addNewTcPr().addNewHMerge().setVal(STMerge.CONTINUE);
                cell.getCTTc().getTcPr().addNewVMerge().setVal(STMerge.CONTINUE);
                cell.getCTTc().getPArray(0).removeR(0);
            }
        }
    }

    /**
     * 设置表体
     *
     * @param table         表
     * @param headList      表头数据
     * @param tableList     表体数据
     * @param cellTableList 合并表体数据
     */
    private static void setTableData(XWPFDocument document, XWPFTable table, JSONArray headList, JSONArray
            tableList, JSONArray cellTableList) {
        //处理表格数据
        doTableData(document, table, headList, tableList, cellTableList);
        //合并表体
        megerTableCellData(document, table, cellTableList, headList);
    }

    /**
     * 设置表体数据
     *
     * @param table         表
     * @param headList      表头数据
     * @param tableList     表体数据
     * @param cellTableList 合并表体数据
     */
    private static void doTableData(XWPFDocument document, XWPFTable table, JSONArray headList, JSONArray
            tableList, JSONArray cellTableList) {
        // 处理表格数据
        for (int i = 0; i < tableList.size(); i++) {
            JSONArray rowArray = tableList.getJSONArray(i);
            XWPFTableRow row = table.getRow(i + headList.size());
            setTableDataRowStyle(row);
            for (int j = 0; j < rowArray.size(); j++) {
                XWPFTableCell cell = row.getCell(j);
                String cellValue = rowArray.getString(j);
                setTableDataCellStyle(document, cell, cellValue);
            }
            XWPFTableCell headerCell = table.getRow(i).getCell(0);
            CTTcPr headerTcPr = headerCell.getCTTc().addNewTcPr();
            headerTcPr.addNewNoWrap().setVal(STOnOff.Enum.forInt(1));
        }
    }

    /**
     * 设置表头行样式
     *
     * @param row 表格
     */
    private static void setTableDataRowStyle(XWPFTableRow row) {
        CTTrPr trPr = row.getCtRow().isSetTrPr() ? row.getCtRow().getTrPr() : row.getCtRow().addNewTrPr();
        CTHeight height = trPr.addNewTrHeight();
        height.setVal(new BigInteger("340"));
        height.setHRule(STHeightRule.Enum.forString("atLeast"));
    }

    /**
     * 设置表头行样式
     *
     * @param cell      表格
     * @param cellValue 表格数据
     */
    private static void setTableDataCellStyle(XWPFDocument document, XWPFTableCell cell, String cellValue) {
        // 设置表格背景色
        cell.setColor("C0D7F1");
        // 设置样式
        cell.setVerticalAlignment(XWPFTableCell.XWPFVertAlign.CENTER);
        XWPFParagraph paragraph = cell.getParagraphs().get(0);
        XWPFStyle style = document.getStyles().getStyleWithName("报告表体正文");
        paragraph.setStyle(style.getStyleId());

        //设置内容
        XWPFRun run = paragraph.createRun();
        run.setText(cellValue);
        // 检查 cellValue 是否包含数字
        if (cellValue != null && cellValue.matches(".*\\d+.*")) {
            run.setFontFamily("Times New Roman");
        }
    }


    /**
     * 合并表体
     */
    private static void megerTableCellData(XWPFDocument document, XWPFTable table, JSONArray
            cellTableList, JSONArray headList) {
        // 根据 cellHeadList 进行合并
        for (int i = 0; i < cellTableList.size(); i++) {
            JSONObject cellHead = cellTableList.getJSONObject(i);
            int row = cellHead.getIntValue("row") + headList.size();
            int col = cellHead.getIntValue("col");
            int colspan = cellHead.getIntValue("colspan");
            int rowspan = cellHead.getIntValue("rowspan");

            if (colspan > 0 && rowspan > 0) {
                mergeCellsCrossMerge(table, row, col, rowspan, colspan);
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

}
