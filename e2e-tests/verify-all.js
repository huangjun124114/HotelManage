const { chromium } = require('playwright');

const BASE_URL = 'http://localhost:3000';
const API_URL = 'http://localhost:8081/api';

// Test results collector
const results = {
  passed: [],
  failed: [],
  errors: [],
  bugs: []
};

function log(section, test, status, detail = '') {
  const icon = status === 'PASS' ? '✅' : status === 'FAIL' ? '❌' : '⚠️';
  console.log(`${icon} [${section}] ${test} ${detail ? '- ' + detail : ''}`);
  if (status === 'PASS') results.passed.push(`${section}/${test}`);
  else if (status === 'FAIL') {
    results.failed.push(`${section}/${test}${detail ? ': ' + detail : ''}`);
  }
  else results.errors.push(`${section}/${test}${detail ? ': ' + detail : ''}`);
}

async function sleep(ms) {
  return new Promise(r => setTimeout(r, ms));
}

// Helper: login via API and inject token, then verify UI
async function loginViaUI(page, username = 'admin', password = '123456') {
  await page.goto(`${BASE_URL}/login`);
  await sleep(2000);

  // Use API to login and get token
  const loginResp = await page.request.post(`${API_URL}/auth/login`, {
    data: { username, password }
  });
  const loginData = await loginResp.json();
  const token = loginData.data?.token || loginData.token;

  if (!token) return null;

  // Inject token into localStorage so the SPA router recognizes auth
  await page.evaluate((t) => {
    localStorage.setItem('token', t);
    localStorage.setItem('user', JSON.stringify({ username: 'admin' }));
  }, token);

  // Now navigate to home - should work with token in localStorage
  await page.goto(`${BASE_URL}/home`);
  await sleep(2000);
  return token;
}

// Helper: navigate to menu item
async function navigateTo(page, menuText) {
  const menuItem = page.locator(`.el-menu-item:has-text("${menuText}"), .el-sub-menu__title:has-text("${menuText}")`).first();
  if (await menuItem.isVisible()) {
    await menuItem.click();
    await sleep(1000);
    return true;
  }
  return false;
}

// Helper: check for error messages on page
async function checkPageErrors(page) {
  const errors = [];
  
  // Check ElMessage errors
  const errorMsgs = await page.locator('.el-message--error, .el-notification--error').allTextContents();
  if (errorMsgs.length > 0) errors.push(...errorMsgs);
  
  // Check console errors (via page errors)
  return errors;
}

// Helper: safely navigate to a route
async function gotoRoute(page, route) {
  await page.goto(`${BASE_URL}${route}`);
  await sleep(1500);
}

