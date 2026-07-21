package com.myreport.service;

import com.myreport.entity.GradEmploymentRecord;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;

/**
 * 就业宽表可统计字段目录：配置下拉与聚合白名单的单一事实源。
 * <p>
 * value = 库列名（snake_case）；label = 中文（对齐 graduate_employment 表头）。
 */
public final class GradEmploymentStatFieldCatalog {

    private static final List<FieldDef> DEFS;
    private static final Map<String, Function<GradEmploymentRecord, String>> EXTRACTORS;
    private static final Map<String, String> CANONICAL_BY_KEY;

    static {
        List<FieldDef> defs = new ArrayList<FieldDef>();
        add(defs, "student_no", "学号", GradEmploymentRecord::getStudentNo);
        add(defs, "student_name", "姓名", GradEmploymentRecord::getStudentName);
        add(defs, "graduation_year", "毕业年份",
                r -> r.getGraduationYear() == null ? null : String.valueOf(r.getGraduationYear()));
        add(defs, "gender_name", "性别", GradEmploymentRecord::getGenderName);
        add(defs, "education_name", "学历", GradEmploymentRecord::getEducationName);
        add(defs, "college_name", "学院名称", GradEmploymentRecord::getCollegeName);
        add(defs, "major_name", "专业名称", GradEmploymentRecord::getMajorName);
        add(defs, "political_status_name", "政治面貌", GradEmploymentRecord::getPoliticalStatusName);
        add(defs, "ethnicity_name", "民族", GradEmploymentRecord::getEthnicityName);
        add(defs, "hardship_type_name", "困难生类别", GradEmploymentRecord::getHardshipTypeName);
        add(defs, "source_province", "生源省", GradEmploymentRecord::getSourceProvince);
        add(defs, "source_city", "生源市", GradEmploymentRecord::getSourceCity);
        add(defs, "source_in_out_province", "省内外生源", GradEmploymentRecord::getSourceInOutProvince);
        add(defs, "source_geo3", "三大地理区域生源", GradEmploymentRecord::getSourceGeo3);
        add(defs, "source_econ4", "四大经济区域生源", GradEmploymentRecord::getSourceEcon4);
        add(defs, "source_geo7", "七大地理区域生源", GradEmploymentRecord::getSourceGeo7);
        add(defs, "source_econ8", "八大经济区生源", GradEmploymentRecord::getSourceEcon8);
        add(defs, "source_gd_region", "广东省生源区域", GradEmploymentRecord::getSourceGdRegion);
        add(defs, "destination_raw", "原始毕业去向", GradEmploymentRecord::getDestinationRaw);
        add(defs, "destination_category", "毕业去向类别", GradEmploymentRecord::getDestinationCategory);
        add(defs, "destination_major_category", "毕业去向大类", GradEmploymentRecord::getDestinationMajorCategory);
        add(defs, "flow_analysis_group", "就业流向分析群体", GradEmploymentRecord::getFlowAnalysisGroup);
        add(defs, "employer_name", "单位名称", GradEmploymentRecord::getEmployerName);
        add(defs, "employer_location", "单位所在地", GradEmploymentRecord::getEmployerLocation);
        add(defs, "job_province", "就业省", GradEmploymentRecord::getJobProvince);
        add(defs, "job_city", "就业市", GradEmploymentRecord::getJobCity);
        add(defs, "job_geo3", "三大地理区域就业", GradEmploymentRecord::getJobGeo3);
        add(defs, "job_econ4", "四大经济区域就业", GradEmploymentRecord::getJobEcon4);
        add(defs, "job_geo7", "七大地理区域就业", GradEmploymentRecord::getJobGeo7);
        add(defs, "job_northeast", "东北地区就业", GradEmploymentRecord::getJobNortheast);
        add(defs, "job_econ8", "八大经济区就业", GradEmploymentRecord::getJobEcon8);
        add(defs, "job_econ8_2", "八大经济区就业2", GradEmploymentRecord::getJobEcon82);
        add(defs, "job_city_tier", "就业城市类别", GradEmploymentRecord::getJobCityTier);
        add(defs, "job_gd_region", "广东省就业区域", GradEmploymentRecord::getJobGdRegion);
        add(defs, "job_in_out_province", "省内外就业", GradEmploymentRecord::getJobInOutProvince);
        add(defs, "job_school_city", "学校属地市就业", GradEmploymentRecord::getJobSchoolCity);
        add(defs, "job_gba", "粤港澳大湾区就业", GradEmploymentRecord::getJobGba);
        add(defs, "job_west", "西部地区就业", GradEmploymentRecord::getJobWest);
        add(defs, "job_belt_road", "一带一路地区就业", GradEmploymentRecord::getJobBeltRoad);
        add(defs, "job_jjj", "京津冀地区就业", GradEmploymentRecord::getJobJjj);
        add(defs, "job_yangtze_belt", "长江经济带就业", GradEmploymentRecord::getJobYangtzeBelt);
        add(defs, "job_yellow_river", "黄河流域就业", GradEmploymentRecord::getJobYellowRiver);
        add(defs, "job_chengyu", "成渝经济圈就业", GradEmploymentRecord::getJobChengyu);
        add(defs, "job_in_out_province_2", "省内外就业-2", GradEmploymentRecord::getJobInOutProvince2);
        add(defs, "in_province_source_job_cross", "省内生源就业交叉", GradEmploymentRecord::getInProvinceSourceJobCross);
        add(defs, "employer_nature", "单位性质", GradEmploymentRecord::getEmployerNature);
        add(defs, "employer_major_type", "单位大类", GradEmploymentRecord::getEmployerMajorType);
        add(defs, "employer_industry", "单位所属行业", GradEmploymentRecord::getEmployerIndustry);
        add(defs, "job_occupation", "就业职业", GradEmploymentRecord::getJobOccupation);
        add(defs, "unit_name_after_occupation", "单位名称", GradEmploymentRecord::getUnitNameAfterOccupation);
        add(defs, "further_study_school_name", "单位名称", GradEmploymentRecord::getFurtherStudySchoolName);
        add(defs, "abroad_country_region", "留学国家/地区", GradEmploymentRecord::getAbroadCountryRegion);
        add(defs, "qs_rank", "QS排名", GradEmploymentRecord::getQsRank);
        add(defs, "us_rank", "US排名", GradEmploymentRecord::getUsRank);
        add(defs, "further_study_level", "升学院校层次", GradEmploymentRecord::getFurtherStudyLevel);

        DEFS = Collections.unmodifiableList(defs);

        Map<String, Function<GradEmploymentRecord, String>> extractors =
                new HashMap<String, Function<GradEmploymentRecord, String>>();
        Map<String, String> canonical = new HashMap<String, String>();
        for (FieldDef def : defs) {
            bind(extractors, canonical, def.value, def.value, def.extractor);
            String camel = toCamel(def.value);
            if (camel != null && !camel.equals(def.value)) {
                bind(extractors, canonical, camel, def.value, def.extractor);
            }
        }
        EXTRACTORS = Collections.unmodifiableMap(extractors);
        CANONICAL_BY_KEY = Collections.unmodifiableMap(canonical);
    }

