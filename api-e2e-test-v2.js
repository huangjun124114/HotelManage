/**
 * 林夕置业日报系统 - API端到端测试 (修正版)
 * 更正所有API路径
 */

const http = require('http');

const API_URL = 'http://localhost:8082';

const results = {
  passed: [],
  failed: [],
  total: 0
};

function log(msg, type = 'info') {
  const timestamp = new Date().toISOString().split('T')[1].slice(0, 8);
  if (type === 'pass') console.log(`  ✅ [${timestamp}] ${msg}`);
  else if (type === 'fail') console.log(`  ❌ [${timestamp}] ${msg}`);
  else console.log(`  ℹ️  [${timestamp}] ${msg}`);
}

function api(endpoint, method = 'GET', body = null, token = null) {
  return new Promise((resolve, reject) => {
    const url = new URL(endpoint, API_URL);
    const options = {
      hostname: url.hostname,
      port: url.port,
      path: url.pathname + url.search,
      method: method,
      headers: {
        'Content-Type': 'application/json',
        ...(token && { 'Authorization': `Bearer ${token}` })
      }
    };

    const req = http.request(options, (res) => {
      let data = '';
      res.on('data', chunk => data += chunk);
      res.on('end', () => {
        try {
          resolve({ status: res.statusCode, data: JSON.parse(data) });
        } catch {
          resolve({ status: res.statusCode, data: data });
        }
      });
    });

    req.on('error', reject);
    if (body) req.write(JSON.stringify(body));
    req.end();
  });
}

async function runTest(name, testFn) {
  results.total++;
  try {
    const result = await testFn();
    results.passed.push(name);
    log(`${name}`, 'pass');
    return result;
  } catch (err) {
    results.failed.push({ name, error: err.message });
    log(`${name}: ${err.message}`, 'fail');
    return null;
  }
}

