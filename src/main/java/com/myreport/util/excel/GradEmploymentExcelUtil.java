package com.myreport.util.excel;

import com.myreport.entity.GradEmploymentRecord;
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
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 毕业生就业去向 Excel 解析（对齐现行 56 列表头）。
 * <p>
 * 「单位名称」按出现次序映射三列；「学校名称」导入时解析为 school_id。
 */
public final class GradEmploymentExcelUtil {

    private static final DataFormatter FORMATTER = new DataFormatter();

    /** 唯一表头 -> 字段名（不含三次「单位名称」） */
    private static final Map<String, String> HEADER_TO_FIELD;

    static {
        Map<String, String> map = new HashMap<String, String>();
        map.put("学校ID", "schoolId");
        map.put("学校id", "schoolId");
        map.put("school_id", "schoolId");
        map.put("schoolId", "schoolId");
        map.put("学校名称", "importSchoolName");
        map.put("学校名", "importSchoolName");
        map.put("school_name", "importSchoolName");
        map.put("schoolName", "importSchoolName");
        map.put("学号", "studentNo");
        map.put("姓名", "studentName");
        map.put("毕业年份", "graduationYear");
        map.put("性别", "genderName");
        map.put("学历", "educationName");
        map.put("学院名称", "collegeName");
        map.put("专业名称", "majorName");
        map.put("政治面貌", "politicalStatusName");
        map.put("民族", "ethnicityName");
        map.put("困难生类别", "hardshipTypeName");
        map.put("生源省", "sourceProvince");
        map.put("生源市", "sourceCity");
        map.put("省内外生源", "sourceInOutProvince");
        map.put("三大地理区域生源", "sourceGeo3");
        map.put("四大经济区域生源", "sourceEcon4");
        map.put("七大地理区域生源", "sourceGeo7");
        map.put("八大经济区生源", "sourceEcon8");
        map.put("广东省生源区域", "sourceGdRegion");
        map.put("原始毕业去向", "destinationRaw");
        map.put("毕业去向类别", "destinationCategory");
        map.put("毕业去向大类", "destinationMajorCategory");
        map.put("就业流向分析群体", "flowAnalysisGroup");
        map.put("单位所在地", "employerLocation");
        map.put("就业省", "jobProvince");
        map.put("就业市", "jobCity");
        map.put("三大地理区域就业", "jobGeo3");
        map.put("四大经济区域就业", "jobEcon4");
        map.put("七大地理区域就业", "jobGeo7");
        map.put("东北地区就业", "jobNortheast");
        map.put("八大经济区就业", "jobEcon8");
        map.put("八大经济区就业2", "jobEcon82");
        map.put("就业城市类别", "jobCityTier");
        map.put("广东省就业区域", "jobGdRegion");
        map.put("省内外就业", "jobInOutProvince");
        map.put("学校属地市就业", "jobSchoolCity");
        map.put("粤港澳大湾区就业", "jobGba");
        map.put("西部地区就业", "jobWest");
        map.put("一带一路地区就业", "jobBeltRoad");
        map.put("京津冀地区就业", "jobJjj");
        map.put("长江经济带就业", "jobYangtzeBelt");
        map.put("黄河流域就业", "jobYellowRiver");
        map.put("成渝经济圈就业", "jobChengyu");
        map.put("省内外就业-2", "jobInOutProvince2");
        map.put("省内生源就业交叉", "inProvinceSourceJobCross");
        map.put("单位性质", "employerNature");
        map.put("单位大类", "employerMajorType");
        map.put("单位所属行业", "employerIndustry");
        map.put("就业职业", "jobOccupation");
        map.put("留学国家/地区", "abroadCountryRegion");
        map.put("QS排名", "qsRank");
        map.put("US排名", "usRank");
        map.put("升学院校层次", "furtherStudyLevel");
        HEADER_TO_FIELD = Collections.unmodifiableMap(map);
    }

    private GradEmploymentExcelUtil() {
    }

    public static List<GradEmploymentRecord> parse(InputStream inputStream, String filename, String batchId)
            throws Exception {
        return parse(inputStream, filename, batchId, null);
    }

