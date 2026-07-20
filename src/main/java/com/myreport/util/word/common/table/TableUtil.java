package com.myreport.util.word.common.table;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import java.util.*;

public class TableUtil {

    /**
     * 合并重复的表头
     * */
    public static JSONArray mergeRepeatHeader(JSONArray headRows) {
        JSONArray result = new JSONArray();
        for (int i = 0; i < headRows.size(); i++) {
            JSONArray row = headRows.getJSONArray(i);
            JSONArray distinct = distinctRow(row);
            if (result.isEmpty()) {
                result.add(row);
                continue;
            }
            if (isSupplementRow(distinct)) {
                // 补充行：拼到上一行
                JSONArray last = result.getJSONArray(result.size() - 1);
                JSONArray merged = mergeRowByColumn(last, row);
                result.set(result.size() - 1, merged);
            } else {
                // 结构行：新起一行
                result.add(row);
            }
        }
        return result;
    }

    // 执行合并操作
    public static void mergeColumn(JSONObject jsonObject) {
        JSONArray mergedHead = mergeCells(jsonObject.getJSONArray("headList"));
        JSONArray mergeTable = mergeTable(jsonObject.getJSONArray("tableList"));
        if (mergeTable.size() > 0) {
            jsonObject.put("cellTableList", mergeTable.getJSONArray(0));
        } else {
            jsonObject.put("cellTableList", new JSONArray());
        }
        jsonObject.put("cellHeadList", mergedHead);
    }

    /**
     * 新加判断前三列合并计算  第一列合并范围内 第二列相邻相同值合并 第三列在第二列合并范围行相临相同值进行合并
     */
    public static JSONArray mergeTable(JSONArray table) {
        List<JSONObject> result = new ArrayList<>();
        int rows = table.size();
        if (rows == 0) return new JSONArray();
        Integer tableItemSize = table.getJSONArray(0).size();
        boolean[] rowProcessed = new boolean[rows];
        // 1. 处理第一列合并（连续相同值纵向合并）
        for (int i = 0; i < rows; i++) {
            if (rowProcessed[i]) continue;
            String firstColVal = table.getJSONArray(i).getString(0);
            int firstColRowSpan = 1;
            // 查找第一列相同的连续行
            int j = i + 1;
            while (j < rows && table.getJSONArray(j).getString(0).equals(firstColVal)) {
                firstColRowSpan++;
                j++;
            }
            if (firstColRowSpan > 1) {
                // 合并第一列
                JSONObject firstColMerge = new JSONObject();
                firstColMerge.put("row", i);
                firstColMerge.put("col", 0);
                firstColMerge.put("rowspan", firstColRowSpan - 1);
                firstColMerge.put("colspan", 0);
                result.add(firstColMerge);
                // 2. 在第一列合并的范围内处理第二列合并
                int startRow = i;
                int endRow = i + firstColRowSpan;
                if (tableItemSize > 1) {
                    // 处理第二列合并，并获取第二列合并的范围
                    List<MergeRange> secondColMergeRanges = processColumnMerge(result, table, 1, startRow, endRow, firstColVal);
                    // 3. 在第二列合并的范围内处理第三列合并
                    for (MergeRange range : secondColMergeRanges) {
                        if (range.rowSpan > 1 && tableItemSize > 2) { // 只有在第二列合并的范围内才处理第三列合并
                            processColumnMergeInRange(result, table, 2, range.startRow, range.endRow);
                        }
                    }
                    // 标记这些行已处理
                    for (int r = i; r < endRow; r++) {
                        rowProcessed[r] = true;
                    }
                }
            }
        }

        // 4. 处理单行横向合并（第一列和第二列相同的情况）
        for (int i = 0; i < rows; i++) {
            if (rowProcessed[i]) continue;
            JSONArray row = table.getJSONArray(i);
            if (row.size() >= 2 && row.getString(0).equals(row.getString(1))) {
                JSONObject horizontalMerge = new JSONObject();
                horizontalMerge.put("row", i);
                horizontalMerge.put("col", 0);
                horizontalMerge.put("rowspan", 0);
                horizontalMerge.put("colspan", 1);
                result.add(horizontalMerge);
                rowProcessed[i] = true;
            }
        }
        return new JSONArray(Collections.singletonList(result));
    }

