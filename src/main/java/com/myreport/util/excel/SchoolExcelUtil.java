package com.myreport.util.excel;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 学校 Excel 解析（首行为表头，名称列：名称 / name）。
 */
public final class SchoolExcelUtil {

    private static final DataFormatter FORMATTER = new DataFormatter();

    private SchoolExcelUtil() {
    }

    /**
     * 解析学校名称列表；跳过空行与空名称。
     *
     * @param inputStream Excel 流
     * @param filename    原始文件名，用于区分 xls / xlsx
     */
    public static List<String> parseSchoolNames(InputStream inputStream, String filename) throws Exception {
        Workbook workbook = openWorkbook(inputStream, filename);
        try {
            Sheet sheet = workbook.getSheetAt(0);
            if (sheet == null) {
                throw new IllegalArgumentException("Excel 中没有工作表");
            }

            Row header = sheet.getRow(sheet.getFirstRowNum());
            if (header == null) {
                throw new IllegalArgumentException("Excel 表头为空");
            }

            int nameCol = findNameColumn(header);
            if (nameCol < 0) {
                throw new IllegalArgumentException("未找到名称列，请使用表头「名称」或「name」");
            }

            List<String> names = new ArrayList<String>();
            int firstDataRow = sheet.getFirstRowNum() + 1;
            int lastRow = sheet.getLastRowNum();
            for (int r = firstDataRow; r <= lastRow; r++) {
                Row row = sheet.getRow(r);
                if (row == null) {
                    continue;
                }
                String name = cellText(row.getCell(nameCol));
                if (name == null || name.isEmpty()) {
                    continue;
                }
                if (name.length() > 128) {
                    throw new IllegalArgumentException("第 " + (r + 1) + " 行学校名称超过 128 字符");
                }
                names.add(name);
            }
            return names;
        } finally {
            workbook.close();
        }
    }

    private static Workbook openWorkbook(InputStream inputStream, String filename) throws Exception {
        String lower = filename == null ? "" : filename.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".xls") && !lower.endsWith(".xlsx")) {
            return new HSSFWorkbook(inputStream);
        }
        return new XSSFWorkbook(inputStream);
    }

    private static int findNameColumn(Row header) {
        short last = header.getLastCellNum();
        for (int i = 0; i < last; i++) {
            String text = cellText(header.getCell(i));
            if (text == null) {
                continue;
            }
            String normalized = text.trim().toLowerCase(Locale.ROOT);
            if ("名称".equals(text.trim()) || "name".equals(normalized) || "学校名称".equals(text.trim())) {
                return i;
            }
        }
        return -1;
    }

    private static String cellText(Cell cell) {
        if (cell == null) {
            return null;
        }
        if (cell.getCellType() == CellType.BLANK) {
            return "";
        }
        String text = FORMATTER.formatCellValue(cell);
        return text == null ? null : text.trim();
    }
}
