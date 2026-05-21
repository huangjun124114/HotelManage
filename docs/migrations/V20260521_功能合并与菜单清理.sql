-- ============================================================================
-- 林夕置业酒店管理系统 - 生产环境数据库变更脚本
-- 功能：门店+团队合并、投资人+投资关系合并、菜单清理、投资比例修正
-- 版本：V1.2
-- 日期：2026-05-21
-- 分支：V260515
-- 关联提交：fcbbae0 → 718c541 → 9d94149 → 62f6bef
-- 数据库：SQLite (linxi.db)
-- ============================================================================

-- ⚠️ 执行前必读：
-- 1. 请先备份生产数据库：cp data/linxi.db data/linxi.db.bak.20260521
-- 2. 本脚本包含 DML 操作（数据增删改），无 DDL 操作（无表结构变更）
-- 3. 请按顺序执行，不要跳步
-- 4. 执行后请验证结果（见文末验证脚本）

-- ============================================================================
-- 变更1：清理已合并菜单的旧菜单记录
-- 背景：门店人员(id=12)和投资关系配置(id=42)的功能已合并到门店列表和投资人列表
-- ============================================================================

-- 1.1 删除旧菜单的角色关联（sys_role_menu）
DELETE FROM sys_role_menu WHERE menu_id IN (12, 42);

-- 1.2 删除旧菜单记录（sys_menu）
-- id=12: 门店人员（功能已合并至门店列表→查看团队弹层）
-- id=42: 投资关系配置（功能已合并至投资人列表→投资记录弹层）
DELETE FROM sys_menu WHERE id IN (12, 42);

-- ============================================================================
-- 变更2：投资者列表菜单挪位
-- 背景：投资者列表(id=41)原属于"投资者管理"一级菜单(id=40)，
--        现挪至"门店管理"一级菜单(id=10)下，并删除"投资者管理"一级菜单
-- ============================================================================

-- 2.1 将投资者列表(id=41)的父级改为门店管理(id=10)，排序号设为2
-- 修改前：parent_id=40, sort_no=1
-- 修改后：parent_id=10, sort_no=2（排在门店列表之后）
UPDATE sys_menu SET parent_id = 10, sort_no = 2 WHERE id = 41;

-- 2.2 删除"投资者管理"一级菜单(id=40)的角色关联
DELETE FROM sys_role_menu WHERE menu_id = 40;

-- 2.3 删除"投资者管理"一级菜单(id=40)
DELETE FROM sys_menu WHERE id = 40;

-- ============================================================================
-- 变更3：投资比例数据修正（小数→百分比）
-- 背景：investor_store.investment_ratio 字段语义为百分比，
--        但历史数据混存了小数形式（如0.25表示25%），需统一为百分比存储
--        修正后：30.00 表示 30%，25.00 表示 25%，33.30 表示 33.3%
-- ============================================================================

-- 3.1 将小数值（< 1 且 > 0）乘以100转为百分比
-- 示例：0.25 → 25.0, 0.30 → 30.0, 0.333 → 33.3
UPDATE investor_store
SET investment_ratio = investment_ratio * 100
WHERE investment_ratio < 1
  AND investment_ratio > 0;

-- ============================================================================
-- 变更4：角色菜单关联更新
-- 背景：投资者列表(id=41)挪到门店管理下后，需要确保原有角色仍能访问
--        如果原角色(id=1 超级管理员)已有 id=41 的权限则无需额外操作
--        以下查询用于确认：
-- ============================================================================

-- 4.1 确认投资者列表(id=41)的角色关联存在
-- 如果以下查询返回0行，则需要手动插入（根据实际角色ID调整）
-- INSERT INTO sys_role_menu (role_id, menu_id) VALUES (1, 41);
-- INSERT INTO sys_role_menu (role_id, menu_id) VALUES (2, 41);
-- 一般情况下，id=41的角色关联已在原始数据中存在，无需额外插入

-- ============================================================================
-- 验证脚本（执行完上述变更后运行，确认结果正确）
-- ============================================================================

-- V1: 验证旧菜单已删除
-- 期望结果：0 行
-- SELECT * FROM sys_menu WHERE id IN (12, 40, 42);