    /**
     * @param defaultSchoolId Excel 行未填学校ID/学校名称时使用的默认值（可为 null）
     */
    public static List<GradEmploymentRecord> parse(InputStream inputStream, String filename, String batchId,
                                                   Long defaultSchoolId) throws Exception {
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

            Map<Integer, String> colToField = buildColumnMapping(header);
            if (colToField.isEmpty()) {
                throw new IllegalArgumentException("未识别到有效表头列");
            }
            if (!colToField.containsValue("studentNo") || !colToField.containsValue("graduationYear")) {
                throw new IllegalArgumentException("表头须包含「学号」与「毕业年份」");
            }

            List<GradEmploymentRecord> list = new ArrayList<GradEmploymentRecord>();
            int firstDataRow = sheet.getFirstRowNum() + 1;
            int lastRow = sheet.getLastRowNum();
            for (int r = firstDataRow; r <= lastRow; r++) {
                Row row = sheet.getRow(r);
                if (row == null || isBlankRow(row, colToField)) {
                    continue;
                }
                GradEmploymentRecord record = new GradEmploymentRecord();
                record.setBatchId(batchId);
                for (Map.Entry<Integer, String> e : colToField.entrySet()) {
                    String text = cellText(row.getCell(e.getKey()));
                    applyField(record, e.getValue(), text, r + 1);
                }
                if (record.getSchoolId() == null
                        && (record.getImportSchoolName() == null || record.getImportSchoolName().isEmpty())
                        && defaultSchoolId != null) {
                    record.setSchoolId(defaultSchoolId);
                }
                if (isBlank(record.getStudentNo()) || record.getGraduationYear() == null) {
                    throw new IllegalArgumentException("第 " + (r + 1) + " 行学号或毕业年份为空");
                }
                list.add(record);
            }
            return list;
        } finally {
            workbook.close();
        }
    }

    private static Map<Integer, String> buildColumnMapping(Row header) {
        Map<Integer, String> colToField = new LinkedHashMap<Integer, String>();
        int unitNameCount = 0;
        short last = header.getLastCellNum();
        for (int i = 0; i < last; i++) {
            String text = cellText(header.getCell(i));
            if (isBlank(text)) {
                continue;
            }
            String headerName = text.trim();
            if ("单位名称".equals(headerName)) {
                unitNameCount++;
                if (unitNameCount == 1) {
                    colToField.put(i, "employerName");
                } else if (unitNameCount == 2) {
                    colToField.put(i, "unitNameAfterOccupation");
                } else if (unitNameCount == 3) {
                    colToField.put(i, "furtherStudySchoolName");
                }
                continue;
            }
            String field = HEADER_TO_FIELD.get(headerName);
            if (field != null) {
                colToField.put(i, field);
            }
        }
        return colToField;
    }

    private static void applyField(GradEmploymentRecord record, String field, String text, int excelRow) {
        if (isBlank(text)) {
            return;
        }
        if ("graduationYear".equals(field)) {
            record.setGraduationYear(parseYear(text, excelRow));
            return;
        }
        if ("schoolId".equals(field)) {
            record.setSchoolId(parseLongId(text, excelRow, "学校ID"));
            return;
        }
        if ("importSchoolName".equals(field)) {
            record.setImportSchoolName(text);
            return;
        }
        if ("studentNo".equals(field)) {
            record.setStudentNo(text);
            return;
        }
        if ("studentName".equals(field)) {
            record.setStudentName(text);
            return;
        }
        if ("genderName".equals(field)) {
            record.setGenderName(text);
            return;
        }
        if ("educationName".equals(field)) {
            record.setEducationName(text);
            return;
        }
        if ("collegeName".equals(field)) {
            record.setCollegeName(text);
            return;
        }
        if ("majorName".equals(field)) {
            record.setMajorName(text);
            return;
        }
        if ("politicalStatusName".equals(field)) {
            record.setPoliticalStatusName(text);
            return;
        }
        if ("ethnicityName".equals(field)) {
            record.setEthnicityName(text);
            return;
        }
        if ("hardshipTypeName".equals(field)) {
            record.setHardshipTypeName(text);
            return;
        }
        if ("sourceProvince".equals(field)) {
            record.setSourceProvince(text);
            return;
        }
        if ("sourceCity".equals(field)) {
            record.setSourceCity(text);
            return;
        }
        if ("sourceInOutProvince".equals(field)) {
            record.setSourceInOutProvince(text);
            return;
        }
        if ("sourceGeo3".equals(field)) {
            record.setSourceGeo3(text);
            return;
        }
        if ("sourceEcon4".equals(field)) {
            record.setSourceEcon4(text);
            return;
        }
        if ("sourceGeo7".equals(field)) {
            record.setSourceGeo7(text);
            return;
        }
        if ("sourceEcon8".equals(field)) {
            record.setSourceEcon8(text);
            return;
        }
        if ("sourceGdRegion".equals(field)) {
            record.setSourceGdRegion(text);
            return;
        }
        if ("destinationRaw".equals(field)) {
            record.setDestinationRaw(text);
            return;
        }
        if ("destinationCategory".equals(field)) {
            record.setDestinationCategory(text);
            return;
        }
        if ("destinationMajorCategory".equals(field)) {
            record.setDestinationMajorCategory(text);
            return;
        }
        if ("flowAnalysisGroup".equals(field)) {
            record.setFlowAnalysisGroup(text);
            return;
        }
        if ("employerName".equals(field)) {
            record.setEmployerName(text);
            return;
        }
        if ("employerLocation".equals(field)) {
            record.setEmployerLocation(text);
            return;
        }
        if ("jobProvince".equals(field)) {
            record.setJobProvince(text);
            return;
        }
        if ("jobCity".equals(field)) {
            record.setJobCity(text);
            return;
        }
        if ("jobGeo3".equals(field)) {
            record.setJobGeo3(text);
            return;
        }
        if ("jobEcon4".equals(field)) {
            record.setJobEcon4(text);
            return;
        }
        if ("jobGeo7".equals(field)) {
            record.setJobGeo7(text);
            return;
        }
        if ("jobNortheast".equals(field)) {
            record.setJobNortheast(text);
            return;
        }
        if ("jobEcon8".equals(field)) {
            record.setJobEcon8(text);
            return;
        }
        if ("jobEcon82".equals(field)) {
            record.setJobEcon82(text);
            return;
        }
        if ("jobCityTier".equals(field)) {
            record.setJobCityTier(text);
            return;
        }
        if ("jobGdRegion".equals(field)) {
            record.setJobGdRegion(text);
            return;
        }
        if ("jobInOutProvince".equals(field)) {
            record.setJobInOutProvince(text);
            return;
        }
        if ("jobSchoolCity".equals(field)) {
            record.setJobSchoolCity(text);
            return;
        }
        if ("jobGba".equals(field)) {
            record.setJobGba(text);
            return;
        }
        if ("jobWest".equals(field)) {
            record.setJobWest(text);
            return;
        }
        if ("jobBeltRoad".equals(field)) {
            record.setJobBeltRoad(text);
            return;
        }
        if ("jobJjj".equals(field)) {
            record.setJobJjj(text);
            return;
        }
        if ("jobYangtzeBelt".equals(field)) {
            record.setJobYangtzeBelt(text);
            return;
        }
        if ("jobYellowRiver".equals(field)) {
            record.setJobYellowRiver(text);
            return;
        }
        if ("jobChengyu".equals(field)) {
            record.setJobChengyu(text);
            return;
        }
        if ("jobInOutProvince2".equals(field)) {
            record.setJobInOutProvince2(text);
            return;
        }
        if ("inProvinceSourceJobCross".equals(field)) {
            record.setInProvinceSourceJobCross(text);
            return;
        }
        if ("employerNature".equals(field)) {
            record.setEmployerNature(text);
            return;
        }
        if ("employerMajorType".equals(field)) {
            record.setEmployerMajorType(text);
            return;
        }
        if ("employerIndustry".equals(field)) {
            record.setEmployerIndustry(text);
            return;
        }
        if ("jobOccupation".equals(field)) {
            record.setJobOccupation(text);
            return;
        }
        if ("unitNameAfterOccupation".equals(field)) {
            record.setUnitNameAfterOccupation(text);
            return;
        }
        if ("furtherStudySchoolName".equals(field)) {
            record.setFurtherStudySchoolName(text);
            return;
        }
        if ("abroadCountryRegion".equals(field)) {
            record.setAbroadCountryRegion(text);
            return;
        }
        if ("qsRank".equals(field)) {
            record.setQsRank(text);
            return;
        }
        if ("usRank".equals(field)) {
            record.setUsRank(text);
            return;
        }
        if ("furtherStudyLevel".equals(field)) {
            record.setFurtherStudyLevel(text);
        }
    }

    private static Integer parseYear(String text, int excelRow) {
        String digits = text.trim();
        if (digits.endsWith(".0")) {
            digits = digits.substring(0, digits.length() - 2);
        }
        try {
            return Integer.valueOf(digits);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("第 " + excelRow + " 行毕业年份无效：" + text);
        }
    }

    private static Long parseLongId(String text, int excelRow, String label) {
        String digits = text.trim();
        if (digits.endsWith(".0")) {
            digits = digits.substring(0, digits.length() - 2);
        }
        try {
            return Long.valueOf(digits);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("第 " + excelRow + " 行" + label + "无效：" + text);
        }
    }

    private static boolean isBlankRow(Row row, Map<Integer, String> colToField) {
        for (Integer col : colToField.keySet()) {
            if (!isBlank(cellText(row.getCell(col)))) {
                return false;
            }
        }
        return true;
    }

    private static Workbook openWorkbook(InputStream inputStream, String filename) throws Exception {
        String lower = filename == null ? "" : filename.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".xls") && !lower.endsWith(".xlsx")) {
            return new HSSFWorkbook(inputStream);
        }
        return new XSSFWorkbook(inputStream);
    }

    private static String cellText(Cell cell) {
        if (cell == null || cell.getCellType() == CellType.BLANK) {
            return null;
        }
        String text = FORMATTER.formatCellValue(cell);
        return text == null ? null : text.trim();
    }

    private static boolean isBlank(String text) {
        return text == null || text.isEmpty();
    }
}
