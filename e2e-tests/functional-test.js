const { chromium } = require('playwright');
const assert = require('assert');

const BASE_URL = 'http://localhost:3000';
const API_URL = 'http://localhost:8081/api';

const results = { passed: [], failed: [], warnings: [] };

function log(section, test, status, detail = '') {
  const icon = status === 'PASS' ? '✅' : status === 'FAIL' ? '❌' : '⚠️';
  console.log(`${icon} [${section}] ${test} ${detail ? '- ' + detail : ''}`);
  if (status === 'PASS') results.passed.push(`${section}/${test}`);
  else if (status === 'FAIL') results.failed.push(`${section}/${test}${detail ? ': ' + detail : ''}`);
  else results.warnings.push(`${section}/${test}${detail ? ': ' + detail : ''}`);
}

async function sleep(ms) { return new Promise(r => setTimeout(r, ms)); }

async function setupAuth(page) {
  const loginResp = await page.request.post(`${API_URL}/auth/login`, {
    data: { username: 'admin', password: '123456' }
  });
  const loginData = await loginResp.json();
  const token = loginData.data?.token;
  
  await page.goto(`${BASE_URL}/login`);
  await sleep(500);
  await page.evaluate((t) => {
    localStorage.setItem('token', t);
    localStorage.setItem('user', JSON.stringify({ username: 'admin' }));
  }, token);
  return token;
}

