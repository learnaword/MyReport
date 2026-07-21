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
 * 报告模版节点（标题 / 指标邻接表）。
 *
 * @see docs/report_template_config/数据库设计.md
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
        name = "report_template_node",
        indexes = {
                @Index(name = "idx_template_id", columnList = "template_id"),
                @Index(name = "idx_parent_id", columnList = "parent_id"),
                @Index(name = "idx_template_parent_sort", columnList = "template_id,parent_id,sort_order"),
                @Index(name = "idx_node_deleted", columnList = "deleted")
        }
)
public class ReportTemplateNode {

    public static final String TYPE_TITLE = "TITLE";
    public static final String TYPE_METRIC = "METRIC";

    public static final String FORMAT_PERCENT = "PERCENT";
    public static final String FORMAT_COUNT = "COUNT";

    public static final String DISPLAY_TABLE = "TABLE";
    public static final String DISPLAY_CHART = "CHART";

    public static final String CHART_BAR = "BAR";
    public static final String CHART_PIE = "PIE";
    public static final String CHART_LINE = "LINE";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "template_id", nullable = false)
    private Long templateId;

    @Column(name = "parent_id")
    private Long parentId;

    @Column(name = "node_type", length = 16, nullable = false)
    private String nodeType;

    @Column(name = "level", nullable = false)
    private Integer level;

    @Column(name = "name", length = 256, nullable = false)
    private String name;

    @Column(name = "intro", length = 2000)
    private String intro;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;

    @Column(name = "stat_field", length = 128)
    private String statField;

    @Column(name = "value_format", length = 16)
    private String valueFormat;

    @Column(name = "display_type", length = 16)
    private String displayType;

    @Column(name = "chart_style", length = 16)
    private String chartStyle;

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
        if (sortOrder == null) {
            sortOrder = 0;
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