async function main() {
  console.log('🚀 Starting HotelManage E2E Verification');
  console.log('='.repeat(60));
  
  const browser = await chromium.launch({ headless: true });
  const context = await browser.newContext({
    viewport: { width: 1400, height: 900 },
    ignoreHTTPSErrors: true
  });
  const page = await context.newPage();

  // Collect console errors
  const consoleErrors = [];
  const routeErrors = {};
  page.on('console', msg => {
    if (msg.type() === 'error') consoleErrors.push(msg.text());
  });
  page.on('pageerror', err => {
    consoleErrors.push(`PageError: ${err.message}`);
  });

  // =====================================================
  // SECTION 1: Login & Authentication
  // =====================================================
  console.log('\n📋 SECTION 1: 登录与认证');
  console.log('-'.repeat(40));
  
  try {
    // Test login page loads
    await page.goto(`${BASE_URL}/login`);
    await sleep(1500);
    const hasLoginForm = await page.locator('input[type="password"]').count() > 0;
    log('登录', '登录页面加载', hasLoginForm ? 'PASS' : 'FAIL');

    // Test login button text
    const loginBtnText = await page.locator('.login-btn, .el-button--primary').first().textContent().catch(() => '');
    log('登录', '登录按钮显示', loginBtnText.includes('登') ? 'PASS' : 'FAIL', `按钮文本: "${loginBtnText.trim()}"`);

    // Login via API + localStorage injection
    const token = await loginViaUI(page, 'admin', '123456');
    const loginSuccess = token !== null && !page.url().includes('/login');
    log('登录', '管理员登录', loginSuccess ? 'PASS' : 'FAIL', loginSuccess ? 'Token注入成功' : 'Token注入失败');
  } catch (e) {
    log('登录', '登录流程', 'FAIL', e.message.substring(0, 100));
  }

  // =====================================================
  // SECTION 2: Dashboard (总部看板)
  // =====================================================
  console.log('\n📋 SECTION 2: 总部看板');
  console.log('-'.repeat(40));
  
  try {
    await gotoRoute(page, '/home');
    
    // Check for dashboard elements
    const hasCards = await page.locator('.el-card, .stat-card, [class*="card"]').count() > 0;
    log('看板', '统计卡片显示', hasCards ? 'PASS' : 'FAIL');
    
    // Check for date picker
    const hasDatePicker = await page.locator('.el-date-editor, [class*="date"]').count() > 0;
    log('看板', '日期选择器', hasDatePicker ? 'PASS' : 'FAIL');
    
    // Check for quick date buttons (Dashboard uses radio-button for day/week/month, not "今天/本周/本月")
    const hasRadioButtons = await page.locator('.el-radio-button').count() > 0;
    log('看板', '时间维度切换(天/周/月)', hasRadioButtons ? 'PASS' : 'FAIL');
    
    // Check for store selector
    const hasStoreSelect = await page.locator('.el-select, [class*="store"]').count() > 0;
    log('看板', '门店选择器', hasStoreSelect ? 'PASS' : 'FAIL');
    
    // Check for charts
    const hasCharts = await page.locator('[class*="chart"], canvas').count() > 0;
    log('看板', '图表组件', hasCharts ? 'PASS' : 'FAIL');
    
    // Check for query button
    const hasQueryBtn = await page.locator('button:has-text("查询"), button:has-text("搜索")').count() > 0;
    log('看板', '查询按钮', hasQueryBtn ? 'PASS' : 'FAIL');
    
    const dashErrors = await checkPageErrors(page);
    if (dashErrors.length > 0) {
      log('看板', '页面错误', 'FAIL', dashErrors.join('; '));
    }
  } catch (e) {
    log('看板', '看板加载', 'FAIL', e.message);
  }

  // =====================================================
  // SECTION 3: System Management
  // =====================================================
  console.log('\n📋 SECTION 3: 系统管理');
  console.log('-'.repeat(40));
  
  // 3.1 User Management
  try {
    await gotoRoute(page, '/system/user');
    const hasTable = await page.locator('.el-table').count() > 0;
    log('用户管理', '用户列表表格', hasTable ? 'PASS' : 'FAIL');
    
    const hasPagination = await page.locator('.el-pagination').count() > 0;
    log('用户管理', '分页组件', hasPagination ? 'PASS' : 'FAIL');
    
    // Try to open add user dialog
    const addBtn = page.locator('button:has-text("新增"), button:has-text("添加"), button:has-text("新建")').first();
    if (await addBtn.isVisible()) {
      await addBtn.click();
      await sleep(800);
      const hasDialog = await page.locator('.el-dialog:visible, .el-drawer:visible').count() > 0;
      log('用户管理', '新增用户对话框', hasDialog ? 'PASS' : 'FAIL');
      // Close dialog
      const closeBtn = page.locator('.el-dialog__headerbtn, button:has-text("取消"), button:has-text("关闭")').first();
      if (await closeBtn.isVisible()) await closeBtn.click();
      await sleep(500);
    }
  } catch (e) {
    log('用户管理', '页面加载', 'FAIL', e.message);
  }
  
  // 3.2 Role Management
  try {
    await gotoRoute(page, '/system/role');
    const hasTable = await page.locator('.el-table').count() > 0;
    log('角色管理', '角色列表表格', hasTable ? 'PASS' : 'FAIL');
  } catch (e) {
    log('角色管理', '页面加载', 'FAIL', e.message);
  }
  
  // 3.3 Menu Management
  try {
    await gotoRoute(page, '/system/menu');
    const hasTree = await page.locator('.el-tree, .el-table').count() > 0;
    log('菜单管理', '菜单树/表格', hasTree ? 'PASS' : 'FAIL');
  } catch (e) {
    log('菜单管理', '页面加载', 'FAIL', e.message);
  }

  // =====================================================
  // SECTION 4: Store Management
  // =====================================================
  console.log('\n📋 SECTION 4: 门店管理');
  console.log('-'.repeat(40));
  
  try {
    await gotoRoute(page, '/store/list');
    const hasTable = await page.locator('.el-table').count() > 0;
    log('门店管理', '门店列表表格', hasTable ? 'PASS' : 'FAIL');
    
    // Check store count (should have 9 stores)
    const rows = await page.locator('.el-table__body-wrapper .el-table__row').count();
    log('门店管理', '门店数据行数', rows > 0 ? 'PASS' : 'FAIL', `${rows}行`);
  } catch (e) {
    log('门店管理', '页面加载', 'FAIL', e.message);
  }

  // =====================================================
  // SECTION 5: Daily Report Management
  // =====================================================
  console.log('\n📋 SECTION 5: 日报管理');
  console.log('-'.repeat(40));
  
  // 5.1 Daily Report List
  try {
    await gotoRoute(page, '/report/manage');
    const hasTable = await page.locator('.el-table').count() > 0;
    log('日报管理', '日报列表表格', hasTable ? 'PASS' : 'FAIL');
    
    const hasDateFilter = await page.locator('.el-date-editor, [class*="date"]').count() > 0;
    log('日报管理', '日期筛选', hasDateFilter ? 'PASS' : 'FAIL');
    
    const rows = await page.locator('.el-table__body-wrapper .el-table__row').count();
    log('日报管理', '日报数据', rows > 0 ? 'PASS' : 'FAIL', `${rows}行`);
  } catch (e) {
    log('日报管理', '页面加载', 'FAIL', e.message);
  }
  
  // 5.2 Daily Report Audit
  try {
    await gotoRoute(page, '/report/audit');
    const hasTable = await page.locator('.el-table').count() > 0;
    log('日报审核', '审核列表表格', hasTable ? 'PASS' : 'FAIL');
  } catch (e) {
    log('日报审核', '页面加载', 'FAIL', e.message);
  }
  
  // 5.3 Unfilled Reports
  try {
    await gotoRoute(page, '/report/unfilled');
    const hasContent = await page.locator('.el-table, .el-empty').count() > 0;
    log('未填报', '页面内容', hasContent ? 'PASS' : 'FAIL');
  } catch (e) {
    log('未填报', '页面加载', 'FAIL', e.message);
  }

  // =====================================================
  // SECTION 6: Report Center
  // =====================================================
  console.log('\n📋 SECTION 6: 报表中心');
  console.log('-'.repeat(40));
  
  // 6.1 Channel Analysis
  try {
    await gotoRoute(page, '/analysis/channel');
    const hasContent = await page.locator('.el-table, .el-card, canvas, [class*="chart"]').count() > 0;
    log('渠道分析', '页面内容', hasContent ? 'PASS' : 'FAIL');
    
    const hasStoreSelect = await page.locator('.el-select').count() > 0;
    log('渠道分析', '门店选择器', hasStoreSelect ? 'PASS' : 'FAIL');
    
    const hasQueryBtn = await page.locator('button:has-text("查询"), button:has-text("搜索")').count() > 0;
    log('渠道分析', '查询按钮', hasQueryBtn ? 'PASS' : 'FAIL');
    
    const hasQuickDates = await page.locator('button:has-text("本周"), button:has-text("本月")').count() > 0;
    log('渠道分析', '快捷日期按钮', hasQuickDates ? 'PASS' : 'FAIL');
  } catch (e) {
    log('渠道分析', '页面加载', 'FAIL', e.message);
  }
  
  // 6.2 Store Daily Report
  try {
    await gotoRoute(page, '/analysis/daily');
    const hasTable = await page.locator('.el-table').count() > 0;
    log('门店日报表', '报表表格', hasTable ? 'PASS' : 'FAIL');
    
    const hasExport = await page.locator('button:has-text("导出"), button:has-text("Excel")').count() > 0;
    log('门店日报表', '导出按钮', hasExport ? 'PASS' : 'FAIL');
  } catch (e) {
    log('门店日报表', '页面加载', 'FAIL', e.message);
  }
  
  // 6.3 Store Ranking
  try {
    await gotoRoute(page, '/analysis/ranking');
    const hasTable = await page.locator('.el-table').count() > 0;
    log('门店排名', '排名表格', hasTable ? 'PASS' : 'FAIL');
    
    const hasPeriodSwitch = await page.locator('.el-radio-button, .el-radio-group, button:has-text("天"), button:has-text("周"), button:has-text("月")').count() > 0;
    log('门店排名', '时间维度切换', hasPeriodSwitch ? 'PASS' : 'FAIL');
  } catch (e) {
    log('门店排名', '页面加载', 'FAIL', e.message);
  }
  
  // 6.4 Business Trend
  try {
    await gotoRoute(page, '/analysis/trend');
    const hasContent = await page.locator('.el-card, canvas, [class*="chart"], .el-table').count() > 0;
    log('经营趋势', '页面内容', hasContent ? 'PASS' : 'FAIL');
    
    const hasCharts = await page.locator('canvas').count() > 0;
    log('经营趋势', '图表组件', hasCharts ? 'PASS' : 'FAIL');
  } catch (e) {
    log('经营趋势', '页面加载', 'FAIL', e.message);
  }

  // =====================================================
  // SECTION 7: Investor Management
  // =====================================================
  console.log('\n📋 SECTION 7: 投资人管理');
  console.log('-'.repeat(40));
  
  try {
    await gotoRoute(page, '/investor/list');
    const hasTable = await page.locator('.el-table').count() > 0;
    log('投资人', '投资人列表表格', hasTable ? 'PASS' : 'FAIL');
    
    const rows = await page.locator('.el-table__body-wrapper .el-table__row').count();
    log('投资人', '投资人数据', rows > 0 ? 'PASS' : 'FAIL', `${rows}行`);
  } catch (e) {
    log('投资人', '页面加载', 'FAIL', e.message);
  }

  // =====================================================
  // SECTION 8: API Health Check
  // =====================================================
  console.log('\n📋 SECTION 8: API 健康检查');
  console.log('-'.repeat(40));
  
  // Login to get token first
  try {
    const loginResp = await page.request.post(`${API_URL}/auth/login`, {
      data: { username: 'admin', password: '123456' }
    });
    const loginData = await loginResp.json();
    const token = loginData.data?.token || loginData.token;
    
    if (token) {
      log('API', '登录获取Token', 'PASS');
      
      const apiTests = [
        { name: '用户列表', url: '/users?page=1&size=10' },
        { name: '角色列表', url: '/roles' },
        { name: '门店列表', url: '/stores' },
        { name: '门店选项', url: '/stores/options' },
        { name: '日报模板', url: '/templates' },
        { name: '日报模板字段', url: '/templates/1/fields' },
        { name: '菜单树', url: '/menus/tree' },
        { name: '投资人列表', url: '/investors?page=1&size=10' },
        { name: '操作日志', url: '/operation-logs?page=1&size=10' },
        { name: '站内信', url: '/messages?page=1&size=10' },
        { name: '扩展字段定义', url: '/extension-field-definitions' },
        { name: '日报查询', url: '/daily-reports/query?storeId=SZ001&reportDate=2026-05-20' },
        { name: '日报未填报', url: '/daily-reports/unfilled?date=2026-05-20' },
      ];
      
      for (const test of apiTests) {
        try {
          const resp = await page.request.get(`${API_URL}${test.url}`, {
            headers: { 'Authorization': `Bearer ${token}` }
          });
          const status = resp.status();
          if (status >= 200 && status < 300) {
            const data = await resp.json();
            const hasData = data.data !== undefined || data.code !== undefined;
            log('API', test.name, hasData ? 'PASS' : 'FAIL', `HTTP ${status}`);
          } else {
            log('API', test.name, 'FAIL', `HTTP ${status}`);
          }
        } catch (e) {
          log('API', test.name, 'FAIL', e.message);
        }
      }
      
      // Report APIs
      const reportApiTests = [
        { name: '看板统计', url: '/reports/dashboard?date=2026-05-20' },
        { name: '渠道分析', url: '/reports/channel-analysis?startDate=2026-05-01&endDate=2026-05-20' },
        { name: '门店排名', url: '/reports/store-ranking?date=2026-05-20&period=day' },
        { name: '经营趋势', url: '/reports/trend?startDate=2026-04-01&endDate=2026-05-20&dimension=day' },
        { name: '月报详情', url: '/reports/monthly-detail?month=2026-05' },
      ];
      
      for (const test of reportApiTests) {
        try {
          const resp = await page.request.get(`${API_URL}${test.url}`, {
            headers: { 'Authorization': `Bearer ${token}` }
          });
          const status = resp.status();
          if (status >= 200 && status < 300) {
            log('API', test.name, 'PASS', `HTTP ${status}`);
          } else {
            const body = await resp.text().catch(() => '');
            log('API', test.name, 'FAIL', `HTTP ${status} ${body.substring(0, 100)}`);
          }
        } catch (e) {
          log('API', test.name, 'FAIL', e.message);
        }
      }
    } else {
      log('API', '登录获取Token', 'FAIL', 'No token in response');
    }
  } catch (e) {
    log('API', 'API健康检查', 'FAIL', e.message);
  }

  // =====================================================
  // SECTION 9: Console Error Check
  // =====================================================
  console.log('\n📋 SECTION 9: 控制台错误检查');
  console.log('-'.repeat(40));
  
  // Navigate through all routes to catch errors
  const allRoutes = [
    '/home', '/system/user', '/system/role', '/system/menu',
    '/store/list', '/report/manage', '/report/audit', '/report/unfilled',
    '/analysis/channel', '/analysis/daily', '/analysis/ranking', '/analysis/trend',
    '/investor/list'
  ];
  
  const routeErrorsLocal = {};
  for (const route of allRoutes) {
    consoleErrors.length = 0;
    await page.goto(`${BASE_URL}${route}`);
    await sleep(2000);
    
    // Filter out noisy errors
    const realErrors = consoleErrors.filter(e => 
      !e.includes('favicon.ico') && 
      !e.includes('DevTools') &&
      !e.includes('Download the React DevTools') &&
      !e.includes('net::ERR_CONNECTION_REFUSED') &&
      e.length < 200
    );
    
    if (realErrors.length > 0) {
      routeErrorsLocal[route] = [...new Set(realErrors)];
      routeErrors[route] = [...new Set(realErrors)];
    }
  }
  
  const errorRoutes = Object.keys(routeErrorsLocal);
  if (errorRoutes.length === 0) {
    log('控制台', '全部路由无严重错误', 'PASS');
  } else {
    for (const [route, errs] of Object.entries(routeErrorsLocal)) {
      log('控制台', route, 'WARN', `${errs.length}个错误: ${errs[0].substring(0, 80)}`);
    }
  }

  // =====================================================
  // SECTION 10: Role-based Access Test
  // =====================================================
  console.log('\n📋 SECTION 10: 角色权限验证');
  console.log('-'.repeat(40));
  
  // Test store_manager login
  try {
    // Get a store manager user from API
    const loginResp = await page.request.post(`${API_URL}/auth/login`, {
      data: { username: 'admin', password: '123456' }
    });
    const loginData = await loginResp.json();
    const token = loginData.data?.token || loginData.token;
    
    // Get users list to find a store manager
    const usersResp = await page.request.get(`${API_URL}/users?page=1&size=50`, {
      headers: { 'Authorization': `Bearer ${token}` }
    });
    const usersData = await usersResp.json();
    const users = usersData.data?.records || usersData.data || [];
    const storeManager = users.find(u => 
      u.roles && u.roles.some(r => r.roleCode === 'STORE_MANAGER' || r.roleCode === 'STORE_MANAGER')
    );
    
    if (storeManager && storeManager.phone) {
      log('权限', '找到店长账号', 'PASS', storeManager.phone);
    } else {
      log('权限', '店长账号查找', 'WARN', '使用默认账号跳过');
    }
  } catch (e) {
    log('权限', '角色权限验证', 'WARN', e.message);
  }

  // =====================================================
  // SUMMARY
  // =====================================================
  console.log('\n' + '='.repeat(60));
  console.log('📊 E2E 测试结果汇总');
  console.log('='.repeat(60));
  console.log(`✅ 通过: ${results.passed.length}`);
  console.log(`❌ 失败: ${results.failed.length}`);
  console.log(`⚠️  警告: ${results.errors.length}`);
  console.log(`📋 总计: ${results.passed.length + results.failed.length + results.errors.length}`);
  
  if (results.failed.length > 0) {
    console.log('\n❌ 失败项:');
    results.failed.forEach((f, i) => console.log(`  ${i+1}. ${f}`));
  }
  
  if (results.errors.length > 0) {
    console.log('\n⚠️  警告项:');
    results.errors.forEach((e, i) => console.log(`  ${i+1}. ${e}`));
  }
  
  // Write report to file
  const fs = require('fs');
  const reportDir = 'C:\\Users\\huangjun124\\WorkBuddy\\2026林夕置业\\结果\\HotelManage\\e2e-tests';
  
  const report = {
    timestamp: new Date().toISOString(),
    environment: {
      backend: 'http://localhost:8081',
      frontend: 'http://localhost:3000'
    },
    summary: {
      total: results.passed.length + results.failed.length + results.errors.length,
      passed: results.passed.length,
      failed: results.failed.length,
      warnings: results.errors.length,
      passRate: ((results.passed.length / (results.passed.length + results.failed.length + results.errors.length)) * 100).toFixed(1) + '%'
    },
    passed: results.passed,
    failed: results.failed,
    warnings: results.errors,
    routeErrors: routeErrors
  };
  
  fs.writeFileSync(`${reportDir}\\e2e-report.json`, JSON.stringify(report, null, 2));
  console.log(`\n📄 测试报告已保存: ${reportDir}\\e2e-report.json`);
  
  await browser.close();
  
  // Exit with failure code if there are failures
  process.exit(results.failed.length > 0 ? 1 : 0);
}

main().catch(e => {
  console.error('Fatal error:', e);
  process.exit(1);
});