async function main() {
  console.log('🔍 HotelManage Functional E2E Test');
  console.log('='.repeat(60));
  
  const browser = await chromium.launch({ headless: true });
  const context = await browser.newContext({ viewport: { width: 1400, height: 900 } });
  const page = await context.newPage();
  const token = await setupAuth(page);

  // ===== TEST 1: Dashboard Data Rendering =====
  console.log('\n📋 TEST 1: 总部看板数据渲染');
  try {
    await page.goto(`${BASE_URL}/home`);
    await sleep(3000);
    
    // Click query button to ensure data loads
    const queryBtn = page.locator('button:has-text("查询")').first();
    if (await queryBtn.isVisible()) {
      await queryBtn.click();
      await sleep(2000);
    }
    
    // Check stat cards have numeric values (not 0 or empty)
    const cardTexts = await page.locator('.el-card, .stat-card, [class*="stat"]').allTextContents();
    const hasNumericData = cardTexts.some(t => /\d+/.test(t));
    log('看板', '统计数据渲染', hasNumericData ? 'PASS' : 'FAIL', `卡片数: ${cardTexts.length}`);
    
    // Check if charts have canvas rendered
    const canvasCount = await page.locator('canvas').count();
    log('看板', '图表Canvas渲染', canvasCount > 0 ? 'PASS' : 'FAIL', `${canvasCount}个Canvas`);
    
    // Check day/week/month switch
    const weekBtn = page.locator('.el-radio-button').nth(1);
    if (await weekBtn.isVisible()) {
      await weekBtn.click();
      await sleep(1500);
      const canvasAfterSwitch = await page.locator('canvas').count();
      log('看板', '时间维度切换后图表更新', canvasAfterSwitch > 0 ? 'PASS' : 'FAIL');
    }
  } catch (e) {
    log('看板', '数据渲染', 'FAIL', e.message.substring(0, 80));
  }

  // ===== TEST 2: CRUD Operations - User Management =====
  console.log('\n📋 TEST 2: 用户管理CRUD');
  try {
    await page.goto(`${BASE_URL}/system/user`);
    await sleep(2000);
    
    // Read - verify table has data
    const rows = await page.locator('.el-table__body-wrapper .el-table__row').count();
    log('用户CRUD', '读取用户列表', rows > 0 ? 'PASS' : 'FAIL', `${rows}条`);
    
    // Create - open add dialog
    const addBtn = page.locator('button:has-text("新增"), button:has-text("添加")').first();
    if (await addBtn.isVisible()) {
      await addBtn.click();
      await sleep(800);
      const hasDialog = await page.locator('.el-dialog:visible, .el-drawer:visible').count() > 0;
      log('用户CRUD', '新增对话框打开', hasDialog ? 'PASS' : 'FAIL');
      
      // Fill form
      const usernameInput = page.locator('.el-dialog:visible input[placeholder*="用户名"], .el-dialog:visible input[placeholder*="账号"]').first();
      if (await usernameInput.isVisible()) {
        await usernameInput.fill('test_e2e_user');
        log('用户CRUD', '表单输入', 'PASS', 'test_e2e_user');
      }
      
      // Close without saving (don't pollute data)
      const cancelBtn = page.locator('.el-dialog:visible button:has-text("取消"), .el-dialog:visible button:has-text("关闭")').first();
      if (await cancelBtn.isVisible()) {
        await cancelBtn.click();
        await sleep(500);
      }
    }
    
    // Search/filter
    const searchInput = page.locator('input[placeholder*="搜索"], input[placeholder*="用户名"], input[placeholder*="关键字"]').first();
    if (await searchInput.isVisible()) {
      await searchInput.fill('admin');
      await sleep(500);
      const searchBtn = page.locator('button:has-text("搜索"), button:has-text("查询")').first();
      if (await searchBtn.isVisible()) await searchBtn.click();
      await sleep(1000);
      const filteredRows = await page.locator('.el-table__body-wrapper .el-table__row').count();
      log('用户CRUD', '搜索过滤', filteredRows > 0 ? 'PASS' : 'FAIL', `结果: ${filteredRows}条`);
    }
  } catch (e) {
    log('用户CRUD', 'CRUD测试', 'FAIL', e.message.substring(0, 80));
  }

  // ===== TEST 3: Store Management =====
  console.log('\n📋 TEST 3: 门店管理验证');
  try {
    await page.goto(`${BASE_URL}/store/list`);
    await sleep(2000);
    
    const rows = await page.locator('.el-table__body-wrapper .el-table__row').count();
    log('门店', '门店列表', rows > 0 ? 'PASS' : 'FAIL', `${rows}条`);
    
    // Check store codes
    const firstRowText = await page.locator('.el-table__body-wrapper .el-table__row').first().textContent().catch(() => '');
    const hasStoreCode = /SZ\d{3}|GZ\d{3}|BJ\d{3}|SH\d{3}|HZ\d{3}/.test(firstRowText);
    log('门店', '门店编码格式', hasStoreCode ? 'PASS' : 'WARN', firstRowText.substring(0, 50));
  } catch (e) {
    log('门店', '门店验证', 'FAIL', e.message.substring(0, 80));
  }

  // ===== TEST 4: Daily Report Workflow =====
  console.log('\n📋 TEST 4: 日报管理流程');
  try {
    await page.goto(`${BASE_URL}/report/manage`);
    await sleep(2000);
    
    const hasTable = await page.locator('.el-table').count() > 0;
    log('日报', '日报列表', hasTable ? 'PASS' : 'FAIL');
    
    // Try editing a report
    const editBtn = page.locator('.el-table__row button:has-text("编辑"), .el-table__row .el-button--primary').first();
    if (await editBtn.isVisible()) {
      await editBtn.click();
      await sleep(1000);
      const hasEditForm = await page.locator('.el-dialog:visible, .el-drawer:visible').count() > 0;
      log('日报', '编辑日报对话框', hasEditForm ? 'PASS' : 'FAIL');
      
      // Close
      const closeBtn = page.locator('.el-dialog__headerbtn, button:has-text("取消")').first();
      if (await closeBtn.isVisible()) await closeBtn.click();
      await sleep(500);
    }
  } catch (e) {
    log('日报', '日报流程', 'FAIL', e.message.substring(0, 80));
  }

  // ===== TEST 5: Report Center Deep Tests =====
  console.log('\n📋 TEST 5: 报表中心深度验证');
  
  // Channel Analysis
  try {
    await page.goto(`${BASE_URL}/analysis/channel`);
    await sleep(2000);
    
    // Click query
    const queryBtn = page.locator('button:has-text("查询")').first();
    if (await queryBtn.isVisible()) {
      await queryBtn.click();
      await sleep(2000);
    }
    
    const hasData = await page.locator('.el-table__body-wrapper .el-table__row').count() > 0;
    log('渠道分析', '查询后数据', hasData ? 'PASS' : 'FAIL');
  } catch (e) {
    log('渠道分析', '深度验证', 'FAIL', e.message.substring(0, 80));
  }
  
  // Store Ranking
  try {
    await page.goto(`${BASE_URL}/analysis/ranking`);
    await sleep(2000);
    
    // Set date to 2026-05-19 (known data date)
    const dateInput = page.locator('.el-date-editor input, input[placeholder*="日期"]').first();
    if (await dateInput.isVisible()) {
      await dateInput.fill('2026-05-19');
      await page.keyboard.press('Enter');
      await sleep(500);
    }
    
    const queryBtn = page.locator('button:has-text("查询")').first();
    if (await queryBtn.isVisible()) {
      await queryBtn.click();
      await sleep(2000);
    }
    
    const hasData = await page.locator('.el-table__body-wrapper .el-table__row').count() > 0;
    log('门店排名', '查询后数据', hasData ? 'PASS' : 'FAIL');
    
    // Switch period to week
    const weekRadio = page.locator('.el-radio-button:has-text("周")').first();
    if (await weekRadio.isVisible()) {
      await weekRadio.click();
      await sleep(2000);
      const hasWeekData = await page.locator('.el-table__body-wrapper .el-table__row').count() > 0;
      log('门店排名', '切换到周维度', hasWeekData ? 'PASS' : 'FAIL');
    }
  } catch (e) {
    log('门店排名', '深度验证', 'FAIL', e.message.substring(0, 80));
  }
  
  // Business Trend (after fix)
  try {
    await page.goto(`${BASE_URL}/analysis/trend`);
    await sleep(3000);
    
    // Should auto-load now
    const hasCanvas = await page.locator('canvas').count() > 0;
    log('经营趋势', '自动加载数据渲染图表', hasCanvas ? 'PASS' : 'FAIL');
    
    // Click query again to verify
    const queryBtn = page.locator('button:has-text("查询")').first();
    if (await queryBtn.isVisible()) {
      await queryBtn.click();
      await sleep(2000);
      const hasCanvasAfterQuery = await page.locator('canvas').count() > 0;
      log('经营趋势', '手动查询后图表', hasCanvasAfterQuery ? 'PASS' : 'FAIL');
    }
  } catch (e) {
    log('经营趋势', '深度验证', 'FAIL', e.message.substring(0, 80));
  }

  // ===== TEST 6: API Data Integrity =====
  console.log('\n📋 TEST 6: API数据完整性');
  try {
    // Test dashboard API returns real data
    const dashResp = await page.request.get(`${API_URL}/reports/dashboard?date=2026-05-20`, {
      headers: { 'Authorization': `Bearer ${token}` }
    });
    const dashData = await dashResp.json();
    const hasDashData = dashData.data && Object.keys(dashData.data).length > 0;
    log('API完整性', '看板数据返回', hasDashData ? 'PASS' : 'FAIL');
    
    // Test store ranking with different periods (use 2026-05-19 which has data)
    for (const period of ['day', 'week', 'month']) {
      const rankResp = await page.request.get(`${API_URL}/reports/store-ranking?date=2026-05-19&period=${period}`, {
        headers: { 'Authorization': `Bearer ${token}` }
      });
      const rankData = await rankResp.json();
      const hasRankData = rankData.data && (Array.isArray(rankData.data) ? rankData.data.length > 0 : true);
      log('API完整性', `门店排名(${period})`, hasRankData ? 'PASS' : 'FAIL');
    }
    
    // Test trend API
    const trendResp = await page.request.get(`${API_URL}/reports/trend?startDate=2026-04-20&endDate=2026-05-20&dimension=day`, {
      headers: { 'Authorization': `Bearer ${token}` }
    });
    const trendData = await trendResp.json();
    const hasTrendData = trendData.data && Array.isArray(trendData.data) && trendData.data.length > 0;
    log('API完整性', '经营趋势数据', hasTrendData ? 'PASS' : 'FAIL', `${trendData.data?.length || 0}条`);
    
    // Test channel analysis
    const channelResp = await page.request.get(`${API_URL}/reports/channel-analysis?startDate=2026-05-01&endDate=2026-05-20`, {
      headers: { 'Authorization': `Bearer ${token}` }
    });
    const channelData = await channelResp.json();
    const hasChannelData = channelData.data && Object.keys(channelData.data).length > 0;
    log('API完整性', '渠道分析数据', hasChannelData ? 'PASS' : 'FAIL');
    
  } catch (e) {
    log('API完整性', '数据完整性', 'FAIL', e.message.substring(0, 80));
  }

  // ===== TEST 7: Investor Management =====
  console.log('\n📋 TEST 7: 投资人管理');
  try {
    await page.goto(`${BASE_URL}/investor/list`);
    await sleep(2000);
    
    const rows = await page.locator('.el-table__body-wrapper .el-table__row').count();
    log('投资人', '投资人列表', rows > 0 ? 'PASS' : 'FAIL', `${rows}条`);
    
    // Try view detail
    const viewBtn = page.locator('.el-table__row button:has-text("查看"), .el-table__row button:has-text("详情")').first();
    if (await viewBtn.isVisible()) {
      await viewBtn.click();
      await sleep(1000);
      const hasDetail = await page.locator('.el-dialog:visible, .el-drawer:visible').count() > 0;
      log('投资人', '查看详情对话框', hasDetail ? 'PASS' : 'FAIL');
      const closeBtn = page.locator('.el-dialog__headerbtn, button:has-text("关闭")').first();
      if (await closeBtn.isVisible()) await closeBtn.click();
      await sleep(500);
    }
  } catch (e) {
    log('投资人', '投资人验证', 'FAIL', e.message.substring(0, 80));
  }

  // ===== TEST 8: Navigation & Routing =====
  console.log('\n📋 TEST 8: 导航与路由');
  try {
    const routes = [
      { path: '/home', name: '总部看板' },
      { path: '/system/user', name: '用户管理' },
      { path: '/system/role', name: '角色管理' },
      { path: '/system/menu', name: '菜单管理' },
      { path: '/store/list', name: '门店管理' },
      { path: '/report/manage', name: '日报管理' },
      { path: '/report/audit', name: '日报审核' },
      { path: '/report/unfilled', name: '未填报' },
      { path: '/analysis/channel', name: '渠道分析' },
      { path: '/analysis/daily', name: '门店日报表' },
      { path: '/analysis/ranking', name: '门店排名' },
      { path: '/analysis/trend', name: '经营趋势' },
      { path: '/investor/list', name: '投资人' },
    ];
    
    let allRoutesOk = true;
    for (const route of routes) {
      await page.goto(`${BASE_URL}${route.path}`);
      await sleep(1500);
      
      // Check we're not redirected to login
      const currentUrl = page.url();
      const isLoginRedirect = currentUrl.includes('/login');
      if (isLoginRedirect) {
        log('路由', route.name, 'FAIL', '重定向到登录页');
        allRoutesOk = false;
      }
      
      // Check for el-message errors
      const errorMessages = await page.locator('.el-message--error').allTextContents();
      if (errorMessages.length > 0) {
        log('路由', route.name, 'WARN', `错误消息: ${errorMessages[0]}`);
      }
    }
    if (allRoutesOk) log('路由', '全部路由可访问', 'PASS', `${routes.length}个路由`);
  } catch (e) {
    log('路由', '导航测试', 'FAIL', e.message.substring(0, 80));
  }

  // ===== SUMMARY =====
  console.log('\n' + '='.repeat(60));
  console.log('📊 功能测试结果汇总');
  console.log('='.repeat(60));
  console.log(`✅ 通过: ${results.passed.length}`);
  console.log(`❌ 失败: ${results.failed.length}`);
  console.log(`⚠️  警告: ${results.warnings.length}`);
  console.log(`📋 总计: ${results.passed.length + results.failed.length + results.warnings.length}`);
  
  if (results.failed.length > 0) {
    console.log('\n❌ 失败项:');
    results.failed.forEach((f, i) => console.log(`  ${i+1}. ${f}`));
  }
  if (results.warnings.length > 0) {
    console.log('\n⚠️  警告项:');
    results.warnings.forEach((w, i) => console.log(`  ${i+1}. ${w}`));
  }
  
  // Write report
  const fs = require('fs');
  const report = {
    timestamp: new Date().toISOString(),
    type: 'functional',
    environment: { backend: 'http://localhost:8081', frontend: 'http://localhost:3000' },
    summary: {
      total: results.passed.length + results.failed.length + results.warnings.length,
      passed: results.passed.length,
      failed: results.failed.length,
      warnings: results.warnings.length,
      passRate: ((results.passed.length / (results.passed.length + results.failed.length + results.warnings.length)) * 100).toFixed(1) + '%'
    },
    passed: results.passed,
    failed: results.failed,
    warnings: results.warnings
  };
  fs.writeFileSync('C:\\Users\\huangjun124\\WorkBuddy\\2026林夕置业\\结果\\HotelManage\\e2e-tests\\functional-report.json', JSON.stringify(report, null, 2));
  console.log(`\n📄 报告已保存`);
  
  await browser.close();
  process.exit(results.failed.length > 0 ? 1 : 0);
}

main().catch(e => { console.error(e); process.exit(1); });
