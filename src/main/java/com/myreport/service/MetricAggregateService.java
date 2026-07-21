package com.myreport.service;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.myreport.entity.GradEmploymentRecord;
import com.myreport.entity.ReportTemplateNode;
import com.myreport.repository.GradEmploymentRecordRepository;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;

/**
 * 按学校就业数据与模版指标配置做 COUNT / PERCENT 聚合。
 */
@Service
public class MetricAggregateService {

    private static final Logger logger = Logger.getLogger(MetricAggregateService.class);

    private static final Map<String, Function<GradEmploymentRecord, String>> FIELD_EXTRACTORS =
            buildFieldExtractors();

    private final GradEmploymentRecordRepository recordRepository;

    public MetricAggregateService(GradEmploymentRecordRepository recordRepository) {
        this.recordRepository = recordRepository;
    }

    /**
     * @param schoolId 学校
     * @param metrics  模版 METRIC 节点（Map：id/name/statField/valueFormat 等）
     * @param parentIntroByMetricId 指标 → 最近祖先 TITLE intro（用于 strText）
     * @return nodeId → 指标结果 JSON；算失败的节点不出现在 map 中
     */
    public Map<Long, JSONObject> aggregate(Long schoolId,
                                           List<Map<String, Object>> metrics,
                                           Map<Long, String> parentIntroByMetricId) {
        Map<Long, JSONObject> result = new LinkedHashMap<Long, JSONObject>();
        if (schoolId == null || metrics == null || metrics.isEmpty()) {
            return result;
        }
        List<GradEmploymentRecord> rows = recordRepository.findBySchoolId(schoolId);
        if (rows == null) {
            rows = Collections.emptyList();
        }

        for (Map<String, Object> metric : metrics) {
            if (metric == null) {
                continue;
            }
            Long nodeId = toLong(metric.get("id"));
            String name = metric.get("name") == null ? "" : String.valueOf(metric.get("name"));
            String statField = metric.get("statField") == null ? null : String.valueOf(metric.get("statField"));
            String valueFormat = metric.get("valueFormat") == null
                    ? ReportTemplateNode.FORMAT_COUNT
                    : String.valueOf(metric.get("valueFormat"));
            if (nodeId == null || StringUtils.isBlank(statField)) {
                logger.warn("skip metric without id/statField, name=" + name);
                continue;
            }
            Function<GradEmploymentRecord, String> extractor = resolveExtractor(statField);
            if (extractor == null) {
                logger.warn("skip metric unknown statField=" + statField + ", id=" + nodeId);
                continue;
            }

            Map<String, Long> groupCounts = new LinkedHashMap<String, Long>();
            long nonNullTotal = 0L;
            for (GradEmploymentRecord row : rows) {
                String raw = extractor.apply(row);
                if (StringUtils.isBlank(raw)) {
                    continue;
                }
                String key = raw.trim();
                nonNullTotal++;
                Long c = groupCounts.get(key);
                groupCounts.put(key, c == null ? 1L : c + 1L);
            }
            if (nonNullTotal == 0L || groupCounts.isEmpty()) {
                logger.warn("skip metric empty data, statField=" + statField + ", id=" + nodeId);
                continue;
            }

            boolean percent = ReportTemplateNode.FORMAT_PERCENT.equalsIgnoreCase(valueFormat);
            if (percent && nonNullTotal <= 0L) {
                continue;
            }

            List<Map.Entry<String, Long>> entries = new ArrayList<Map.Entry<String, Long>>(groupCounts.entrySet());
            Collections.sort(entries, new Comparator<Map.Entry<String, Long>>() {
                @Override
                public int compare(Map.Entry<String, Long> a, Map.Entry<String, Long> b) {
                    int byCount = Long.compare(b.getValue(), a.getValue());
                    if (byCount != 0) {
                        return byCount;
                    }
                    return a.getKey().compareTo(b.getKey());
                }
            });

            JSONArray tableList = new JSONArray();
            for (Map.Entry<String, Long> e : entries) {
                JSONArray row = new JSONArray();
                row.add(e.getKey());
                if (percent) {
                    BigDecimal pct = BigDecimal.valueOf(e.getValue())
                            .multiply(BigDecimal.valueOf(100))
                            .divide(BigDecimal.valueOf(nonNullTotal), 2, RoundingMode.HALF_UP);
                    row.add(pct.toPlainString() + "%");
                } else {
                    row.add(String.valueOf(e.getValue()));
                }
                tableList.add(row);
            }

            JSONArray headList = new JSONArray();
            JSONArray headRow = new JSONArray();
            headRow.add("名称层");
            headRow.add("数据");
            headList.add(headRow);

            String intro = parentIntroByMetricId == null ? "" : parentIntroByMetricId.get(nodeId);
            if (intro == null) {
                intro = "";
            }

            JSONObject data = new JSONObject(true);
            data.put("strText", intro);
            data.put("strText2", "");
            data.put("headList", headList);
            data.put("chartDataList", new JSONArray());
            data.put("tableList", tableList);
            data.put("cellHeadList", new JSONArray());
            data.put("title", name);
            data.put("cellTableList", new JSONArray());
            result.put(nodeId, data);
        }
        return result;
    }

    public static boolean isKnownStatField(String statField) {
        return resolveExtractor(statField) != null;
    }

    private static Function<GradEmploymentRecord, String> resolveExtractor(String statField) {
        if (StringUtils.isBlank(statField)) {
            return null;
        }
        String key = normalizeFieldKey(statField);
        return FIELD_EXTRACTORS.get(key);
    }

