package com.myreport.entity;

import java.time.LocalDateTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.UniqueConstraint;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 毕业生就业去向宽表（对齐现行 56 列表头）。
 *
 * @see docs/graduate_employment/数据库设计.md
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
        name = "grad_employment_record",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_school_student_year",
                        columnNames = {"school_id", "student_no", "graduation_year"})
        },
        indexes = {
                @Index(name = "idx_school_id", columnList = "school_id"),
                @Index(name = "idx_student_no", columnList = "student_no"),
                @Index(name = "idx_grad_year", columnList = "graduation_year"),
                @Index(name = "idx_college_major", columnList = "college_name,major_name"),
                @Index(name = "idx_dest_category", columnList = "destination_category"),
                @Index(name = "idx_job_province_city", columnList = "job_province,job_city"),
                @Index(name = "idx_batch", columnList = "batch_id")
        }
)
public class GradEmploymentRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 学校 ID，关联 school.id */
    @Column(name = "school_id")
    private Long schoolId;

    /**
     * 导入时 Excel「学校名称」列，不落库；由服务按名称解析为 {@link #schoolId}。
     */
    @Transient
    private String importSchoolName;

    /** 学号 */
    @Column(name = "student_no", length = 32)
    private String studentNo;

    /** 姓名 */
    @Column(name = "student_name", length = 64)
    private String studentName;

    /** 毕业年份 */
    @Column(name = "graduation_year")
    private Integer graduationYear;

    /** 性别 */
    @Column(name = "gender_name", length = 16)
    private String genderName;

    /** 学历 */
    @Column(name = "education_name", length = 32)
    private String educationName;

    /** 学院名称 */
    @Column(name = "college_name", length = 128)
    private String collegeName;

    /** 专业名称 */
    @Column(name = "major_name", length = 128)
    private String majorName;

    /** 政治面貌 */
    @Column(name = "political_status_name", length = 32)
    private String politicalStatusName;

    /** 民族 */
    @Column(name = "ethnicity_name", length = 32)
    private String ethnicityName;

    /** 困难生类别 */
    @Column(name = "hardship_type_name", length = 64)
    private String hardshipTypeName;

    /** 生源省 */
    @Column(name = "source_province", length = 32)
    private String sourceProvince;

    /** 生源市 */
    @Column(name = "source_city", length = 64)
    private String sourceCity;

    /** 省内外生源 */
    @Column(name = "source_in_out_province", length = 32)
    private String sourceInOutProvince;

    /** 三大地理区域生源 */
    @Column(name = "source_geo3", length = 32)
    private String sourceGeo3;

    /** 四大经济区域生源 */
    @Column(name = "source_econ4", length = 32)
    private String sourceEcon4;

    /** 七大地理区域生源 */
    @Column(name = "source_geo7", length = 32)
    private String sourceGeo7;

    /** 八大经济区生源 */
    @Column(name = "source_econ8", length = 64)
    private String sourceEcon8;

    /** 广东省生源区域 */
    @Column(name = "source_gd_region", length = 64)
    private String sourceGdRegion;

    /** 原始毕业去向 */
    @Column(name = "destination_raw", length = 128)
    private String destinationRaw;

    /** 毕业去向类别 */
    @Column(name = "destination_category", length = 64)
    private String destinationCategory;

    /** 毕业去向大类 */
    @Column(name = "destination_major_category", length = 64)
    private String destinationMajorCategory;

    /** 就业流向分析群体 */
    @Column(name = "flow_analysis_group", length = 64)
    private String flowAnalysisGroup;

    /** 单位名称（就业，第 1 次「单位名称」） */
    @Column(name = "employer_name", length = 256)
    private String employerName;

    /** 单位所在地 */
    @Column(name = "employer_location", length = 128)
    private String employerLocation;

    /** 就业省 */
    @Column(name = "job_province", length = 32)
    private String jobProvince;

    /** 就业市 */
    @Column(name = "job_city", length = 64)
    private String jobCity;

    /** 三大地理区域就业 */
    @Column(name = "job_geo3", length = 32)
    private String jobGeo3;

    /** 四大经济区域就业 */
    @Column(name = "job_econ4", length = 32)
    private String jobEcon4;

    /** 七大地理区域就业 */
    @Column(name = "job_geo7", length = 32)
    private String jobGeo7;

    /** 东北地区就业 */
    @Column(name = "job_northeast", length = 32)
    private String jobNortheast;

    /** 八大经济区就业 */
    @Column(name = "job_econ8", length = 64)
    private String jobEcon8;

    /** 八大经济区就业2 */
    @Column(name = "job_econ8_2", length = 64)
    private String jobEcon82;

    /** 就业城市类别 */
    @Column(name = "job_city_tier", length = 32)
    private String jobCityTier;

    /** 广东省就业区域 */
    @Column(name = "job_gd_region", length = 64)
    private String jobGdRegion;

    /** 省内外就业 */
    @Column(name = "job_in_out_province", length = 32)
    private String jobInOutProvince;

    /** 学校属地市就业 */
    @Column(name = "job_school_city", length = 32)
    private String jobSchoolCity;

    /** 粤港澳大湾区就业 */
    @Column(name = "job_gba", length = 32)
    private String jobGba;

    /** 西部地区就业 */
    @Column(name = "job_west", length = 32)
    private String jobWest;

    /** 一带一路地区就业 */
    @Column(name = "job_belt_road", length = 32)
    private String jobBeltRoad;

    /** 京津冀地区就业 */
    @Column(name = "job_jjj", length = 32)
    private String jobJjj;

    /** 长江经济带就业 */
    @Column(name = "job_yangtze_belt", length = 32)
    private String jobYangtzeBelt;

    /** 黄河流域就业 */
    @Column(name = "job_yellow_river", length = 32)
    private String jobYellowRiver;

    /** 成渝经济圈就业 */
    @Column(name = "job_chengyu", length = 32)
    private String jobChengyu;

    /** 省内外就业-2 */
    @Column(name = "job_in_out_province_2", length = 32)
    private String jobInOutProvince2;

    /** 省内生源就业交叉 */
    @Column(name = "in_province_source_job_cross", length = 64)
    private String inProvinceSourceJobCross;

    /** 单位性质 */
    @Column(name = "employer_nature", length = 64)
    private String employerNature;

    /** 单位大类 */
    @Column(name = "employer_major_type", length = 64)
    private String employerMajorType;

    /** 单位所属行业 */
    @Column(name = "employer_industry", length = 128)
    private String employerIndustry;

    /** 就业职业 */
    @Column(name = "job_occupation", length = 128)
    private String jobOccupation;

    /** 单位名称（就业职业后，第 2 次「单位名称」） */
    @Column(name = "unit_name_after_occupation", length = 256)
    private String unitNameAfterOccupation;

    /** 单位名称（升学院校，第 3 次「单位名称」） */
    @Column(name = "further_study_school_name", length = 256)
    private String furtherStudySchoolName;

    /** 留学国家/地区 */
    @Column(name = "abroad_country_region", length = 64)
    private String abroadCountryRegion;

    /** QS排名 */
    @Column(name = "qs_rank", length = 32)
    private String qsRank;

    /** US排名 */
    @Column(name = "us_rank", length = 32)
    private String usRank;

    /** 升学院校层次 */
    @Column(name = "further_study_level", length = 64)
    private String furtherStudyLevel;

    /** 导入批次 */
    @Column(name = "batch_id", length = 64)
    private String batchId;

    /** 创建时间 */
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    /** 更新时间 */
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
