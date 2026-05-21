-- ============================================================
-- 林夕置业经营日报系统 - 初始化数据脚本
-- ============================================================

-- 1. 默认角色（密码均为 BCrypt 加密后的 "123456"）
-- admin / 123456  for all test accounts
INSERT OR IGNORE INTO sys_role (id, role_code, role_name, role_type, status) VALUES
(1, 'SUPER_ADMIN', '超级管理员', 1, 1),
(2, 'CEO', 'CEO/总部管理层', 2, 1),
(3, 'REGION_MANAGER', '区域/运营管理', 3, 1),
(4, 'STORE_MANAGER', '门店店长', 4, 1),
(5, 'STORE_STAFF', '门店员工', 5, 1),
(6, 'INVESTOR', '投资者', 6, 1);

-- 2. 系统用户（密码均为 BCrypt 加密 "123456"）
INSERT OR IGNORE INTO sys_user (id, username, real_name, password, phone, user_type, status)
VALUES
(1, 'admin', '系统管理员', '$2b$12$u5sKXcYws70o4ghphg7r4OLqb8WJ5xIzgyZRS3O.M6NpGnyGNCf/K', '13800000001', 1, 1),
(2, 'ceo', '张总', '$2b$12$u5sKXcYws70o4ghphg7r4OLqb8WJ5xIzgyZRS3O.M6NpGnyGNCf/K', '13800000002', 1, 1),
(3, 'store1', '深圳湾店长', '$2b$12$u5sKXcYws70o4ghphg7r4OLqb8WJ5xIzgyZRS3O.M6NpGnyGNCf/K', '13800000003', 2, 1);

-- 3. 用户角色关系
INSERT OR IGNORE INTO sys_user_role (user_id, role_id) VALUES
(1, 1),
(2, 2),
(3, 4);

-- 4. 默认菜单（按需求说明书第十五章菜单结构）
INSERT OR IGNORE INTO sys_menu (id, parent_id, menu_name, menu_code, menu_type, path, component, permission_code, sort_no)
VALUES
-- 首页
(1, 0, '首页', 'home', 1, '/home', 'views/Home.vue', 'home:view', 1),

-- 门店管理
(10, 0, '门店管理', 'store', 1, '/store', NULL, 'store:view', 2),
(11, 10, '门店列表', 'store:list', 2, '/store/list', 'views/store/StoreList.vue', 'store:list', 1),
(41, 10, '投资者列表', 'investor:list', 2, '/investor/list', 'views/investor/InvestorList.vue', 'investor:list', 2),

-- 日报管理
(20, 0, '日报管理', 'report', 1, '/report', NULL, 'report:view', 3),
(21, 20, '日报管理', 'report:manage', 2, '/report/manage', 'views/report/ReportManage.vue', 'report:manage', 1),
(23, 20, '日报审核', 'report:audit', 2, '/report/audit', 'views/report/ReportAudit.vue', 'report:audit', 2),
(24, 20, '未填报统计', 'report:unfilled', 2, '/report/unfilled', 'views/report/UnfilledStats.vue', 'report:unfilled', 3),

-- 报表中心
(30, 0, '报表中心', 'analysis', 1, '/analysis', NULL, 'analysis:view', 4),
(31, 30, '总部看板', 'analysis:dashboard', 2, '/analysis/dashboard', 'views/analysis/Dashboard.vue', 'analysis:dashboard', 1),
(32, 30, '门店日报表', 'analysis:daily', 2, '/analysis/daily', 'views/analysis/DailyReport.vue', 'analysis:daily', 2),
(33, 30, '门店月报表', 'analysis:monthly', 2, '/analysis/monthly', 'views/analysis/MonthlyReport.vue', 'analysis:monthly', 3),
(34, 30, '渠道分析', 'analysis:channel', 2, '/analysis/channel', 'views/analysis/ChannelAnalysis.vue', 'analysis:channel', 4),
(35, 30, '门店排名', 'analysis:ranking', 2, '/analysis/ranking', 'views/analysis/StoreRanking.vue', 'analysis:ranking', 5),
(36, 30, '经营趋势', 'analysis:trend', 2, '/analysis/trend', 'views/analysis/Trend.vue', 'analysis:trend', 6),

-- 投资者管理（已合并到门店管理下，id=41挪到parent_id=10）

