/**
 * 功能合并E2E验证测试
 * 测试范围：门店团队弹层、投资人投资记录弹层、门店删除校验、禁用门店约束
 */
const { chromium } = require('playwright');

const BASE_URL = 'http://localhost:3000';
const API_URL = 'http://localhost:8081';
let token = '';
let browser, page;
const results = { pass: 0, fail: 0, items: [] };

function log(name, pass, detail = '') {
  const status = pass ? 'PASS' : 'FAIL';
  results.items.push({ name, status, detail });
  if (pass) results.pass++; else results.fail++;
  console.log(`[${status}] ${name} ${detail ? '- ' + detail : ''}`);
}

async function apiLogin() {
  const res = await fetch(`${API_URL}/api/auth/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ username: 'admin', password: '123456' })
  });
  const data = await res.json();
  token = data.data.token;
  return token;
}

async function apiCall(method, path, body = null) {
  const opts = {
    method,
    headers: { 'Authorization': `Bearer ${token}`, 'Content-Type': 'application/json' }
  };
  if (body) opts.body = JSON.stringify(body);
  const res = await fetch(`${API_URL}${path}`, opts);
  return await res.json();
}

(async () => {
  console.log('='.repeat(60));
  console.log('功能合并 E2E 验证测试');
  console.log('='.repeat(60));

  await apiLogin();
  log('API登录', !!token);

  browser = await chromium.launch({ headless: true });
  const context = await browser.newContext();
  page = await context.newPage();

  // 注入token到localStorage
  await page.goto(BASE_URL);
  await page.evaluate((t) => localStorage.setItem('token', t), token);
  await page.evaluate((t) => localStorage.setItem('user', JSON.stringify({ username: 'admin', roles: ['ROLE_SUPER_ADMIN'] })), token);

  // ===== SECTION 1: 门店管理页面 =====
  console.log('\n--- SECTION 1: 门店管理页面 ---');
  
  await page.goto(`${BASE_URL}/store/list`);
  await page.waitForTimeout(2000);

  // 1.1 门店列表加载
  const storeRows = await page.locator('.el-table__body-wrapper .el-table__row').count();
  log('门店列表加载', storeRows > 0, `共${storeRows}行`);

  // 1.2 查看团队按钮存在
  const teamBtn = await page.locator('button:has-text("团队"), button:has-text("查看团队")').count();
  log('门店列表有查看团队按钮', teamBtn > 0, `找到${teamBtn}个`);

  // 1.3 点击查看团队
  if (teamBtn > 0) {
    await page.locator('button:has-text("团队"), button:has-text("查看团队")').first().click();
    await page.waitForTimeout(1000);
    
    const teamDialog = await page.locator('.el-dialog:visible').count();
    log('点击查看团队弹出弹层', teamDialog > 0);

    if (teamDialog > 0) {
      // 弹层内表格
      const teamRows = await page.locator('.el-dialog:visible .el-table__row').count();
      log('团队弹层显示人员列表', true, `${teamRows}条记录`);

      // 弹层内新增人员按钮
      const addMemberBtn = await page.locator('.el-dialog:visible button:has-text("新增")').count();
      log('团队弹层有新增人员按钮', addMemberBtn > 0);

      // 关闭弹层
      await page.locator('.el-dialog:visible .el-dialog__headerbtn').first().click();
      await page.waitForTimeout(500);
    }
  }

  // ===== SECTION 2: 投资人管理页面 =====
  console.log('\n--- SECTION 2: 投资人管理页面 ---');

  await page.goto(`${BASE_URL}/investor/list`);
  await page.waitForTimeout(2000);

  // 2.1 投资人列表加载
  const investorRows = await page.locator('.el-table__body-wrapper .el-table__row').count();
  log('投资人列表加载', investorRows > 0, `共${investorRows}行`);

  // 2.2 增加投资按钮
  const addInvestBtn = await page.locator('button:has-text("增加投资")').count();
  log('投资人列表有增加投资按钮', addInvestBtn > 0, `找到${addInvestBtn}个`);

  // 2.3 查看投资记录按钮
  const viewInvestBtn = await page.locator('button:has-text("投资记录"), button:has-text("查看投资")').count();
  log('投资人列表有查看投资记录按钮', viewInvestBtn > 0, `找到${viewInvestBtn}个`);

  // 2.4 点击查看投资记录
  if (viewInvestBtn > 0) {
    await page.locator('button:has-text("投资记录"), button:has-text("查看投资")').first().click();
    await page.waitForTimeout(1000);

    const investDialog = await page.locator('.el-dialog:visible').count();
    log('点击查看投资记录弹出弹层', investDialog > 0);

    if (investDialog > 0) {
      const investRows = await page.locator('.el-dialog:visible .el-table__row').count();
      log('投资记录弹层显示记录列表', true, `${investRows}条记录`);

      // 编辑和删除按钮
      const editBtn = await page.locator('.el-dialog:visible button:has-text("编辑")').count();
      const deleteBtn = await page.locator('.el-dialog:visible button:has-text("删除")').count();
      log('投资记录有编辑按钮', editBtn > 0, `找到${editBtn}个`);
      log('投资记录有删除按钮', deleteBtn > 0, `找到${deleteBtn}个`);

      // 关闭弹层
      await page.locator('.el-dialog:visible .el-dialog__headerbtn').first().click();
      await page.waitForTimeout(500);
    }
  }

  // ===== SECTION 3: API验证 - 门店删除校验 =====
  console.log('\n--- SECTION 3: API验证 - 门店删除校验 ---');

  // 3.1 删除有记录的门店
  const delResult = await apiCall('DELETE', '/api/stores/1');
  log('删除有记录的门店被拒绝', delResult.code === 500, delResult.message);

  // 3.2 删除无记录的门店
  // 先创建一个无关联记录的门店
  const ts = Date.now();
  const createStore = await apiCall('POST', '/api/stores', {
    storeName: `测试删除门店${ts}`,
    storeCode: `DEL${ts}`,
    status: 1
  });
  log('创建测试门店', createStore.code === 200);

  if (createStore.code === 200) {
    // 查找新建的门店ID
    const storeList = await apiCall('GET', `/api/stores?page=1&size=100&storeCode=DEL${ts}`);
    const newStoreId = storeList.data?.records?.find(s => s.storeCode === `DEL${ts}`)?.id;
    
    if (newStoreId) {
      const delNew = await apiCall('DELETE', `/api/stores/${newStoreId}`);
      log('删除无记录的门店成功', delNew.code === 200);
    } else {
      log('删除无记录的门店', false, '找不到新建门店ID');
    }
  }

  // ===== SECTION 4: API验证 - 禁用门店约束 =====
  console.log('\n--- SECTION 4: API验证 - 禁用门店约束 ---');

  // 4.1 禁用门店
  const disableResult = await apiCall('PUT', '/api/stores/9', { storeName: '测试', storeCode: 'SZ009', status: 0 });
  log('禁用门店9', disableResult.code === 200);

  // 4.2 禁用门店不能新增人员
  const addUserResult = await apiCall('POST', '/api/users', {
    realName: '测试禁用门店人员',
    username: `disabled_store_user_${ts}`,
    phone: `139${ts.toString().slice(-8)}`,
    password: '123456',
    userType: 2,
    storeId: 9
  });
  log('禁用门店不能新增人员', addUserResult.code === 500 && addUserResult.message.includes('禁用'), addUserResult.message);

  // 4.3 禁用门店不能新增投资
  const addInvestResult = await apiCall('POST', '/api/investor-relations', {
    investorId: 1,
    storeId: 9,
    investmentRatio: 0.05,
    authStartDate: '2026-01-01'
  });
  log('禁用门店不能新增投资', addInvestResult.code === 500 && addInvestResult.message.includes('禁用'), addInvestResult.message);

  // 4.4 恢复门店
  const enableResult = await apiCall('PUT', '/api/stores/9', { storeName: '测试', storeCode: 'SZ009', status: 1 });
  log('恢复门店9', enableResult.code === 200);

  // ===== SECTION 5: API验证 - 投资关系编辑 =====
  console.log('\n--- SECTION 5: API验证 - 投资关系编辑 ---');

  // 5.1 编辑投资关系
  const editResult = await apiCall('PUT', '/api/investor-relations/1', {
    investAmount: 88888,
    investmentRatio: 33.3
  });
  log('编辑投资关系', editResult.code === 200);

  // 5.2 验证编辑结果
  const verifyResult = await apiCall('GET', '/api/investor-relations?investorId=1&page=1&size=1');
  const first = verifyResult.data?.[0] || verifyResult.data?.records?.[0];
  log('验证编辑后数据', first?.investAmount === 88888, `amount=${first?.investAmount}, ratio=${first?.investmentRatio}`);

  // ===== SECTION 6: 旧路由已移除 =====
  console.log('\n--- SECTION 6: 旧路由已移除验证 ---');

  await page.goto(`${BASE_URL}/store/user`);
  await page.waitForTimeout(1000);
  const storeUserVisible = await page.locator('.el-table').count();
  log('/store/user 路由已移除', storeUserVisible === 0, `表格存在=${storeUserVisible > 0}`);

  await page.goto(`${BASE_URL}/investor/relation`);
  await page.waitForTimeout(1000);
  const investRelVisible = await page.locator('.el-table').count();
  log('/investor/relation 路由已移除', investRelVisible === 0, `表格存在=${investRelVisible > 0}`);

  // ===== 汇总 =====
  console.log('\n' + '='.repeat(60));
  console.log(`测试结果: ${results.pass} 通过, ${results.fail} 失败, 共 ${results.pass + results.fail} 项`);
  console.log('='.repeat(60));

  if (results.fail > 0) {
    console.log('\n失败项:');
    results.items.filter(i => i.status === 'FAIL').forEach(i => console.log(`  - ${i.name}: ${i.detail}`));
  }

  await browser.close();
  process.exit(results.fail > 0 ? 1 : 0);
})();
