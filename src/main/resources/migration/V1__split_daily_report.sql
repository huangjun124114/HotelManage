-- ============================================================
-- 林夕置业经营日报系统 - 数据库拆分迁移脚本
-- 版本: V260515
-- 日期: 2026-05-15
-- 说明: 将日报表从EAV模式拆分为3张表（基础信息+渠道评价+弹性域）
-- ============================================================

-- ============================================================
-- 第一步：创建备份表（原表备份）
-- ============================================================

-- 备份日报主表
ALTER TABLE daily_report RENAME TO daily_report_backup;

-- 备份日报字段值表（EAV）
ALTER TABLE daily_report_value RENAME TO daily_report_value_backup;

-- 备份日报摘要表
ALTER TABLE daily_report_summary RENAME TO daily_report_summary_backup;

-- 备份日报渠道表
ALTER TABLE daily_report_channel RENAME TO daily_report_channel_backup;

-- 备份日报平台表
ALTER TABLE daily_report_platform RENAME TO daily_report_platform_backup;

-- ============================================================
-- 第二步：创建新的日报基础信息表
-- ============================================================

CREATE TABLE daily_report (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    store_id BIGINT NOT NULL,
    store_name VARCHAR(100),
    store_code VARCHAR(50),
    report_date DATE NOT NULL,
    report_month VARCHAR(7) NOT NULL,
    week_day VARCHAR(10),
    template_id BIGINT,

    -- 房量字段
    own_room_count INT DEFAULT 0,
    hourly_room_count INT DEFAULT 0,
    repair_room_count INT DEFAULT 0,

    -- 间夜数字段
    room_nights DECIMAL(10,1) DEFAULT 0,
    walkin_room_nights DECIMAL(10,1) DEFAULT 0,
    ctrip_room_nights DECIMAL(10,1) DEFAULT 0,
    ly_room_nights DECIMAL(10,1) DEFAULT 0,
    qunar_room_nights DECIMAL(10,1) DEFAULT 0,
    zhixing_room_nights DECIMAL(10,1) DEFAULT 0,
    external_room_nights DECIMAL(10,1) DEFAULT 0,
    meituan_hotel_room_nights DECIMAL(10,1) DEFAULT 0,
    fliggy_room_nights DECIMAL(10,1) DEFAULT 0,
    douyin_room_nights DECIMAL(10,1) DEFAULT 0,
    xiaozhu_room_nights DECIMAL(10,1) DEFAULT 0,
    tujia_room_nights DECIMAL(10,1) DEFAULT 0,
    meituan_homestay_room_nights DECIMAL(10,1) DEFAULT 0,
    jiali_room_nights DECIMAL(10,1) DEFAULT 0,

    -- 经营指标（计算字段）
    occupancy_rate DECIMAL(10,4) DEFAULT 0,
    adr DECIMAL(12,2) DEFAULT 0,
    revpar DECIMAL(12,2) DEFAULT 0,

    -- 收入字段
    daily_room_fee DECIMAL(12,2) DEFAULT 0,
    hourly_room_fee DECIMAL(12,2) DEFAULT 0,
    other_fee DECIMAL(12,2) DEFAULT 0,
    total_revenue DECIMAL(12,2) DEFAULT 0,
    deposit_amount DECIMAL(12,2) DEFAULT 0,

    -- 扫码字段
    ctrip_scan_count INT DEFAULT 0,
    meituan_scan_count INT DEFAULT 0,

    -- 预留扩展字段（10个）
    ext1 VARCHAR(500),
    ext2 VARCHAR(500),
    ext3 VARCHAR(500),
    ext4 VARCHAR(500),
    ext5 VARCHAR(500),
    ext6 VARCHAR(500),
    ext7 VARCHAR(500),
    ext8 VARCHAR(500),
    ext9 VARCHAR(500),
    ext10 VARCHAR(500),

    -- 元数据字段
    status INT DEFAULT 0,
    abnormal_flag INT DEFAULT 0,
    abnormal_message VARCHAR(500),
    submit_user_id BIGINT,
    submit_time VARCHAR(50),
    lock_user_id BIGINT,
    lock_time VARCHAR(50),
    reject_reason VARCHAR(500),
    remark VARCHAR(500),
    deleted INT DEFAULT 0,
    create_time VARCHAR(50),
    create_by BIGINT,
    update_time VARCHAR(50),
    update_by BIGINT,

    UNIQUE KEY uk_store_date (store_id, report_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- 第三步：创建新的渠道评价表
-- ============================================================

CREATE TABLE daily_channel (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    daily_report_id BIGINT NOT NULL,
    store_id BIGINT NOT NULL,
    report_date DATE NOT NULL,
    template_id BIGINT,

    -- 好评数字段
    ctrip_good_review_count INT DEFAULT 0,
    meituan_good_review_count INT DEFAULT 0,
    ly_good_review_count INT DEFAULT 0,
    qunar_good_review_count INT DEFAULT 0,
    zhixing_good_review_count INT DEFAULT 0,
    fliggy_good_review_count INT DEFAULT 0,

    -- 评分字段
    ctrip_score DECIMAL(4,2) DEFAULT 0,
    ly_score DECIMAL(4,2) DEFAULT 0,
    qunar_score DECIMAL(4,2) DEFAULT 0,
    zhixing_score DECIMAL(4,2) DEFAULT 0,
    meituan_score DECIMAL(4,2) DEFAULT 0,
    fliggy_score DECIMAL(4,2) DEFAULT 0,

    -- 预留扩展字段（10个）
    ext1 VARCHAR(500),
    ext2 VARCHAR(500),
    ext3 VARCHAR(500),
    ext4 VARCHAR(500),
    ext5 VARCHAR(500),
    ext6 VARCHAR(500),
    ext7 VARCHAR(500),
    ext8 VARCHAR(500),
    ext9 VARCHAR(500),
    ext10 VARCHAR(500),

    -- 元数据字段
    deleted INT DEFAULT 0,
    create_time VARCHAR(50),
    update_time VARCHAR(50),

    UNIQUE KEY uk_report_id (daily_report_id),
    KEY idx_store_date (store_id, report_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- 第四步：创建新的弹性域表
-- ============================================================

CREATE TABLE daily_extension (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    daily_report_id BIGINT NOT NULL,
    store_id BIGINT NOT NULL,
    report_date DATE NOT NULL,
    template_id BIGINT,

    -- 30个弹性域字段
    ext_field_1 VARCHAR(500),
    ext_field_2 VARCHAR(500),
    ext_field_3 VARCHAR(500),
    ext_field_4 VARCHAR(500),
    ext_field_5 VARCHAR(500),
    ext_field_6 VARCHAR(500),
    ext_field_7 VARCHAR(500),
    ext_field_8 VARCHAR(500),
    ext_field_9 VARCHAR(500),
    ext_field_10 VARCHAR(500),
    ext_field_11 VARCHAR(500),
    ext_field_12 VARCHAR(500),
    ext_field_13 VARCHAR(500),
    ext_field_14 VARCHAR(500),
    ext_field_15 VARCHAR(500),
    ext_field_16 VARCHAR(500),
    ext_field_17 VARCHAR(500),
    ext_field_18 VARCHAR(500),
    ext_field_19 VARCHAR(500),
    ext_field_20 VARCHAR(500),
    ext_field_21 VARCHAR(500),
    ext_field_22 VARCHAR(500),
    ext_field_23 VARCHAR(500),
    ext_field_24 VARCHAR(500),
    ext_field_25 VARCHAR(500),
    ext_field_26 VARCHAR(500),
    ext_field_27 VARCHAR(500),
    ext_field_28 VARCHAR(500),
    ext_field_29 VARCHAR(500),
    ext_field_30 VARCHAR(500),

    -- 元数据字段
    deleted INT DEFAULT 0,
    create_time VARCHAR(50),
    update_time VARCHAR(50),

    UNIQUE KEY uk_report_id (daily_report_id),
    KEY idx_store_date (store_id, report_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- 第五步：创建弹性域定义表
-- ============================================================

CREATE TABLE extension_field_definition (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    field_code VARCHAR(50) NOT NULL,
    field_name VARCHAR(100) NOT NULL,
    table_type VARCHAR(20) NOT NULL,
    field_type VARCHAR(20) NOT NULL,
    default_value VARCHAR(500),
    min_value DECIMAL(20,4),
    max_value DECIMAL(20,4),
    max_length INT DEFAULT 500,
    options TEXT,
    required INT DEFAULT 0,
    visible INT DEFAULT 1,
    readonly INT DEFAULT 0,
    sort_no INT DEFAULT 0,
    status INT DEFAULT 1,
    remark VARCHAR(500),
    create_time VARCHAR(50),
    update_time VARCHAR(50),

    UNIQUE KEY uk_field_code_table (field_code, table_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- 第六步：初始化弹性域定义数据
-- ============================================================

INSERT INTO extension_field_definition (field_code, field_name, table_type, field_type, sort_no, status) VALUES
-- 基础表预留字段
('ext1', '预留字段1', 'report', 'TEXT', 1, 0),
('ext2', '预留字段2', 'report', 'TEXT', 2, 0),
('ext3', '预留字段3', 'report', 'TEXT', 3, 0),
('ext4', '预留字段4', 'report', 'TEXT', 4, 0),
('ext5', '预留字段5', 'report', 'TEXT', 5, 0),
('ext6', '预留字段6', 'report', 'TEXT', 6, 0),
('ext7', '预留字段7', 'report', 'TEXT', 7, 0),
('ext8', '预留字段8', 'report', 'TEXT', 8, 0),
('ext9', '预留字段9', 'report', 'TEXT', 9, 0),
('ext10', '预留字段10', 'report', 'TEXT', 10, 0),
-- 渠道表预留字段
('ext1', '预留字段1', 'channel', 'TEXT', 1, 0),
('ext2', '预留字段2', 'channel', 'TEXT', 2, 0),
('ext3', '预留字段3', 'channel', 'TEXT', 3, 0),
('ext4', '预留字段4', 'channel', 'TEXT', 4, 0),
('ext5', '预留字段5', 'channel', 'TEXT', 5, 0),
('ext6', '预留字段6', 'channel', 'TEXT', 6, 0),
('ext7', '预留字段7', 'channel', 'TEXT', 7, 0),
('ext8', '预留字段8', 'channel', 'TEXT', 8, 0),
('ext9', '预留字段9', 'channel', 'TEXT', 9, 0),
('ext10', '预留字段10', 'channel', 'TEXT', 10, 0),
-- 弹性域表30个字段
('ext_field_1', '弹性域1', 'extension', 'TEXT', 1, 0),
('ext_field_2', '弹性域2', 'extension', 'TEXT', 2, 0),
('ext_field_3', '弹性域3', 'extension', 'TEXT', 3, 0),
('ext_field_4', '弹性域4', 'extension', 'TEXT', 4, 0),
('ext_field_5', '弹性域5', 'extension', 'TEXT', 5, 0),
('ext_field_6', '弹性域6', 'extension', 'TEXT', 6, 0),
('ext_field_7', '弹性域7', 'extension', 'TEXT', 7, 0),
('ext_field_8', '弹性域8', 'extension', 'TEXT', 8, 0),
('ext_field_9', '弹性域9', 'extension', 'TEXT', 9, 0),
('ext_field_10', '弹性域10', 'extension', 'TEXT', 10, 0),
('ext_field_11', '弹性域11', 'extension', 'TEXT', 11, 0),
('ext_field_12', '弹性域12', 'extension', 'TEXT', 12, 0),
('ext_field_13', '弹性域13', 'extension', 'TEXT', 13, 0),
('ext_field_14', '弹性域14', 'extension', 'TEXT', 14, 0),
('ext_field_15', '弹性域15', 'extension', 'TEXT', 15, 0),
('ext_field_16', '弹性域16', 'extension', 'TEXT', 16, 0),
('ext_field_17', '弹性域17', 'extension', 'TEXT', 17, 0),
('ext_field_18', '弹性域18', 'extension', 'TEXT', 18, 0),
('ext_field_19', '弹性域19', 'extension', 'TEXT', 19, 0),
('ext_field_20', '弹性域20', 'extension', 'TEXT', 20, 0),
('ext_field_21', '弹性域21', 'extension', 'TEXT', 21, 0),
('ext_field_22', '弹性域22', 'extension', 'TEXT', 22, 0),
('ext_field_23', '弹性域23', 'extension', 'TEXT', 23, 0),
('ext_field_24', '弹性域24', 'extension', 'TEXT', 24, 0),
('ext_field_25', '弹性域25', 'extension', 'TEXT', 25, 0),
('ext_field_26', '弹性域26', 'extension', 'TEXT', 26, 0),
('ext_field_27', '弹性域27', 'extension', 'TEXT', 27, 0),
('ext_field_28', '弹性域28', 'extension', 'TEXT', 28, 0),
('ext_field_29', '弹性域29', 'extension', 'TEXT', 29, 0),
('ext_field_30', '弹性域30', 'extension', 'TEXT', 30, 0);

-- ============================================================
-- 第七步：数据迁移（从备份表到新表）
-- 注意：此迁移假设原EAV数据中的fieldCode与新表字段名一致
-- ============================================================

-- 迁移主表数据（基础字段）
INSERT INTO daily_report (
    id, store_id, report_date, report_month, week_day, template_id,
    status, abnormal_flag, abnormal_message, submit_user_id, submit_time,
    lock_user_id, lock_time, reject_reason, remark, deleted,
    create_time, create_by, update_time, update_by
)
SELECT
    id, store_id, report_date, report_month, week_day, template_id,
    status, abnormal_flag, abnormal_message, submit_user_id, submit_time,
    lock_user_id, lock_time, reject_reason, remark, deleted,
    create_time, create_by, update_time, update_by
FROM daily_report_backup;

-- 从EAV值表迁移房量、间夜、收入字段到主表
-- 房量字段
UPDATE daily_report dr
SET dr.own_room_count = (
    SELECT CAST(value_number AS INT) FROM daily_report_value_backup
    WHERE report_id = dr.id AND field_code = 'own_room_count' AND value_number IS NOT NULL
    LIMIT 1
),
dr.hourly_room_count = (
    SELECT CAST(value_number AS INT) FROM daily_report_value_backup
    WHERE report_id = dr.id AND field_code = 'hourly_room_count' AND value_number IS NOT NULL
    LIMIT 1
),
dr.repair_room_count = (
    SELECT CAST(value_number AS INT) FROM daily_report_value_backup
    WHERE report_id = dr.id AND field_code = 'repair_room_count' AND value_number IS NOT NULL
    LIMIT 1
);

-- 间夜数字段
UPDATE daily_report dr
SET dr.room_nights = (
    SELECT value_number FROM daily_report_value_backup
    WHERE report_id = dr.id AND field_code = 'room_nights' AND value_number IS NOT NULL
    LIMIT 1
),
dr.walkin_room_nights = (
    SELECT value_number FROM daily_report_value_backup
    WHERE report_id = dr.id AND field_code = 'walkin_room_nights' AND value_number IS NOT NULL
    LIMIT 1
),
dr.ctrip_room_nights = (
    SELECT value_number FROM daily_report_value_backup
    WHERE report_id = dr.id AND field_code = 'ctrip_room_nights' AND value_number IS NOT NULL
    LIMIT 1
),
dr.ly_room_nights = (
    SELECT value_number FROM daily_report_value_backup
    WHERE report_id = dr.id AND field_code = 'ly_room_nights' AND value_number IS NOT NULL
    LIMIT 1
),
dr.qunar_room_nights = (
    SELECT value_number FROM daily_report_value_backup
    WHERE report_id = dr.id AND field_code = 'qunar_room_nights' AND value_number IS NOT NULL
    LIMIT 1
),
dr.zhixing_room_nights = (
    SELECT value_number FROM daily_report_value_backup
    WHERE report_id = dr.id AND field_code = 'zhixing_room_nights' AND value_number IS NOT NULL
    LIMIT 1
),
dr.external_room_nights = (
    SELECT value_number FROM daily_report_value_backup
    WHERE report_id = dr.id AND field_code = 'external_room_nights' AND value_number IS NOT NULL
    LIMIT 1
),
dr.meituan_hotel_room_nights = (
    SELECT value_number FROM daily_report_value_backup
    WHERE report_id = dr.id AND field_code = 'meituan_hotel_room_nights' AND value_number IS NOT NULL
    LIMIT 1
),
dr.fliggy_room_nights = (
    SELECT value_number FROM daily_report_value_backup
    WHERE report_id = dr.id AND field_code = 'fliggy_room_nights' AND value_number IS NOT NULL
    LIMIT 1
),
dr.douyin_room_nights = (
    SELECT value_number FROM daily_report_value_backup
    WHERE report_id = dr.id AND field_code = 'douyin_room_nights' AND value_number IS NOT NULL
    LIMIT 1
),
dr.xiaozhu_room_nights = (
    SELECT value_number FROM daily_report_value_backup
    WHERE report_id = dr.id AND field_code = 'xiaozhu_room_nights' AND value_number IS NOT NULL
    LIMIT 1
),
dr.tujia_room_nights = (
    SELECT value_number FROM daily_report_value_backup
    WHERE report_id = dr.id AND field_code = 'tujia_room_nights' AND value_number IS NOT NULL
    LIMIT 1
),
dr.meituan_homestay_room_nights = (
    SELECT value_number FROM daily_report_value_backup
    WHERE report_id = dr.id AND field_code = 'meituan_homestay_room_nights' AND value_number IS NOT NULL
    LIMIT 1
),
dr.jiali_room_nights = (
    SELECT value_number FROM daily_report_value_backup
    WHERE report_id = dr.id AND field_code = 'jiali_room_nights' AND value_number IS NOT NULL
    LIMIT 1
);

-- 经营指标字段
UPDATE daily_report dr
SET dr.occupancy_rate = (
    SELECT value_number FROM daily_report_value_backup
    WHERE report_id = dr.id AND field_code = 'occupancy_rate' AND value_number IS NOT NULL
    LIMIT 1
),
dr.adr = (
    SELECT value_number FROM daily_report_value_backup
    WHERE report_id = dr.id AND field_code = 'adr' AND value_number IS NOT NULL
    LIMIT 1
),
dr.revpar = (
    SELECT value_number FROM daily_report_value_backup
    WHERE report_id = dr.id AND field_code = 'revpar' AND value_number IS NOT NULL
    LIMIT 1
);

-- 收入字段
UPDATE daily_report dr
SET dr.daily_room_fee = (
    SELECT value_number FROM daily_report_value_backup
    WHERE report_id = dr.id AND field_code = 'daily_room_fee' AND value_number IS NOT NULL
    LIMIT 1
),
dr.hourly_room_fee = (
    SELECT value_number FROM daily_report_value_backup
    WHERE report_id = dr.id AND field_code = 'hourly_room_fee' AND value_number IS NOT NULL
    LIMIT 1
),
dr.other_fee = (
    SELECT value_number FROM daily_report_value_backup
    WHERE report_id = dr.id AND field_code = 'other_fee' AND value_number IS NOT NULL
    LIMIT 1
),
dr.total_revenue = (
    SELECT value_number FROM daily_report_value_backup
    WHERE report_id = dr.id AND field_code = 'total_revenue' AND value_number IS NOT NULL
    LIMIT 1
),
dr.deposit_amount = (
    SELECT value_number FROM daily_report_value_backup
    WHERE report_id = dr.id AND field_code = 'deposit_amount' AND value_number IS NOT NULL
    LIMIT 1
);

-- 扫码字段
UPDATE daily_report dr
SET dr.ctrip_scan_count = (
    SELECT CAST(value_number AS INT) FROM daily_report_value_backup
    WHERE report_id = dr.id AND field_code = 'ctrip_scan_count' AND value_number IS NOT NULL
    LIMIT 1
),
dr.meituan_scan_count = (
    SELECT CAST(value_number AS INT) FROM daily_report_value_backup
    WHERE report_id = dr.id AND field_code = 'meituan_scan_count' AND value_number IS NOT NULL
    LIMIT 1
);

-- 从平台表迁移评分数据到渠道表
INSERT INTO daily_channel (daily_report_id, store_id, report_date, template_id,
    ctrip_score, ly_score, qunar_score, zhixing_score, meituan_score, fliggy_score,
    ctrip_good_review_count, meituan_good_review_count, ly_good_review_count,
    qunar_good_review_count, zhixing_good_review_count, fliggy_good_review_count,
    create_time, update_time)
SELECT
    dr.id, dr.store_id, dr.report_date, dr.template_id,
    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
    dr.create_time, dr.update_time
FROM daily_report_backup dr;

-- 更新渠道表中的评分和好评数（从平台表）
UPDATE daily_channel dc
INNER JOIN daily_report_backup dr ON dc.daily_report_id = dr.id
INNER JOIN daily_report_platform_backup dp ON dp.report_id = dr.id
SET
    dc.ctrip_score = COALESCE((SELECT score FROM daily_report_platform_backup WHERE report_id = dr.id AND platform_code = 'ctrip'), 0),
    dc.ly_score = COALESCE((SELECT score FROM daily_report_platform_backup WHERE report_id = dr.id AND platform_code = 'ly'), 0),
    dc.qunar_score = COALESCE((SELECT score FROM daily_report_platform_backup WHERE report_id = dr.id AND platform_code = 'qunar'), 0),
    dc.zhixing_score = COALESCE((SELECT score FROM daily_report_platform_backup WHERE report_id = dr.id AND platform_code = 'zhixing'), 0),
    dc.meituan_score = COALESCE((SELECT score FROM daily_report_platform_backup WHERE report_id = dr.id AND platform_code = 'meituan'), 0),
    dc.fliggy_score = COALESCE((SELECT score FROM daily_report_platform_backup WHERE report_id = dr.id AND platform_code = 'fliggy'), 0),
    dc.ctrip_good_review_count = COALESCE((SELECT good_review_count FROM daily_report_platform_backup WHERE report_id = dr.id AND platform_code = 'ctrip'), 0),
    dc.meituan_good_review_count = COALESCE((SELECT good_review_count FROM daily_report_platform_backup WHERE report_id = dr.id AND platform_code = 'meituan'), 0),
    dc.ly_good_review_count = COALESCE((SELECT good_review_count FROM daily_report_platform_backup WHERE report_id = dr.id AND platform_code = 'ly'), 0),
    dc.qunar_good_review_count = COALESCE((SELECT good_review_count FROM daily_report_platform_backup WHERE report_id = dr.id AND platform_code = 'qunar'), 0),
    dc.zhixing_good_review_count = COALESCE((SELECT good_review_count FROM daily_report_platform_backup WHERE report_id = dr.id AND platform_code = 'zhixing'), 0),
    dc.fliggy_good_review_count = COALESCE((SELECT good_review_count FROM daily_report_platform_backup WHERE report_id = dr.id AND platform_code = 'fliggy'), 0);

-- ============================================================
-- 第八步：从门店表补充门店名称和编码
-- ============================================================

UPDATE daily_report dr
INNER JOIN store s ON dr.store_id = s.id
SET dr.store_name = s.store_name, dr.store_code = s.store_code;

-- ============================================================
-- 迁移完成
-- ============================================================
