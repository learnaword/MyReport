-- 报告实例表（正式环境建议显式执行；JPA ddl-auto=update 可建基础列）
-- 见 docs/report_manage/数据库设计.md

CREATE TABLE IF NOT EXISTS managed_report (
  id                      BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键，兼作生成侧 reportId',
  name                    VARCHAR(128)   NOT NULL COMMENT '报告名称',
  school_id               BIGINT         NOT NULL COMMENT '学校ID',
  template_id             BIGINT         NOT NULL COMMENT '模版ID',
  generate_status         TINYINT        NOT NULL DEFAULT 0 COMMENT '0草稿 1生成中 2成功 3失败',
  file_path               VARCHAR(1024)  DEFAULT NULL COMMENT '成功生成的docx路径',
  fail_message            VARCHAR(512)   DEFAULT NULL COMMENT '最近失败原因摘要',
  last_generate_time      DATETIME       DEFAULT NULL COMMENT '最近生成相关时间',
  deleted                 TINYINT        NOT NULL DEFAULT 0 COMMENT '0未删 1已删',
  delete_time             DATETIME       DEFAULT NULL COMMENT '软删时间',
  create_time             DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time             DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  KEY idx_mr_school_id (school_id),
  KEY idx_mr_template_id (template_id),
  KEY idx_mr_status_update (generate_status, update_time),
  KEY idx_mr_deleted (deleted),
  KEY idx_mr_school_template_deleted (school_id, template_id, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='报告实例（管理台报告管理）';
