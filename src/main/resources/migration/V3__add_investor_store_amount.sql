-- ============================================================
-- Migration V3: 为investor_store表增加invest_amount字段
-- ============================================================

-- SQLite支持ADD COLUMN
ALTER TABLE investor_store ADD COLUMN invest_amount REAL;
