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
 * 报告模版头。
 *
 * @see docs/report_template_config/数据库设计.md
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
        name = "report_template",
        indexes = {
                @Index(name = "idx_status_update", columnList = "status,update_time"),
                @Index(name = "idx_deleted", columnList = "deleted")
        }
)
public class ReportTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", length = 128, nullable = false)
    private String name;

    @Column(name = "description", length = 512)
    private String description;

    /** 1 启用 / 0 停用 */
    @Column(name = "status", nullable = false)
    private Integer status;

    /** 封面图 URL 或路径 */
    @Column(name = "cover_image", length = 1024)
    private String coverImage;

    /** 底图 URL 或路径 */
    @Column(name = "back_cover_image", length = 1024)
    private String backCoverImage;

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
        if (status == null) {
            status = 1;
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
