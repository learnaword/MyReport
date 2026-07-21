package com.myreport.entity;

import java.time.LocalDateTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Lob;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * AI 简化报告运行实例。
 *
 * @see docs/ai_simple_report/数据库设计.md
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
        name = "simple_report_run",
        indexes = {
                @Index(name = "idx_srr_report_status", columnList = "report_id,run_status"),
                @Index(name = "idx_srr_status_create", columnList = "run_status,create_time"),
                @Index(name = "idx_srr_create_time", columnList = "create_time"),
                @Index(name = "uk_srr_engine_report_id", columnList = "engine_report_id", unique = true)
        }
)
public class SimpleReportRun {

    public static final int STATUS_PENDING_CONFIRM = 1;
    public static final int STATUS_GENERATING = 2;
    public static final int STATUS_SUCCESS = 3;
    public static final int STATUS_FAILED = 4;
    public static final int STATUS_CANCELLED = 5;

    public static final int TRIGGER_MANUAL = 0;
    public static final int TRIGGER_SCHEDULE = 1;

    /** Spire/Redis reportId = BASE + run.id */
    public static final int ENGINE_ID_BASE = 1_000_000_000;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "report_id", nullable = false)
    private Long reportId;

    @Column(name = "report_name", length = 128, nullable = false)
    private String reportName;

    @Column(name = "user_prompt", length = 2000)
    private String userPrompt;

    @Lob
    @Column(name = "plan_md", columnDefinition = "MEDIUMTEXT")
    private String planMd;

    @Lob
    @Column(name = "config_snapshot_json", nullable = false, columnDefinition = "MEDIUMTEXT")
    private String configSnapshotJson;

    @Column(name = "run_status", nullable = false)
    private Integer runStatus;

    @Column(name = "engine_report_id")
    private Integer engineReportId;

    @Column(name = "warn_count", nullable = false)
    private Integer warnCount;

    @Column(name = "file_path", length = 1024)
    private String filePath;

    @Column(name = "delivery_path", length = 1024)
    private String deliveryPath;

    @Column(name = "fail_message", length = 512)
    private String failMessage;

    @Column(name = "trigger_type", nullable = false)
    private Integer triggerType;

    @Column(name = "confirmed_time")
    private LocalDateTime confirmedTime;

    @Column(name = "finished_time")
    private LocalDateTime finishedTime;

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
        if (warnCount == null) {
            warnCount = 0;
        }
        if (triggerType == null) {
            triggerType = TRIGGER_MANUAL;
        }
    }

    @PreUpdate
    public void preUpdate() {
        updateTime = LocalDateTime.now();
    }
}