    public static void addMergeObject(JSONArray result, int row, int col, int rowspanVal, int colspanVal) {
        JSONObject obj = new JSONObject();
        obj.put("row", row);
        obj.put("col", col);
        obj.put("rowspan", rowspanVal);
        obj.put("colspan", colspanVal);
        result.add(obj);
    }

    /**
     * 处理指定列的合并，并返回合并的范围信息
     *
     * @param result      合并结果列表
     * @param table       表格数据
     * @param colIndex    列索引
     * @param startRow    起始行
     * @param endRow      结束行（不包含）
     * @param firstColVal 第一列的值（用于判断矩形合并）
     * @return 合并的范围列表
     */
    private static List<MergeRange> processColumnMerge(List<JSONObject> result, JSONArray table, int colIndex, int startRow, int endRow, String firstColVal) {
        List<MergeRange> mergeRanges = new ArrayList<>();
        int currentRow = startRow;
        while (currentRow < endRow) {
            String currentVal = table.getJSONArray(currentRow).getString(colIndex);
            int rowSpan = 1;
            // 查找连续相同值的行
            int nextRow = currentRow + 1;
            while (nextRow < endRow &&
                    table.getJSONArray(nextRow).getString(colIndex).equals(currentVal)) {
                rowSpan++;
                nextRow++;
            }
            // 如果有多行相同值，进行合并
            if (rowSpan > 1) {
                // 检查是否需要矩形合并（第二列与第一列值相同）
                boolean isRectMerge = (colIndex == 1 && currentVal.equals(firstColVal));
                JSONObject merge = new JSONObject();
                merge.put("row", currentRow);
                merge.put("col", colIndex);
                merge.put("rowspan", rowSpan - 1);
                merge.put("colspan", isRectMerge ? 0 : 0);
                // 如果是矩形合并，需要特殊标记
                if (isRectMerge) {
                    merge.put("rectMerge", true);
                }
                result.add(merge);
                // 记录合并范围
                mergeRanges.add(new MergeRange(currentRow, currentRow + rowSpan, rowSpan));
            } else {
                // 即使没有合并，也记录单行范围
                mergeRanges.add(new MergeRange(currentRow, currentRow + 1, 1));
            }
            currentRow += rowSpan;
        }
        return mergeRanges;
    }

    /**
     * 在指定范围内处理列的合并
     *
     * @param result   合并结果列表
     * @param table    表格数据
     * @param colIndex 列索引
     * @param startRow 起始行
     * @param endRow   结束行（不包含）
     */
    private static void processColumnMergeInRange(List<JSONObject> result, JSONArray table, int colIndex, int startRow, int endRow) {
        int currentRow = startRow;
        while (currentRow < endRow) {
            String currentVal = table.getJSONArray(currentRow).getString(colIndex);
            int rowSpan = 1;
            // 查找连续相同值的行
            int nextRow = currentRow + 1;
            while (nextRow < endRow &&
                    table.getJSONArray(nextRow).getString(colIndex).equals(currentVal)) {
                rowSpan++;
                nextRow++;
            }
            // 如果有多行相同值，进行合并
            if (rowSpan > 1) {
                JSONObject merge = new JSONObject();
                merge.put("row", currentRow);
                merge.put("col", colIndex);
                merge.put("rowspan", rowSpan - 1);
                merge.put("colspan", 0);
                result.add(merge);
            }
            currentRow += rowSpan;
        }
    }

    /**
     * 合并范围类
     */
    static class MergeRange {
        int startRow;
        int endRow;
        int rowSpan;

        public MergeRange(int startRow, int endRow, int rowSpan) {
            this.startRow = startRow;
            this.endRow = endRow;
            this.rowSpan = rowSpan;
        }
    }

