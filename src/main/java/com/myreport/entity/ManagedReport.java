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

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 报告实例（管理台报告管理）。
 *
 * @see docs/report_manage/数据库设计.md
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
        name = "managed_report",
        indexes = {
                @Index(name = "idx_mr_school_id", columnList = "school_id"),
                @Index(name = "idx_mr_template_id", columnList = "template_id"),
                @Index(name = "idx_mr_status_update", columnList = "generate_status,update_time"),
                @Index(name = "idx_mr_deleted", columnList = "deleted"),
                @Index(name = "idx_mr_school_template_deleted", columnList = "school_id,template_id,deleted")
        }
)
public class ManagedReport {

    public static final int STATUS_DRAFT = 0;
    public static final int STATUS_GENERATING = 1;
    public static final int STATUS_SUCCESS = 2;
    public static final int STATUS_FAILED = 3;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", length = 128, nullable = false)
    private String name;

    @Column(name = "school_id", nullable = false)
    private Long schoolId;

    @Column(name = "template_id", nullable = false)
    private Long templateId;

    /** 0草稿 1生成中 2成功 3失败 */
    @Column(name = "generate_status", nullable = false)
    private Integer generateStatus;

    @Column(name = "file_path", length = 1024)
    private String filePath;

    @Column(name = "fail_message", length = 512)
    private String failMessage;

    @Column(name = "last_generate_time")
    private LocalDateTime lastGenerateTime;

    /** 0 未删 / 1 已删 */
    @JsonIgnore
    @Column(name = "deleted", nullable = false)
    private Integer deleted;

    @JsonIgnore
    @Column(name = "delete_time")
    private LocalDateTime deleteTime;

    @Column(name = "create_time", nullable = false)
    private LocalDateTime createTime;

    @Column(name = "update_time", nullable = false)
    private LocalDateTime updateTime;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        if (createTime == null) {
            createTime = now;
        }
        updateTime = now;
        if (generateStatus == null) {
            generateStatus = STATUS_DRAFT;
        }
        if (deleted == null) {
            deleted = 0;
        }
    }

    @PreUpdate
    public void preUpdate() {
        updateTime = LocalDateTime.now();
    }
}
