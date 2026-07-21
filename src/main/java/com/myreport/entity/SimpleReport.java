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
 * AI 简化报告配置。
 *
 * @see docs/ai_simple_report/数据库设计.md
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
        name = "simple_report",
        indexes = {
                @Index(name = "idx_sr_status_deleted", columnList = "status,deleted"),
                @Index(name = "idx_sr_schedule", columnList = "schedule_enabled,deleted"),
                @Index(name = "idx_sr_update_time", columnList = "update_time"),
                @Index(name = "idx_sr_name_deleted", columnList = "name,deleted")
        }
)
public class SimpleReport {

    public static final int STATUS_DISABLED = 0;
    public static final int STATUS_ENABLED = 1;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", length = 128, nullable = false)
    private String name;

    /** 1 启用 / 0 停用 */
    @Column(name = "status", nullable = false)
    private Integer status;

    @Column(name = "delivery_dir", length = 512, nullable = false)
    private String deliveryDir;

    /** 生成成功后通知邮箱（QQ 等），多个用英文逗号分隔；空则不发信 */
    @Column(name = "notify_email", length = 512)
    private String notifyEmail;

    @Column(name = "schedule_enabled", nullable = false)
    private Integer scheduleEnabled;

    @Column(name = "cron_expr", length = 64)
    private String cronExpr;

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
        if (status == null) {
            status = STATUS_ENABLED;
        }
        if (scheduleEnabled == null) {
            scheduleEnabled = 0;
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