-- 系统管理
(50, 0, '系统管理', 'system', 1, '/system', NULL, 'system:view', 6),
(51, 50, '用户管理', 'system:user', 2, '/system/user', 'views/system/UserList.vue', 'system:user', 1),
(52, 50, '角色管理', 'system:role', 2, '/system/role', 'views/system/RoleList.vue', 'system:role', 2),
(53, 50, '菜单权限', 'system:menu', 2, '/system/menu', 'views/system/MenuList.vue', 'system:menu', 3),
(54, 50, '日报模板', 'system:template', 2, '/system/template', 'views/system/TemplateList.vue', 'system:template', 4),
(55, 50, '字段配置', 'system:field', 2, '/system/field', 'views/system/FieldConfig.vue', 'system:field', 5),
(56, 50, '系统参数', 'system:config', 2, '/system/config', 'views/system/ConfigList.vue', 'system:config', 6),
(57, 50, '操作日志', 'system:log', 2, '/system/log', 'views/system/OperationLog.vue', 'system:log', 7);

-- 5. 给超级管理员分配所有菜单权限
INSERT OR IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT 1, id FROM sys_menu;

-- 给CEO分配核心菜单（去掉系统管理部分敏感菜单）
INSERT OR IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT 2, id FROM sys_menu WHERE id IN (1,10,11,20,21,23,24,30,31,32,33,34,35,36,41);

-- 给店长分配门店相关菜单
INSERT OR IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT 4, id FROM sys_menu WHERE id IN (1,20,21,30,32,33,34,35,36);

-- 给投资者分配只读菜单（首页、门店列表、日报管理、报表中心、投资者管理）
INSERT OR IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT 6, id FROM sys_menu WHERE id IN (1,10,11,20,21,30,31,32,33,34,35,36,41);

-- 6. 示例门店（9家，均在深圳市，门店编码按拼音首字母定义）
INSERT OR IGNORE INTO store (id, store_code, store_name, short_name, city, region_name, address, own_room_count, status)
VALUES
(1, 'SZ001', '深圳湾店', '深湾店', '深圳', '华南区', '深圳市南山区深圳湾', 39, 1),
(2, 'SZ002', '翠湖店', '翠湖店', '深圳', '华南区', '深圳市罗湖区翠湖', 28, 1),
(3, 'SZ003', '诗雅图(联合广场)', '诗雅图', '深圳', '华南区', '深圳市福田区联合广场', 35, 1),
(4, 'SZ004', '联合广场(珀晞)', '联合广场', '深圳', '华南区', '深圳市福田区联合广场', 42, 1),
(5, 'SZ005', '南山店', '南山店', '深圳', '华南区', '深圳市南山区', 30, 1),
(6, 'SZ006', '岗厦店', '岗厦店', '深圳', '华南区', '深圳市福田区岗厦', 25, 1),
(7, 'SZ007', '南头古城店', '南头古城', '深圳', '华南区', '深圳市南山区南头古城', 20, 1),
(8, 'SZ008', '东湖店', '东湖店', '深圳', '华南区', '深圳市罗湖区东湖', 22, 1),
(9, 'SZ009', '岗厦北店', '岗厦北', '深圳', '华南区', '深圳市福田区岗厦北', 18, 1);

-- 7. 用户门店绑定（店长绑定到门店1）
INSERT OR IGNORE INTO sys_user_store (user_id, store_id, permission_type)
VALUES (3, 1, 2);

-- 8. 默认日报模板
INSERT OR IGNORE INTO daily_report_template (id, template_code, template_name, apply_type, effective_date, status, is_default)
VALUES (1, 'DEFAULT_V1', '默认日报模板 V1', 1, '2025-01-01', 1, 1);

-- 9. 默认日报字段配置（39个固定字段，按需求说明书第十八章）
INSERT OR IGNORE INTO daily_report_field (template_id, field_code, field_name, field_category, field_type, required, visible, readonly_flag, decimal_scale, summary_flag, summary_type, sort_no, formula)
VALUES
-- 基础字段
(1, 'own_room_count', '自有房量', '基础字段', 'number', 1, 1, 0, 0, 1, 'LAST', 1, NULL),
(1, 'hourly_room_count', '钟点房数量', '基础字段', 'number', 0, 1, 0, 0, 1, 'SUM', 2, NULL),
(1, 'repair_room_count', '维修房', '基础字段', 'number', 0, 1, 0, 0, 1, 'SUM', 3, NULL),

