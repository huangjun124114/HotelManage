-- ============================================================
-- 林夕置业经营日报系统 - 数据库拆分迁移脚本 V2
-- 版本: V260515
-- 日期: 2026-05-17
-- 说明: 补充 daily_extension 表的历史数据迁移
--       V1迁移了daily_channel数据，但daily_extension表缺少历史记录
--       此脚本为每条daily_report创建对应的daily_extension记录
-- ============================================================

-- SQLite适配版：为每条没有对应daily_extension的daily_report创建extension记录
-- 将daily_report中的ext1-ext10迁移到daily_extension的ext_field_1到ext_field_10
-- ext_field_11到ext_field_30暂时为NULL（未来扩展用）

INSERT INTO daily_extension (
    daily_report_id, store_id, report_date, template_id,
    ext_field_1, ext_field_2, ext_field_3, ext_field_4, ext_field_5,
    ext_field_6, ext_field_7, ext_field_8, ext_field_9, ext_field_10,
    ext_field_11, ext_field_12, ext_field_13, ext_field_14, ext_field_15,
    ext_field_16, ext_field_17, ext_field_18, ext_field_19, ext_field_20,
    ext_field_21, ext_field_22, ext_field_23, ext_field_24, ext_field_25,
    ext_field_26, ext_field_27, ext_field_28, ext_field_29, ext_field_30,
    deleted, create_time, update_time
)
SELECT
    dr.id, dr.store_id, dr.report_date, dr.template_id,
    dr.ext1, dr.ext2, dr.ext3, dr.ext4, dr.ext5,
    dr.ext6, dr.ext7, dr.ext8, dr.ext9, dr.ext10,
    NULL, NULL, NULL, NULL, NULL,
    NULL, NULL, NULL, NULL, NULL,
    NULL, NULL, NULL, NULL, NULL,
    NULL, NULL, NULL, NULL, NULL,
    0, datetime('now','localtime'), datetime('now','localtime')
FROM daily_report dr
WHERE NOT EXISTS (
    SELECT 1 FROM daily_extension de WHERE de.daily_report_id = dr.id
);

-- 验证查询（迁移后执行，确认无遗漏）
-- SELECT COUNT(*) AS missing_extension
-- FROM daily_report dr
-- WHERE NOT EXISTS (SELECT 1 FROM daily_extension de WHERE de.daily_report_id = dr.id);
-- 期望结果: 0
