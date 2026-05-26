const http = require('http');
const https = require('https');

const LOCAL = 'http://localhost:8081';
const PROD = 'http://82.156.225.171';

let localToken = '';
let prodToken = '';
let results = [];

function request(url, method, path, body, token) {
  return new Promise((resolve, reject) => {
    const u = new URL(url + path);
    const opts = {
      hostname: u.hostname,
      port: u.port || 80,
      path: u.pathname + u.search,
      method: method,
      headers: { 'Content-Type': 'application/json' }
    };
    if (token) opts.headers['Authorization'] = 'Bearer ' + token;
    const req = http.request(opts, (res) => {
      let data = '';
      res.on('data', c => data += c);
      res.on('end', () => {
        try { resolve({ status: res.statusCode, body: JSON.parse(data) }); }
        catch(e) { resolve({ status: res.statusCode, body: data }); }
      });
    });
    req.on('error', reject);
    if (body) req.write(JSON.stringify(body));
    req.end();
  });
}

async function login(env, url) {
  const r = await request(url, 'POST', '/api/auth/login', { username: 'admin', password: '123456' });
  if (r.body.code === 200 && r.body.data.token) {
    console.log(`[${env}] ✓ Login OK, userId=${r.body.data.userId}, roles=${r.body.data.roles.join(',')}`);
    return r.body.data.token;
  } else {
    console.log(`[${env}] ✗ Login FAILED:`, r.body);
    return null;
  }
}

async function compareAPI(name, method, path, body) {
  try {
    const lr = await request(LOCAL, method, path, body, localToken);
    const pr = await request(PROD, method, path, body, prodToken);
    
    const match = JSON.stringify(lr.body) === JSON.stringify(pr.body);
    const statusMatch = lr.status === pr.status;
    
    if (match && statusMatch) {
      results.push({ name, status: 'PASS', local: lr.status, prod: pr.status });
      console.log(`✓ ${name}: PASS (HTTP ${lr.status})`);
    } else {
      results.push({ name, status: 'DIFF', local: lr.status, prod: pr.status, diff: true });
      console.log(`✗ ${name}: DIFF (Local=${lr.status}, Prod=${pr.status})`);
      if (!match) {
        console.log(`  Local:`, JSON.stringify(lr.body).substring(0, 100));
        console.log(`  Prod:`, JSON.stringify(pr.body).substring(0, 100));
      }
    }
  } catch(e) {
    results.push({ name, status: 'ERROR', error: e.message });
    console.log(`✗ ${name}: ERROR - ${e.message}`);
  }
}

async function run() {
  console.log('=== 林夕置业 HotelManage 环境对照测试 ===\n');
  console.log('本地:', LOCAL);
  console.log('生产:', PROD);
  console.log('');
  
  // 1. 登录
  console.log('--- 1. 认证 ---');
  localToken = await login('LOCAL', LOCAL);
  prodToken = await login('PROD', PROD);
  if (!localToken || !prodToken) {
    console.log('\n登录失败，终止测试');
    process.exit(1);
  }
  
  // 2. 用户管理
  console.log('\n--- 2. 用户管理 ---');
  await compareAPI('用户列表', 'GET', '/api/users');
  await compareAPI('用户详情(admin)', 'GET', '/api/users/1');
  await compareAPI('角色列表', 'GET', '/api/users/roles');
  
  // 3. 门店管理
  console.log('\n--- 3. 门店管理 ---');
  await compareAPI('门店列表', 'GET', '/api/stores');
  await compareAPI('门店详情(1)', 'GET', '/api/stores/1');
  
  // 4. 投资人管理
  console.log('\n--- 4. 投资人管理 ---');
  await compareAPI('投资人列表', 'GET', '/api/investors');
  await compareAPI('投资人详情(1)', 'GET', '/api/investors/1');
  await compareAPI('投资人门店关系', 'GET', '/api/investors/1/stores');
  
  // 5. 日报管理
  console.log('\n--- 5. 日报管理 ---');
  await compareAPI('日报列表', 'GET', '/api/daily-reports?page=1&size=10');
  await compareAPI('日报汇总', 'GET', '/api/daily-reports/summary?storeId=1&startDate=2026-05-01&endDate=2026-05-10');
  await compareAPI('日报模板', 'GET', '/api/daily-report-templates');
  
  // 6. 报表分析
  console.log('\n--- 6. 报表分析 ---');
  await compareAPI('经营仪表盘', 'GET', '/api/analysis/dashboard?startDate=2026-05-01&endDate=2026-05-10');
  await compareAPI('日报分析', 'GET', '/api/analysis/daily?storeId=1&startDate=2026-05-01&endDate=2026-05-10');
  await compareAPI('月度分析', 'GET', '/api/analysis/monthly?storeId=1&year=2026&month=5');
  await compareAPI('渠道分析', 'GET', '/api/analysis/channel?storeId=1&startDate=2026-05-01&endDate=2026-05-10');
  await compareAPI('排名分析', 'GET', '/api/analysis/ranking?startDate=2026-05-01&endDate=2026-05-10');
  await compareAPI('趋势分析', 'GET', '/api/analysis/trend?storeId=1&startDate=2026-05-01&endDate=2026-05-10');
  
  // 7. 系统配置
  console.log('\n--- 7. 系统配置 ---');
  await compareAPI('系统配置', 'GET', '/api/sys-configs');
  await compareAPI('操作日志', 'GET', '/api/operation-logs?page=1&size=10');
  
  // 8. 菜单权限
  console.log('\n--- 8. 菜单权限 ---');
  await compareAPI('菜单列表', 'GET', '/api/auth/menus');
  await compareAPI('用户信息', 'GET', '/api/auth/userinfo');
  
  // 汇总
  console.log('\n=== 测试汇总 ===');
  const pass = results.filter(r => r.status === 'PASS').length;
  const diff = results.filter(r => r.status === 'DIFF').length;
  const err = results.filter(r => r.status === 'ERROR').length;
  console.log(`总计: ${results.length} | 通过: ${pass} | 差异: ${diff} | 错误: ${err}`);
  console.log(`通过率: ${(pass/results.length*100).toFixed(1)}%`);
  
  if (diff > 0) {
    console.log('\n差异项:');
    results.filter(r => r.status === 'DIFF').forEach(r => {
      console.log(`  - ${r.name}: Local=${r.local}, Prod=${r.prod}`);
    });
  }
  if (err > 0) {
    console.log('\n错误项:');
    results.filter(r => r.status === 'ERROR').forEach(r => {
      console.log(`  - ${r.name}: ${r.error}`);
    });
  }
  
  console.log('\n测试完成');
}

run().catch(e => {
  console.error('测试执行失败:', e);
  process.exit(1);
});