-- 房量与间夜字段
(1, 'room_nights', '间夜数', '房量与间夜', 'number', 0, 1, 1, 1, 1, 'SUM', 10, NULL),
(1, 'walkin_room_nights', '散客', '房量与间夜', 'number', 0, 1, 0, 1, 1, 'SUM', 11, NULL),
(1, 'ctrip_room_nights', '携程', '房量与间夜', 'number', 0, 1, 0, 1, 1, 'SUM', 12, NULL),
(1, 'ly_room_nights', '同程艺龙', '房量与间夜', 'number', 0, 1, 0, 1, 1, 'SUM', 13, NULL),
(1, 'qunar_room_nights', '去哪儿', '房量与间夜', 'number', 0, 1, 0, 1, 1, 'SUM', 14, NULL),
(1, 'zhixing_room_nights', '智行', '房量与间夜', 'number', 0, 1, 0, 1, 1, 'SUM', 15, NULL),
(1, 'external_room_nights', '外网', '房量与间夜', 'number', 0, 1, 0, 1, 1, 'SUM', 16, NULL),
(1, 'meituan_hotel_room_nights', '美团酒店', '房量与间夜', 'number', 0, 1, 0, 1, 1, 'SUM', 17, NULL),
(1, 'fliggy_room_nights', '飞猪', '房量与间夜', 'number', 0, 1, 0, 1, 1, 'SUM', 18, NULL),
(1, 'douyin_room_nights', '抖音', '房量与间夜', 'number', 0, 1, 0, 1, 1, 'SUM', 19, NULL),
(1, 'xiaozhu_room_nights', '小猪', '房量与间夜', 'number', 0, 1, 0, 1, 1, 'SUM', 20, NULL),
(1, 'tujia_room_nights', '途家', '房量与间夜', 'number', 0, 1, 0, 1, 1, 'SUM', 21, NULL),
(1, 'meituan_homestay_room_nights', '美团民宿', '房量与间夜', 'number', 0, 1, 0, 1, 1, 'SUM', 22, NULL),
(1, 'jiali_room_nights', '红色加力/加力', '房量与间夜', 'number', 0, 1, 0, 1, 1, 'SUM', 23, NULL),

-- 经营指标字段（公式计算）
(1, 'occupancy_rate', '出租率', '经营指标', 'number', 0, 1, 1, 4, 1, 'AVG', 30, 'room_nights / own_room_count'),
(1, 'adr', 'ADR', '经营指标', 'number', 0, 1, 1, 2, 1, 'AVG', 31, 'daily_room_fee / room_nights'),
(1, 'revpar', 'RevPAR', '经营指标', 'number', 0, 1, 1, 2, 1, 'AVG', 32, 'total_revenue / own_room_count'),

-- 收入字段
(1, 'daily_room_fee', '日租房房费', '收入', 'number', 0, 1, 0, 2, 1, 'SUM', 40, NULL),
(1, 'hourly_room_fee', '钟点房费用', '收入', 'number', 0, 1, 0, 2, 1, 'SUM', 41, NULL),
(1, 'other_fee', '杂费', '收入', 'number', 0, 1, 0, 2, 1, 'SUM', 42, NULL),
(1, 'total_revenue', '当日总营收', '收入', '0', 1, 1, 1, 2, 1, 'SUM', 43, 'daily_room_fee + hourly_room_fee + other_fee'),
(1, 'deposit_amount', '押金', '收入', 'number', 0, 1, 0, 2, 1, 'SUM', 44, NULL),

-- 扫码字段
(1, 'ctrip_scan_count', '携程扫码', '扫码', 'number', 0, 1, 0, 0, 1, 'SUM', 50, NULL),
(1, 'meituan_scan_count', '美团扫码', '扫码', 'number', 0, 1, 0, 0, 1, 'SUM', 51, NULL),

-- 好评数字段
(1, 'ctrip_good_review_count', '携程好评数', '好评数', 'number', 0, 1, 0, 0, 1, 'SUM', 60, NULL),
(1, 'meituan_good_review_count', '美团好评数', '好评数', 'number', 0, 1, 0, 0, 1, 'SUM', 61, NULL),
(1, 'ly_good_review_count', '同程好评数', '好评数', 'number', 0, 1, 0, 0, 1, 'SUM', 62, NULL),
(1, 'qunar_good_review_count', '去哪好评数', '好评数', 'number', 0, 1, 0, 0, 1, 'SUM', 63, NULL),
(1, 'zhixing_good_review_count', '智行好评数', '好评数', 'number', 0, 1, 0, 0, 1, 'SUM', 64, NULL),
(1, 'fliggy_good_review_count', '飞猪好评数', '好评数', 'number', 0, 1, 0, 0, 1, 'SUM', 65, NULL),

-- 评分字段
(1, 'ctrip_score', '携程评分', '评分', 'number', 0, 1, 0, 2, 1, 'LAST', 70, NULL),
(1, 'ly_score', '同程艺龙评分', '评分', 'number', 0, 1, 0, 2, 1, 'LAST', 71, NULL),
(1, 'qunar_score', '去哪评分', '评分', 'number', 0, 1, 0, 2, 1, 'LAST', 72, NULL),
(1, 'zhixing_score', '智行评分', '评分', 'number', 0, 1, 0, 2, 1, 'LAST', 73, NULL),
(1, 'meituan_score', '美团评分', '评分', 'number', 0, 1, 0, 2, 1, 'LAST', 74, NULL),
(1, 'fliggy_score', '飞猪评分', '评分', 'number', 0, 1, 0, 2, 1, 'LAST', 75, NULL);