    public static JSONArray mergeCells(JSONArray table) {
        JSONArray result = new JSONArray();
        int rowCount = table.size();
        if (rowCount == 0) return result;

        JSONArray row0 = (JSONArray) table.get(0);
        int colCount = row0.size();
        if (colCount == 0) return result;

        // 单行处理
        if (rowCount == 1) {
            int j = 0;
            while (j < colCount) {
                String current = row0.getString(j);
                int startCol = j;
                while (j < colCount && current.equals(row0.getString(j))) {
                    j++;
                }
                int spanLength = j - startCol;
                addSingleRowMerge(result, startCol, spanLength);
            }
            return result;
        }

        // 双行处理
        JSONArray row1 = (JSONArray) table.get(1);
        boolean[][] visited = new boolean[2][colCount];

        // 1. 先处理2x2合并
        for (int j = 0; j < colCount - 1; j++) {
            if (!visited[0][j] && !visited[0][j + 1] &&
                    !visited[1][j] && !visited[1][j + 1]) {
                String val = row0.getString(j);
                if (val.equals(row0.getString(j + 1)) &&
                        val.equals(row1.getString(j)) &&
                        val.equals(row1.getString(j + 1))) {
                    addMergeObject(result, 0, j, 1, 1);
                    visited[0][j] = visited[0][j + 1] = true;
                    visited[1][j] = visited[1][j + 1] = true;
                    j++; // 跳过下一列
                }
            }
        }

        // 2. 处理纵向合并（单列）
        for (int j = 0; j < colCount; j++) {
            if (!visited[0][j] && !visited[1][j] &&
                    row0.getString(j).equals(row1.getString(j))) {

                addMergeObject(result, 0, j, 1, 0);
                visited[0][j] = visited[1][j] = true;
            }
        }

        // 3. 处理第一行横向合并
        int j = 0;
        while (j < colCount) {
            if (visited[0][j]) {
                j++;
                continue;
            }

            String current = row0.getString(j);
            int startCol = j;
            int spanLength = 0;
            while (j < colCount && !visited[0][j] && current.equals(row0.getString(j))) {
                spanLength++;
                j++;
            }

            if (spanLength > 1) {
                addSingleRowMerge(result, startCol, spanLength);
                for (int k = startCol; k < startCol + spanLength; k++) {
                    visited[0][k] = true;
                }
            }
        }

        // 4. 处理第一行剩余未合并的单元格（纵向合并但值不同）
        for (j = 0; j < colCount; j++) {
            if (!visited[0][j]) {
                // 检查是否与下一行相同（可能被前面的纵向合并漏掉）
                if (!visited[1][j] && row0.getString(j).equals(row1.getString(j))) {
                    addMergeObject(result, 0, j, 1, 0);
                    visited[0][j] = visited[1][j] = true;
                }
            }
        }

        return result;
    }

    private static void addSingleRowMerge(JSONArray result, int startCol, int spanLength) {
        JSONObject obj = new JSONObject();
        obj.put("row", 0);
        obj.put("col", startCol);
        obj.put("rowspan", 0);
        obj.put("colspan", spanLength - 1);
        result.add(obj);
    }

    private static boolean isSupplementRow(JSONArray distinctRow) {
        return distinctRow.size() == 2;
    }

    private static JSONArray distinctRow(JSONArray row) {
        JSONArray result = new JSONArray();
        Set<Object> seen = new LinkedHashSet<>();
        for (Object o : row) {
            if (seen.add(o)) {
                result.add(o);
            }
        }
        return result;
    }


    private static JSONArray mergeRowByColumn(JSONArray baseRow, JSONArray supplementRow) {JSONArray result = new JSONArray();
        for (int i = 0; i < baseRow.size(); i++) {
            String base = String.valueOf(baseRow.get(i));
            String sup  = String.valueOf(supplementRow.get(i));
            // 完全相同的不再重复拼
            if (base.equals(sup)) {
                result.add(base);
            } else {
                result.add(base + sup);
            }
        }
        return result;
    }
}