async function main() {
  console.log('\n========================================');
  console.log('林夕置业日报系统 - API端到端测试(修正版)');
  console.log('========================================\n');

  // Step 1: 登录获取token
  log('步骤1: 登录认证');
  let token;
  try {
    const loginResp = await api('/api/auth/login', 'POST', { username: 'admin', password: 'admin123' });
    if (loginResp.status === 200 && loginResp.data?.data?.token) {
      token = loginResp.data.data.token;
      log(`登录成功, Token: ${token.slice(0, 30)}...`);
    } else {
      throw new Error(`登录失败: ${JSON.stringify(loginResp)}`);
    }
  } catch (err) {
    log(`登录失败: ${err.message}`, 'fail');
    return;
  }

  // ============ 用户管理模块 ============
  log('\n步骤2: 用户管理模块测试');

  await runTest('用户列表查询', async () => {
    const resp = await api('/api/users?page=1&size=10', 'GET', null, token);
    if (resp.status !== 200 || resp.data.code !== 200) throw new Error(`失败: ${JSON.stringify(resp.data)}`);
    log(`  用户数量: ${resp.data.data?.total || resp.data.data?.records?.length || 0}`);
  });

  await runTest('用户详情查询', async () => {
    const resp = await api('/api/users/1', 'GET', null, token);
    if (resp.status !== 200 || resp.data.code !== 200) throw new Error(`失败: ${JSON.stringify(resp.data)}`);
    log(`  用户名: ${resp.data.data?.username}`);
  });

  await runTest('用户创建', async () => {
    const newUser = {
      username: 'test_e2e_' + Date.now(),
      password: 'test123',
      realName: 'E2E测试用户',
      phone: '138' + Date.now(),  // 使用时间戳确保唯一
      status: 1
    };
    const resp = await api('/api/users', 'POST', newUser, token);
    if (resp.status !== 200 || resp.data.code !== 200) throw new Error(`失败: ${JSON.stringify(resp.data)}`);
    log(`  创建用户成功`);
  });

  // ============ 门店管理模块 ============
  log('\n步骤3: 门店管理模块测试');

  await runTest('门店列表查询', async () => {
    const resp = await api('/api/stores?page=1&size=10', 'GET', null, token);
    if (resp.status !== 200 || resp.data.code !== 200) throw new Error(`失败: ${JSON.stringify(resp.data)}`);
    log(`  门店数量: ${resp.data.data?.total || resp.data.data?.records?.length || 0}`);
  });

  await runTest('门店详情查询', async () => {
    const resp = await api('/api/stores/1', 'GET', null, token);
    if (resp.status !== 200 || resp.data.code !== 200) throw new Error(`失败: ${JSON.stringify(resp.data)}`);
    log(`  门店名: ${resp.data.data?.storeName}`);
  });

  // ============ 角色管理模块 ============
  log('\n步骤4: 角色管理模块测试');

  await runTest('角色列表查询', async () => {
    const resp = await api('/api/roles?page=1&size=10', 'GET', null, token);
    if (resp.status !== 200 || resp.data.code !== 200) throw new Error(`失败: ${JSON.stringify(resp.data)}`);
    log(`  角色数量: ${resp.data.data?.total || resp.data.data?.records?.length || 0}`);
  });

  await runTest('角色详情查询', async () => {
    const resp = await api('/api/roles/1', 'GET', null, token);
    if (resp.status !== 200 || resp.data.code !== 200) throw new Error(`失败: ${JSON.stringify(resp.data)}`);
    log(`  角色名: ${resp.data.data?.roleName}`);
  });

  // ============ 日报管理模块 ============
  log('\n步骤5: 日报管理模块测试');

  await runTest('日报列表查询(query)', async () => {
    const resp = await api('/api/daily-reports/query?page=1&size=10', 'GET', null, token);
    if (resp.status !== 200 || resp.data.code !== 200) throw new Error(`失败: ${JSON.stringify(resp.data)}`);
    log(`  日报数量: ${resp.data.data?.total || 0}`);
  });

  await runTest('日报详情查询', async () => {
    const resp = await api('/api/daily-reports/query?page=1&size=1', 'GET', null, token);
    if (resp.status !== 200 || resp.data.code !== 200) throw new Error(`失败: ${JSON.stringify(resp.data)}`);
    if (resp.data.data?.records?.length > 0) {
      log(`  日报日期: ${resp.data.data.records[0].reportDate}`);
    }
  });

  await runTest('日报草稿创建', async () => {
    const today = new Date().toISOString().split('T')[0];
    const newReport = {
      storeId: 1,
      reportDate: today,
      occupancyRate: 85.5,
      adr: 350.00,
      revpar: 297.50
    };
    const resp = await api('/api/daily-reports/draft', 'POST', newReport, token);
    if (resp.status !== 200 || resp.data.code !== 200) throw new Error(`失败: ${JSON.stringify(resp.data)}`);
    log(`  创建日报成功`);
  });

  await runTest('日报未填报表', async () => {
    const resp = await api('/api/daily-reports/unfilled?month=2026-05', 'GET', null, token);
    if (resp.status !== 200 || resp.data.code !== 200) throw new Error(`失败: ${JSON.stringify(resp.data)}`);
    log(`  未填报表获取成功`);
  });

  // ============ 投资人管理模块 ============
  log('\n步骤6: 投资人管理模块测试');

  await runTest('投资人列表查询', async () => {
    const resp = await api('/api/investors?page=1&size=10', 'GET', null, token);
    if (resp.status !== 200 || resp.data.code !== 200) throw new Error(`失败: ${JSON.stringify(resp.data)}`);
    log(`  投资人数量: ${resp.data.data?.total || resp.data.data?.records?.length || 0}`);
  });

  await runTest('投资人创建', async () => {
    const newInvestor = {
      investorName: 'E2E测试投资人_' + Date.now(),
      phone: '139' + Date.now(),  // 使用时间戳确保唯一
      status: 1
    };
    const resp = await api('/api/investors', 'POST', newInvestor, token);
    if (resp.status !== 200 || resp.data.code !== 200) throw new Error(`失败: ${JSON.stringify(resp.data)}`);
    log(`  创建投资人成功`);
  });

  await runTest('投资人更新', async () => {
    const resp = await api('/api/investors/1', 'PUT', { investorName: '测试更新' }, token);
    if (resp.status !== 200 || resp.data.code !== 200) throw new Error(`失败: ${JSON.stringify(resp.data)}`);
    log(`  更新成功`);
  });

  // ============ 菜单管理模块 ============
  log('\n步骤7: 菜单管理模块测试');

  await runTest('菜单树查询', async () => {
    const resp = await api('/api/menus/tree', 'GET', null, token);
    if (resp.status !== 200 || resp.data.code !== 200) throw new Error(`失败: ${JSON.stringify(resp.data)}`);
    log(`  菜单数量: ${resp.data.data?.length || 0}`);
  });

  // ============ 报表模块 ============
  log('\n步骤8: 报表模块测试');

  await runTest('仪表盘数据', async () => {
    const resp = await api('/api/reports/dashboard', 'GET', null, token);
    if (resp.status !== 200 || resp.data.code !== 200) throw new Error(`失败: ${JSON.stringify(resp.data)}`);
    log(`  仪表盘: ${resp.data.data?.totalStores || 0} 门店`);
  });

  await runTest('月度报表', async () => {
    const resp = await api('/api/reports/monthly?month=2026-05', 'GET', null, token);
    if (resp.status !== 200 || resp.data.code !== 200) throw new Error(`失败: ${JSON.stringify(resp.data)}`);
    log(`  月度报表获取成功`);
  });

  await runTest('趋势报表', async () => {
    const resp = await api('/api/reports/trend?startDate=2026-05-01&endDate=2026-05-17', 'GET', null, token);
    if (resp.status !== 200 || resp.data.code !== 200) throw new Error(`失败: ${JSON.stringify(resp.data)}`);
    log(`  趋势报表获取成功`);
  });

  await runTest('渠道分析报表', async () => {
    const resp = await api('/api/reports/channel-analysis?startDate=2026-05-01&endDate=2026-05-17', 'GET', null, token);
    if (resp.status !== 200 || resp.data.code !== 200) throw new Error(`失败: ${JSON.stringify(resp.data)}`);
    log(`  渠道分析获取成功`);
  });

  await runTest('门店排名报表', async () => {
    const resp = await api('/api/reports/store-ranking?date=2026-05-17', 'GET', null, token);
    if (resp.status !== 200 || resp.data.code !== 200) throw new Error(`失败: ${JSON.stringify(resp.data)}`);
    log(`  门店排名获取成功`);
  });

  // ============ 字段配置模块 ============
  log('\n步骤9: 字段配置模块测试');

  await runTest('字段定义列表(report)', async () => {
    const resp = await api('/api/extension-field-definitions?page=1&size=50', 'GET', null, token);
    if (resp.status !== 200 || resp.data.code !== 200) throw new Error(`失败: ${JSON.stringify(resp.data)}`);
    log(`  字段定义数量: ${resp.data.data?.total || 0}`);
  });

  await runTest('字段定义列表(by-table-type)', async () => {
    const resp = await api('/api/extension-field-definitions/by-table-type/report', 'GET', null, token);
    if (resp.status !== 200 || resp.data.code !== 200) throw new Error(`失败: ${JSON.stringify(resp.data)}`);
    log(`  报表字段数量: ${resp.data.data?.length || 0}`);
  });

  await runTest('字段定义创建', async () => {
    const newField = {
      fieldCode: 'test_field_' + Date.now(),
      fieldName: '测试字段',
      fieldType: 'TEXT',
      tableType: 'report',
      sortOrder: 999
    };
    const resp = await api('/api/extension-field-definitions', 'POST', newField, token);
    if (resp.status !== 200 || resp.data.code !== 200) throw new Error(`失败: ${JSON.stringify(resp.data)}`);
    log(`  创建字段成功`);
  });

  // ============ 汇总 ============
  log('\n步骤10: 汇总报告');

  await runTest('投资人门店关联', async () => {
    const resp = await api('/api/investors/1/stores', 'GET', null, token);
    if (resp.status !== 200 || resp.data.code !== 200) throw new Error(`失败: ${JSON.stringify(resp.data)}`);
    log(`  关联门店数量: ${resp.data.data?.length || 0}`);
  });

  // 输出测试结果
  console.log('\n========================================');
  console.log('测试结果汇总');
  console.log('========================================');
  console.log(`总计: ${results.total} 个测试`);
  console.log(`通过: ${results.passed.length} 个`);
  console.log(`失败: ${results.failed.length} 个`);
  console.log(`通过率: ${((results.passed.length / results.total) * 100).toFixed(1)}%`);

  if (results.failed.length > 0) {
    console.log('\n失败详情:');
    results.failed.forEach(f => {
      console.log(`  ❌ ${f.name}: ${f.error}`);
    });
  }

  console.log('\n========================================\n');

  process.exit(results.failed.length > 0 ? 1 : 0);
}

main().catch(err => {
  console.error('测试执行失败:', err);
  process.exit(1);
});