    private GradEmploymentStatFieldCatalog() {
    }

    /** 下拉选项：有序 [{value, label}, ...]。 */
    public static List<Map<String, String>> listOptions() {
        List<Map<String, String>> out = new ArrayList<Map<String, String>>(DEFS.size());
        for (FieldDef def : DEFS) {
            Map<String, String> item = new LinkedHashMap<String, String>();
            item.put("value", def.value);
            item.put("label", def.label);
            out.add(item);
        }
        return out;
    }

    public static boolean isKnown(String statField) {
        return canonicalize(statField) != null;
    }

    /** 将入站字段名解析为规范 snake_case；无法识别返回 null。 */
    public static String canonicalize(String statField) {
        if (StringUtils.isBlank(statField)) {
            return null;
        }
        return CANONICAL_BY_KEY.get(normalizeFieldKey(statField));
    }

    public static Function<GradEmploymentRecord, String> resolveExtractor(String statField) {
        if (StringUtils.isBlank(statField)) {
            return null;
        }
        return EXTRACTORS.get(normalizeFieldKey(statField));
    }

    public static int size() {
        return DEFS.size();
    }

    public static String labelOf(String value) {
        String canon = canonicalize(value);
        if (canon == null) {
            return null;
        }
        for (FieldDef def : DEFS) {
            if (def.value.equals(canon)) {
                return def.label;
            }
        }
        return null;
    }

    static String normalizeFieldKey(String statField) {
        String s = statField.trim();
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '_' || c == '-' || Character.isWhitespace(c)) {
                continue;
            }
            sb.append(Character.toLowerCase(c));
        }
        return sb.toString();
    }

    private static void add(List<FieldDef> defs,
                            String value,
                            String label,
                            Function<GradEmploymentRecord, String> extractor) {
        defs.add(new FieldDef(value, label, extractor));
    }

    private static void bind(Map<String, Function<GradEmploymentRecord, String>> extractors,
                             Map<String, String> canonical,
                             String alias,
                             String canonicalSnake,
                             Function<GradEmploymentRecord, String> extractor) {
        String key = normalizeFieldKey(alias);
        extractors.put(key, extractor);
        canonical.put(key, canonicalSnake);
    }

    private static String toCamel(String snake) {
        if (snake == null || !snake.contains("_")) {
            return snake;
        }
        String[] parts = snake.toLowerCase(Locale.ROOT).split("_");
        StringBuilder sb = new StringBuilder(parts[0]);
        for (int i = 1; i < parts.length; i++) {
            if (parts[i].isEmpty()) {
                continue;
            }
            sb.append(Character.toUpperCase(parts[i].charAt(0)));
            if (parts[i].length() > 1) {
                sb.append(parts[i].substring(1));
            }
        }
        return sb.toString();
    }

    private static final class FieldDef {
        private final String value;
        private final String label;
        private final Function<GradEmploymentRecord, String> extractor;

        private FieldDef(String value, String label, Function<GradEmploymentRecord, String> extractor) {
            this.value = value;
            this.label = label;
            this.extractor = extractor;
        }
    }
}
