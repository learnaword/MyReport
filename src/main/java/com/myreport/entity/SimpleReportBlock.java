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
 * AI 简化报告数据区块（多接口）。
 *
 * @see docs/ai_simple_report/数据库设计.md
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
        name = "simple_report_block",
        indexes = {
                @Index(name = "idx_srb_report_sort", columnList = "report_id,sort_order"),
                @Index(name = "idx_srb_report_id", columnList = "report_id")
        }
)
public class SimpleReportBlock {

    public static final String METHOD_GET = "GET";
    public static final String METHOD_POST = "POST";

    public static final String STYLE_TABLE = "TABLE";
    public static final String STYLE_BAR = "BAR";
    public static final String STYLE_PIE = "PIE";
    public static final String STYLE_LINE = "LINE";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "report_id", nullable = false)
    private Long reportId;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;

    @Column(name = "title", length = 128, nullable = false)
    private String title;

    @Column(name = "http_method", length = 8, nullable = false)
    private String httpMethod;

    @Column(name = "url", length = 1024, nullable = false)
    private String url;

    @Lob
    @Column(name = "query_json", columnDefinition = "TEXT")
    private String queryJson;

    @Lob
    @Column(name = "body_json", columnDefinition = "TEXT")
    private String bodyJson;

    @Column(name = "render_style", length = 16, nullable = false)
    private String renderStyle;

    @Column(name = "prompt_hint", length = 512)
    private String promptHint;

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
        if (httpMethod == null) {
            httpMethod = METHOD_GET;
        }
    }

    @PreUpdate
    public void preUpdate() {
        updateTime = LocalDateTime.now();
    }
}
