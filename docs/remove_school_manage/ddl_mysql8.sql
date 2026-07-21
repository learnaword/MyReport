-- remove_school_manage：学校 name 唯一 + 默认校种子 + 报告模版唯一
-- 目标库：MySQL 8。执行前请备份；加唯一前先查重。
-- 详见 docs/remove_school_manage/数据库设计.md

-- 1) school.name 查重
-- SELECT name, COUNT(*) AS cnt FROM school GROUP BY name HAVING cnt > 1;

-- 2) school.name 唯一（若已存在同名索引可跳过）
ALTER TABLE school
  ADD UNIQUE KEY uk_school_name (name);

-- 3) 种子默认校
INSERT INTO school (name, create_time, update_time)
SELECT '武汉大学', NOW(), NOW()
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM school WHERE name = '武汉大学' LIMIT 1);

-- 4) managed_report：同模版多份未删查重
-- SELECT template_id, COUNT(*) AS cnt FROM managed_report WHERE deleted = 0 GROUP BY template_id HAVING cnt > 1;

-- 5a) 若尚无 active_school_template 生成列，直接加 active_template：
ALTER TABLE managed_report
  ADD COLUMN active_template VARCHAR(32)
    GENERATED ALWAYS AS (IF(deleted = 0, CAST(template_id AS CHAR), NULL)) STORED
    COMMENT '未删时模版唯一键；已删为NULL' AFTER update_time,
  ADD UNIQUE KEY uk_active_template (active_template);

-- 5b) 若已有 active_school_template，先执行再跑 5a：
-- ALTER TABLE managed_report
--   DROP INDEX uk_active_school_template,
--   DROP COLUMN active_school_template;