-- V2: 验证投资者列表已挪到门店管理下
-- 期望结果：id=41, parent_id=10, sort_no=2
-- SELECT id, parent_id, menu_name, sort_no FROM sys_menu WHERE id = 41;

-- V3: 验证菜单结构完整性
-- 期望结果：门店管理(10)下有门店列表(11)和投资者列表(41)
-- SELECT id, parent_id, menu_name, sort_no FROM sys_menu WHERE parent_id = 10 ORDER BY sort_no;

-- V4: 验证投资比例无小数值
-- 期望结果：0 行（所有非零投资比例 >= 1）
-- SELECT id, investment_ratio FROM investor_store WHERE investment_ratio > 0 AND investment_ratio < 1;

-- V5: 验证投资比例数据样例
-- 期望结果：所有值 >= 1（如 25.0, 30.0, 33.3 等）
-- SELECT id, investor_id, store_id, investment_ratio FROM investor_store WHERE investment_ratio > 0 ORDER BY id LIMIT 10;

-- V6: 验证无孤立角色菜单关联
-- 期望结果：0 行
-- SELECT rm.* FROM sys_role_menu rm LEFT JOIN sys_menu m ON rm.menu_id = m.id WHERE m.id IS NULL;

-- ============================================================================
-- 回滚脚本（如需回退，请按顺序执行）
-- ============================================================================

-- R1: 恢复"投资者管理"一级菜单
-- INSERT INTO sys_menu (id, parent_id, menu_name, menu_code, menu_type, path, component, sort_no, visible, status)
-- VALUES (40, 0, '投资者管理', 'investor', 1, '/investor', NULL, 5, 1, 1);

-- R2: 恢复投资者列表的父级为投资者管理
-- UPDATE sys_menu SET parent_id = 40, sort_no = 1 WHERE id = 41;

-- R3: 恢复"门店人员"菜单
-- INSERT INTO sys_menu (id, parent_id, menu_name, menu_code, menu_type, path, component, sort_no, visible, status)
-- VALUES (12, 10, '门店人员', 'store:user', 2, '/store/user', 'views/store/StoreUser.vue', 2, 1, 1);

-- R4: 恢复"投资关系配置"菜单
-- INSERT INTO sys_menu (id, parent_id, menu_name, menu_code, menu_type, path, component, sort_no, visible, status)
-- VALUES (42, 40, '投资关系配置', 'investor:relation', 2, '/investor/relation', 'views/investor/InvestorRelation.vue', 2, 1, 1);

-- R5: 回退投资比例数据（百分比→小数）
-- UPDATE investor_store
-- SET investment_ratio = investment_ratio / 100
-- WHERE investment_ratio >= 1
--   AND investment_ratio <= 100;

-- R6: 恢复角色菜单关联（需根据实际角色ID调整）
-- INSERT INTO sys_role_menu (role_id, menu_id) VALUES (1, 12);
-- INSERT INTO sys_role_menu (role_id, menu_id) VALUES (1, 40);
-- INSERT INTO sys_role_menu (role_id, menu_id) VALUES (1, 42);

-- ============================================================================
-- 变更摘要
-- ============================================================================

-- | 序号 | 变更类型 | 影响表      | 影响行数(约) | 说明                          |
-- |------|----------|-------------|-------------|-------------------------------|
-- | 1    | DELETE   | sys_role_menu | 3-6 行    | 删除菜单12/42的角色关联         |
-- | 2    | DELETE   | sys_menu    | 2 行        | 删除门店人员+投资关系配置菜单    |
-- | 3    | UPDATE   | sys_menu    | 1 行        | 投资者列表挪到门店管理下         |
-- | 4    | DELETE   | sys_role_menu | 1-3 行    | 删除菜单40的角色关联             |
-- | 5    | DELETE   | sys_menu    | 1 行        | 删除投资者管理一级菜单           |
-- | 6    | UPDATE   | investor_store | 0-35 行  | 投资比例小数→百分比（视数据而定） |

-- 无 DDL 变更（无新增表、新增列、修改列类型等结构变更）