    private static String normalizeFieldKey(String statField) {
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

    private static Long toLong(Object v) {
        if (v == null) {
            return null;
        }
        if (v instanceof Number) {
            return ((Number) v).longValue();
        }
        try {
            return Long.parseLong(String.valueOf(v).trim());
        } catch (Exception e) {
            return null;
        }
    }

    private static Map<String, Function<GradEmploymentRecord, String>> buildFieldExtractors() {
        Map<String, Function<GradEmploymentRecord, String>> m =
                new HashMap<String, Function<GradEmploymentRecord, String>>();
        put(m, "student_no", GradEmploymentRecord::getStudentNo);
        put(m, "student_name", GradEmploymentRecord::getStudentName);
        put(m, "gender_name", GradEmploymentRecord::getGenderName);
        put(m, "education_name", GradEmploymentRecord::getEducationName);
        put(m, "college_name", GradEmploymentRecord::getCollegeName);
        put(m, "major_name", GradEmploymentRecord::getMajorName);
        put(m, "political_status_name", GradEmploymentRecord::getPoliticalStatusName);
        put(m, "ethnicity_name", GradEmploymentRecord::getEthnicityName);
        put(m, "hardship_type_name", GradEmploymentRecord::getHardshipTypeName);
        put(m, "source_province", GradEmploymentRecord::getSourceProvince);
        put(m, "source_city", GradEmploymentRecord::getSourceCity);
        put(m, "source_in_out_province", GradEmploymentRecord::getSourceInOutProvince);
        put(m, "source_geo3", GradEmploymentRecord::getSourceGeo3);
        put(m, "source_econ4", GradEmploymentRecord::getSourceEcon4);
        put(m, "source_geo7", GradEmploymentRecord::getSourceGeo7);
        put(m, "source_econ8", GradEmploymentRecord::getSourceEcon8);
        put(m, "source_gd_region", GradEmploymentRecord::getSourceGdRegion);
        put(m, "destination_raw", GradEmploymentRecord::getDestinationRaw);
        put(m, "destination_category", GradEmploymentRecord::getDestinationCategory);
        put(m, "destination_major_category", GradEmploymentRecord::getDestinationMajorCategory);
        put(m, "flow_analysis_group", GradEmploymentRecord::getFlowAnalysisGroup);
        put(m, "employer_name", GradEmploymentRecord::getEmployerName);
        put(m, "employer_location", GradEmploymentRecord::getEmployerLocation);
        put(m, "job_province", GradEmploymentRecord::getJobProvince);
        put(m, "job_city", GradEmploymentRecord::getJobCity);
        put(m, "job_geo3", GradEmploymentRecord::getJobGeo3);
        put(m, "job_econ4", GradEmploymentRecord::getJobEcon4);
        put(m, "job_geo7", GradEmploymentRecord::getJobGeo7);
        put(m, "job_northeast", GradEmploymentRecord::getJobNortheast);
        put(m, "job_econ8", GradEmploymentRecord::getJobEcon8);
        put(m, "job_econ8_2", GradEmploymentRecord::getJobEcon82);
        put(m, "job_city_tier", GradEmploymentRecord::getJobCityTier);
        put(m, "job_gd_region", GradEmploymentRecord::getJobGdRegion);
        put(m, "job_in_out_province", GradEmploymentRecord::getJobInOutProvince);
        put(m, "job_school_city", GradEmploymentRecord::getJobSchoolCity);
        put(m, "job_gba", GradEmploymentRecord::getJobGba);
        put(m, "job_west", GradEmploymentRecord::getJobWest);
        put(m, "job_belt_road", GradEmploymentRecord::getJobBeltRoad);
        put(m, "job_jjj", GradEmploymentRecord::getJobJjj);
        put(m, "job_yangtze_belt", GradEmploymentRecord::getJobYangtzeBelt);
        put(m, "job_yellow_river", GradEmploymentRecord::getJobYellowRiver);
        put(m, "job_chengyu", GradEmploymentRecord::getJobChengyu);
        put(m, "job_in_out_province_2", GradEmploymentRecord::getJobInOutProvince2);
        put(m, "in_province_source_job_cross", GradEmploymentRecord::getInProvinceSourceJobCross);
        put(m, "employer_nature", GradEmploymentRecord::getEmployerNature);
        put(m, "employer_major_type", GradEmploymentRecord::getEmployerMajorType);
        put(m, "employer_industry", GradEmploymentRecord::getEmployerIndustry);
        put(m, "job_occupation", GradEmploymentRecord::getJobOccupation);
        put(m, "unit_name_after_occupation", GradEmploymentRecord::getUnitNameAfterOccupation);
        put(m, "further_study_school_name", GradEmploymentRecord::getFurtherStudySchoolName);
        put(m, "abroad_country_region", GradEmploymentRecord::getAbroadCountryRegion);
        put(m, "qs_rank", GradEmploymentRecord::getQsRank);
        put(m, "us_rank", GradEmploymentRecord::getUsRank);
        put(m, "further_study_level", GradEmploymentRecord::getFurtherStudyLevel);
        // graduation_year 为数字，转字符串参与分组
        put(m, "graduation_year", r -> r.getGraduationYear() == null ? null : String.valueOf(r.getGraduationYear()));
        return m;
    }

    private static void put(Map<String, Function<GradEmploymentRecord, String>> m,
                            String column,
                            Function<GradEmploymentRecord, String> fn) {
        m.put(normalizeFieldKey(column), fn);
        // camelCase 别名：college_name -> collegename 已覆盖；另存去掉下划线后的原名
        String camel = toCamel(column);
        if (camel != null) {
            m.put(normalizeFieldKey(camel), fn);
        }
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
}
