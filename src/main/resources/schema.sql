-- ============================================================
-- 林夕置业经营日报系统 - 数据库建表脚本（SQLite 适配版）
-- 基于需求说明书第九章 DDL，适配 SQLite 语法
-- ============================================================

-- 1. 系统配置表
CREATE TABLE IF NOT EXISTS sys_config (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    config_key VARCHAR(100) NOT NULL,
    config_value TEXT,
    config_name VARCHAR(200),
    config_type VARCHAR(50) DEFAULT 'STRING',
    remark VARCHAR(500),
    deleted INTEGER DEFAULT 0,
    create_time TEXT DEFAULT (datetime('now','localtime')),
    create_by INTEGER,
    update_time TEXT DEFAULT (datetime('now','localtime')),
    update_by INTEGER
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_config_key ON sys_config(config_key);

-- 2. 酒店门店表
CREATE TABLE IF NOT EXISTS store (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    store_code VARCHAR(50) NOT NULL,
    store_name VARCHAR(200) NOT NULL,
    short_name VARCHAR(100),
    brand VARCHAR(100),
    region_name VARCHAR(100),
    city VARCHAR(100),
    address VARCHAR(255),
    own_room_count INTEGER DEFAULT 0,
    manager_user_id INTEGER,
    contact_phone VARCHAR(30),
    open_date TEXT,
    status INTEGER DEFAULT 1,
    remark VARCHAR(500),
    deleted INTEGER DEFAULT 0,
    create_time TEXT DEFAULT (datetime('now','localtime')),
    create_by INTEGER,
    update_time TEXT DEFAULT (datetime('now','localtime')),
    update_by INTEGER
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_store_code ON store(store_code);

-- 3. 系统用户表
CREATE TABLE IF NOT EXISTS sys_user (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    username VARCHAR(50) NOT NULL,
    real_name VARCHAR(100) NOT NULL,
    password VARCHAR(255) NOT NULL,
    phone VARCHAR(30),
    email VARCHAR(100),
    user_type INTEGER NOT NULL,
    status INTEGER DEFAULT 1,
    last_login_time TEXT,
    last_login_ip VARCHAR(50),
    password_reset_required INTEGER DEFAULT 0,
    remark VARCHAR(500),
    deleted INTEGER DEFAULT 0,
    create_time TEXT DEFAULT (datetime('now','localtime')),
    create_by INTEGER,
    update_time TEXT DEFAULT (datetime('now','localtime')),
    update_by INTEGER
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_username ON sys_user(username);
CREATE UNIQUE INDEX IF NOT EXISTS uk_phone ON sys_user(phone) WHERE phone IS NOT NULL;

-- 4. 系统角色表
CREATE TABLE IF NOT EXISTS sys_role (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    role_code VARCHAR(50) NOT NULL,
    role_name VARCHAR(100) NOT NULL,
    role_type INTEGER DEFAULT 1,
    status INTEGER DEFAULT 1,
    remark VARCHAR(500),
    deleted INTEGER DEFAULT 0,
    create_time TEXT DEFAULT (datetime('now','localtime')),
    update_time TEXT DEFAULT (datetime('now','localtime'))
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_role_code ON sys_role(role_code);

-- 5. 用户角色关系表
CREATE TABLE IF NOT EXISTS sys_user_role (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id INTEGER NOT NULL,
    role_id INTEGER NOT NULL,
    create_time TEXT DEFAULT (datetime('now','localtime'))
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_user_role ON sys_user_role(user_id, role_id);

-- 6. 菜单权限表
CREATE TABLE IF NOT EXISTS sys_menu (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    parent_id INTEGER DEFAULT 0,
    menu_name VARCHAR(100) NOT NULL,
    menu_code VARCHAR(100),
    menu_type INTEGER NOT NULL,
    path VARCHAR(255),
    component VARCHAR(255),
    permission_code VARCHAR(100),
    sort_no INTEGER DEFAULT 0,
    visible INTEGER DEFAULT 1,
    status INTEGER DEFAULT 1,
    create_time TEXT DEFAULT (datetime('now','localtime')),
    update_time TEXT DEFAULT (datetime('now','localtime'))
);

-- 7. 角色菜单权限表
CREATE TABLE IF NOT EXISTS sys_role_menu (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    role_id INTEGER NOT NULL,
    menu_id INTEGER NOT NULL,
    create_time TEXT DEFAULT (datetime('now','localtime'))
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_role_menu ON sys_role_menu(role_id, menu_id);

-- 8. 用户门店权限表
CREATE TABLE IF NOT EXISTS sys_user_store (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id INTEGER NOT NULL,
    store_id INTEGER NOT NULL,
    permission_type INTEGER DEFAULT 1,
    create_time TEXT DEFAULT (datetime('now','localtime'))
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_user_store ON sys_user_store(user_id, store_id, permission_type);

-- 9. 投资者表
CREATE TABLE IF NOT EXISTS investor (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    investor_name VARCHAR(100) NOT NULL,
    phone VARCHAR(30) NOT NULL,
    email VARCHAR(100),
    user_id INTEGER NOT NULL,
    status INTEGER DEFAULT 1,
    remark VARCHAR(500),
    deleted INTEGER DEFAULT 0,
    create_time TEXT DEFAULT (datetime('now','localtime')),
    create_by INTEGER,
    update_time TEXT DEFAULT (datetime('now','localtime')),
    update_by INTEGER
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_investor_phone ON investor(phone) WHERE phone IS NOT NULL;

-- 10. 投资者门店授权表
CREATE TABLE IF NOT EXISTS investor_store (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    investor_id INTEGER NOT NULL,
    store_id INTEGER NOT NULL,
    investment_ratio REAL,
    auth_start_date TEXT,
    auth_end_date TEXT,
    can_view_revenue INTEGER DEFAULT 1,
    can_view_channel INTEGER DEFAULT 1,
    can_view_score INTEGER DEFAULT 1,
    can_export INTEGER DEFAULT 0,
    status INTEGER DEFAULT 1,
    create_time TEXT DEFAULT (datetime('now','localtime')),
    update_time TEXT DEFAULT (datetime('now','localtime'))
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_investor_store ON investor_store(investor_id, store_id);

-- 11. 日报模板表
CREATE TABLE IF NOT EXISTS daily_report_template (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    template_code VARCHAR(50) NOT NULL,
    template_name VARCHAR(100) NOT NULL,
    apply_type INTEGER DEFAULT 1,
    effective_date TEXT NOT NULL,
    status INTEGER DEFAULT 0,
    is_default INTEGER DEFAULT 0,
    remark VARCHAR(500),
    deleted INTEGER DEFAULT 0,
    create_time TEXT DEFAULT (datetime('now','localtime')),
    create_by INTEGER,
    update_time TEXT DEFAULT (datetime('now','localtime')),
    update_by INTEGER
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_template_code ON daily_report_template(template_code);

-- 12. 模板适用门店表
CREATE TABLE IF NOT EXISTS daily_report_template_store (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    template_id INTEGER NOT NULL,
    store_id INTEGER NOT NULL,
    create_time TEXT DEFAULT (datetime('now','localtime'))
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_template_store ON daily_report_template_store(template_id, store_id);

-- 13. 日报字段配置表（核心：动态字段定义）
CREATE TABLE IF NOT EXISTS daily_report_field (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    template_id INTEGER NOT NULL,
    field_code VARCHAR(100) NOT NULL,
    field_name VARCHAR(100) NOT NULL,
    field_category VARCHAR(50),
    field_type VARCHAR(50) NOT NULL,
    required INTEGER DEFAULT 0,
    visible INTEGER DEFAULT 1,
    readonly_flag INTEGER DEFAULT 0,
    default_value VARCHAR(255),
    option_json TEXT,
    formula TEXT,
    decimal_scale INTEGER DEFAULT 2,
    min_value REAL,
    max_value REAL,
    summary_flag INTEGER DEFAULT 1,
    summary_type VARCHAR(50) DEFAULT 'SUM',
    investor_visible INTEGER DEFAULT 1,
    store_visible INTEGER DEFAULT 1,
    sort_no INTEGER DEFAULT 0,
    status INTEGER DEFAULT 1,
    remark VARCHAR(500),
    create_time TEXT DEFAULT (datetime('now','localtime')),
    update_time TEXT DEFAULT (datetime('now','localtime'))
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_template_field ON daily_report_field(template_id, field_code);

-- 14. 日报主表
CREATE TABLE IF NOT EXISTS daily_report (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    store_id INTEGER NOT NULL,
    report_date TEXT NOT NULL,
    report_month VARCHAR(7) NOT NULL,
    week_day VARCHAR(10),
    template_id INTEGER NOT NULL,
    status INTEGER DEFAULT 0,
    abnormal_flag INTEGER DEFAULT 0,
    abnormal_message VARCHAR(1000),
    submit_user_id INTEGER,
    submit_time TEXT,
    lock_user_id INTEGER,
    lock_time TEXT,
    reject_reason VARCHAR(1000),
    remark VARCHAR(1000),
    deleted INTEGER DEFAULT 0,
    create_time TEXT DEFAULT (datetime('now','localtime')),
    create_by INTEGER,
    update_time TEXT DEFAULT (datetime('now','localtime')),
    update_by INTEGER
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_store_date ON daily_report(store_id, report_date);
CREATE INDEX IF NOT EXISTS idx_report_month ON daily_report(report_month);
CREATE INDEX IF NOT EXISTS idx_report_status ON daily_report(status);

-- 15. 日报字段值表（EAV弹性域：动态字段值存储）
CREATE TABLE IF NOT EXISTS daily_report_value (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    report_id INTEGER NOT NULL,
    field_id INTEGER NOT NULL,
    field_code VARCHAR(100) NOT NULL,
    field_name VARCHAR(100) NOT NULL,
    field_type VARCHAR(50) NOT NULL,
    value_text TEXT,
    value_number REAL,
    value_date TEXT,
    value_json TEXT,
    sort_no INTEGER DEFAULT 0,
    create_time TEXT DEFAULT (datetime('now','localtime')),
    update_time TEXT DEFAULT (datetime('now','localtime'))
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_report_field ON daily_report_value(report_id, field_code);

-- 16. 日报核心指标汇总表（列式冗余：提升Dashboard查询性能）
CREATE TABLE IF NOT EXISTS daily_report_summary (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    report_id INTEGER NOT NULL,
    store_id INTEGER NOT NULL,
    report_date TEXT NOT NULL,
    report_month VARCHAR(7) NOT NULL,
    own_room_count REAL DEFAULT 0,
    repair_room_count REAL DEFAULT 0,
    hourly_room_count REAL DEFAULT 0,
    room_nights REAL DEFAULT 0,
    occupancy_rate REAL DEFAULT 0,
    adr REAL DEFAULT 0,
    revpar REAL DEFAULT 0,
    daily_room_fee REAL DEFAULT 0,
    hourly_room_fee REAL DEFAULT 0,
    other_fee REAL DEFAULT 0,
    total_revenue REAL DEFAULT 0,
    deposit_amount REAL DEFAULT 0,
    create_time TEXT DEFAULT (datetime('now','localtime')),
    update_time TEXT DEFAULT (datetime('now','localtime'))
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_report_summary ON daily_report_summary(report_id);
CREATE INDEX IF NOT EXISTS idx_summary_store_date ON daily_report_summary(store_id, report_date);
CREATE INDEX IF NOT EXISTS idx_summary_month ON daily_report_summary(report_month);

-- 17. 日报渠道数据表（行式存储：每条渠道一行）
CREATE TABLE IF NOT EXISTS daily_report_channel (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    report_id INTEGER NOT NULL,
    store_id INTEGER NOT NULL,
    report_date TEXT NOT NULL,
    channel_code VARCHAR(100) NOT NULL,
    channel_name VARCHAR(100) NOT NULL,
    room_nights REAL DEFAULT 0,
    create_time TEXT DEFAULT (datetime('now','localtime')),
    update_time TEXT DEFAULT (datetime('now','localtime'))
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_report_channel ON daily_report_channel(report_id, channel_code);

-- 18. 日报平台评价数据表（行式存储：每个平台一行）
CREATE TABLE IF NOT EXISTS daily_report_platform (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    report_id INTEGER NOT NULL,
    store_id INTEGER NOT NULL,
    report_date TEXT NOT NULL,
    platform_code VARCHAR(100) NOT NULL,
    platform_name VARCHAR(100) NOT NULL,
    scan_count INTEGER DEFAULT 0,
    good_review_count INTEGER DEFAULT 0,
    score REAL,
    create_time TEXT DEFAULT (datetime('now','localtime')),
    update_time TEXT DEFAULT (datetime('now','localtime'))
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_report_platform ON daily_report_platform(report_id, platform_code);

-- 19. 消息提醒记录表
CREATE TABLE IF NOT EXISTS message_notify_log (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    notify_type VARCHAR(50) NOT NULL,
    channel VARCHAR(50) NOT NULL,
    receiver_user_id INTEGER,
    receiver_phone VARCHAR(30),
    receiver_name VARCHAR(100),
    store_id INTEGER,
    title VARCHAR(200),
    content TEXT,
    send_status INTEGER DEFAULT 0,
    fail_reason VARCHAR(1000),
    send_time TEXT,
    create_time TEXT DEFAULT (datetime('now','localtime'))
);

-- 20. 系统操作日志表
CREATE TABLE IF NOT EXISTS sys_operation_log (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id INTEGER,
    username VARCHAR(100),
    real_name VARCHAR(100),
    module_name VARCHAR(100),
    operation_type VARCHAR(50),
    business_id VARCHAR(100),
    request_url VARCHAR(255),
    request_method VARCHAR(20),
    request_param TEXT,
    before_data TEXT,
    after_data TEXT,
    ip_address VARCHAR(50),
    result_status INTEGER DEFAULT 1,
    error_message TEXT,
    create_time TEXT DEFAULT (datetime('now','localtime'))
);
