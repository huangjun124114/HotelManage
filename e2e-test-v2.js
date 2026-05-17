/**
 * 林夕置业日报系统 - UI端到端测试 v2
 * HotelManage E2E Tests with Playwright
 * 适配实际前端路由和组件
 */

const { chromium } = require('playwright');

const BASE_URL = 'http://localhost:3000';
const API_URL = 'http://localhost:8081';

// 测试结果收集
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

async function runTest(name, testFn) {
  results.total++;
  try {
    await testFn();
    results.passed.push(name);
    log(`${name}`, 'pass');
  } catch (err) {
    results.failed.push({ name, error: err.message.split('\n')[0] });
    log(`${name}: ${err.message.split('\n')[0]}`, 'fail');
  }
}

async function getAuthToken() {
  const loginResp = await fetch(`${API_URL}/api/auth/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ username: 'admin', password: 'admin123' })
  });
  const loginData = await loginResp.json();
  return loginData.data?.token;
}

async function main() {
  console.log('\n========================================');
  console.log('林夕置业日报系统 - UI端到端测试 v2');
  console.log('========================================\n');

  const browser = await chromium.launch({ headless: true });
  const context = await browser.newContext();
  const page = await context.newPage();

  // 获取认证token
  let token;
  try {
    token = await getAuthToken();
    if (token) {
      log(`API登录成功，获取Token: ${token.slice(0, 20)}...`);
    } else {
      throw new Error('登录失败，无Token');
    }
  } catch (err) {
    log(`API登录失败: ${err.message}`, 'fail');
    await browser.close();
    return;
  }

  const apiHeaders = {
    'Content-Type': 'application/json',
    'Authorization': `Bearer ${token}`
  };

  // ============ 第一部分：UI页面测试 ============
  console.log('\n--- 第一部分：UI页面测试 ---\n');

  // 测试1: 登录页面加载
  await runTest('登录页面加载', async () => {
    await page.goto(`${BASE_URL}/login`);
    await page.waitForLoadState('networkidle');
    const title = await page.title();
    if (!title.includes('林夕')) throw new Error(`页面标题不对: ${title}`);
    log(`页面标题: ${title}`);
  });

  // 测试2: 登录功能（通过UI操作）
  await runTest('登录功能', async () => {
    await page.goto(`${BASE_URL}/login`);
    await page.waitForLoadState('networkidle');
    
    // Element Plus输入框
    const usernameInput = page.locator('input').first();
    const passwordInput = page.locator('input[type="password"]');
    
    await usernameInput.fill('admin');
    await passwordInput.fill('admin123');
    
    // 点击登录按钮（el-button）
    const loginBtn = page.locator('.login-btn, button.el-button--primary').first();
    await loginBtn.click();
    
    // 等待页面跳转
    await page.waitForURL('**/home**', { timeout: 10000 }).catch(() => {});
    await page.waitForTimeout(2000);
    
    const url = page.url();
    if (url.includes('/login')) throw new Error('登录后未跳转，仍在登录页');
    log(`登录后URL: ${url}`);
  });

  // 测试3: 首页仪表盘
  await runTest('首页仪表盘', async () => {
    // 先确保已登录（注入token到localStorage）
    await page.goto(`${BASE_URL}/login`);
    await page.evaluate((t) => {
      localStorage.setItem('token', t);
      localStorage.setItem('userInfo', JSON.stringify({id:1, username:'admin', realName:'管理员'}));
    }, token);
    await page.goto(`${BASE_URL}/home`);
    await page.waitForLoadState('networkidle');
    await page.waitForTimeout(2000);
    const url = page.url();
    if (url.includes('/login')) throw new Error('被重定向到登录页');
    log(`首页URL: ${url}`);
  });

  // 测试4-12: 各管理页面导航（通过直接URL访问+token注入）
  const pageTests = [
    { name: '门店管理页面', path: '/store/list', keyword: 'store' },
    { name: '用户管理页面', path: '/system/user', keyword: 'system/user' },
    { name: '角色管理页面', path: '/system/role', keyword: 'system/role' },
    { name: '日报填报页面', path: '/report/fill', keyword: 'report' },
    { name: '日报查询页面', path: '/report/query', keyword: 'report' },
    { name: '投资人管理页面', path: '/investor/list', keyword: 'investor' },
    { name: '菜单管理页面', path: '/system/menu', keyword: 'system/menu' },
    { name: '字段配置页面', path: '/system/field', keyword: 'system/field' },
    { name: '模板管理页面', path: '/system/template', keyword: 'system/template' },
    { name: '总部看板页面', path: '/analysis/dashboard', keyword: 'analysis' },
  ];

  for (const pt of pageTests) {
    await runTest(pt.name, async () => {
      await page.goto(`${BASE_URL}${pt.path}`);
      await page.waitForLoadState('networkidle');
      await page.waitForTimeout(1500);
      const url = page.url();
      if (url.includes('/login')) throw new Error('被重定向到登录页，认证失败');
      if (!url.includes(pt.keyword)) throw new Error(`URL不含${pt.keyword}: ${url}`);
      log(`URL: ${url}`);
    });
  }

  // ============ 第二部分：API功能测试 ============
  console.log('\n--- 第二部分：API功能测试 ---\n');

  // 测试: 用户列表
  await runTest('API - 用户列表查询', async () => {
    const resp = await fetch(`${API_URL}/api/users?page=1&size=10`, { headers: apiHeaders });
    const data = await resp.json();
    if (data.code !== 200) throw new Error(`API错误: code=${data.code}`);
    log(`用户列表: ${data.data?.records?.length || 0} 条`);
  });

  // 测试: 门店列表
  await runTest('API - 门店列表查询', async () => {
    const resp = await fetch(`${API_URL}/api/stores?page=1&size=10`, { headers: apiHeaders });
    const data = await resp.json();
    if (data.code !== 200) throw new Error(`API错误: code=${data.code}`);
    log(`门店列表: ${data.data?.records?.length || 0} 条`);
  });

  // 测试: 投资人列表
  await runTest('API - 投资人列表查询', async () => {
    const resp = await fetch(`${API_URL}/api/investors?page=1&size=10`, { headers: apiHeaders });
    const data = await resp.json();
    if (data.code !== 200) throw new Error(`API错误: code=${data.code}`);
    log(`投资人列表: ${data.data?.records?.length || 0} 条`);
  });

  // 测试: 日报查询（使用正确的API路径）
  await runTest('API - 日报列表查询', async () => {
    const resp = await fetch(`${API_URL}/api/daily-reports/query?storeId=1&reportDate=2026-05-17`, { headers: apiHeaders });
    const data = await resp.json();
    if (data.code !== 200) throw new Error(`API错误: code=${data.code}, msg=${data.message}`);
    log(`日报查询: 成功`);
  });

  // 测试: 角色列表
  await runTest('API - 角色列表查询', async () => {
    const resp = await fetch(`${API_URL}/api/roles?page=1&size=10`, { headers: apiHeaders });
    const data = await resp.json();
    if (data.code !== 200) throw new Error(`API错误: code=${data.code}`);
    log(`角色列表: ${data.data?.records?.length || 0} 条`);
  });

  // 测试: 菜单树
  await runTest('API - 菜单树查询', async () => {
    const resp = await fetch(`${API_URL}/api/menus/tree`, { headers: apiHeaders });
    const data = await resp.json();
    if (data.code !== 200) throw new Error(`API错误: code=${data.code}`);
    log(`菜单树: ${data.data?.length || 0} 个菜单`);
  });

  // 测试: 仪表盘
  await runTest('API - 仪表盘数据', async () => {
    const resp = await fetch(`${API_URL}/api/reports/dashboard`, { headers: apiHeaders });
    const data = await resp.json();
    if (data.code !== 200) throw new Error(`API错误: code=${data.code}`);
    log(`仪表盘: totalStores=${data.data?.totalStores || 'N/A'}`);
  });

  // 测试: 字段配置（使用正确的API路径）
  await runTest('API - 字段配置查询', async () => {
    const resp = await fetch(`${API_URL}/api/extension-field-definitions?tableType=report&page=1&size=50`, { headers: apiHeaders });
    const data = await resp.json();
    if (data.code !== 200) throw new Error(`API错误: code=${data.code}`);
    log(`字段配置: ${data.data?.records?.length || data.data?.length || 0} 条`);
  });

  // 测试: 投资人CRUD
  await runTest('API - 投资人完整CRUD', async () => {
    // 创建
    let resp = await fetch(`${API_URL}/api/investors`, {
      method: 'POST', headers: apiHeaders,
      body: JSON.stringify({ investorName: 'E2E_TestInv', phone: '13888888888', userId: 1, status: 1 })
    });
    let data = await resp.json();
    if (data.code !== 200) throw new Error(`创建失败: ${data.message}`);
    
    // 查询
    resp = await fetch(`${API_URL}/api/investors?investorName=E2E_TestInv`, { headers: apiHeaders });
    data = await resp.json();
    const invId = data.data?.records?.[0]?.id;
    if (!invId) throw new Error('查询不到刚创建的投资人');
    
    // 更新
    resp = await fetch(`${API_URL}/api/investors/${invId}`, {
      method: 'PUT', headers: apiHeaders,
      body: JSON.stringify({ investorName: 'E2E_TestInv_Updated', phone: '13888888888' })
    });
    data = await resp.json();
    if (data.code !== 200) throw new Error(`更新失败: ${data.message}`);
    
    // 删除
    resp = await fetch(`${API_URL}/api/investors/${invId}`, {
      method: 'DELETE', headers: apiHeaders
    });
    data = await resp.json();
    if (data.code !== 200) throw new Error(`删除失败: ${data.message}`);
    
    log('CRUD全部成功');
  });

  // 测试: 用户创建（含phone唯一性校验）
  await runTest('API - 用户创建与校验', async () => {
    // 创建用户
    let resp = await fetch(`${API_URL}/api/users`, {
      method: 'POST', headers: apiHeaders,
      body: JSON.stringify({ username: 'e2e_testuser', realName: 'E2E测试', password: '123456', phone: '13777777777', userType: 1 })
    });
    let data = await resp.json();
    if (data.code !== 200) throw new Error(`创建失败: ${data.message}`);
    
    // 重复phone创建应失败
    resp = await fetch(`${API_URL}/api/users`, {
      method: 'POST', headers: apiHeaders,
      body: JSON.stringify({ username: 'e2e_testuser2', realName: 'E2E测试2', password: '123456', phone: '13777777777', userType: 1 })
    });
    data = await resp.json();
    if (data.code === 200) throw new Error('重复phone应该被拒绝');
    
    // 清理：删除测试用户
    resp = await fetch(`${API_URL}/api/users`, { headers: apiHeaders });
    data = await resp.json();
    const testUser = data.data?.records?.find(u => u.username === 'e2e_testuser');
    if (testUser) {
      await fetch(`${API_URL}/api/users/${testUser.id}`, { method: 'DELETE', headers: apiHeaders });
    }
    
    log('用户创建与phone校验通过');
  });

  await browser.close();

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
  console.error('测试执行失败:', err.message);
  process.exit(1);
});
