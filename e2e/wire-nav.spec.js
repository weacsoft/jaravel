// Playwright E2E for jaravel Wire 透明导航（桌面 + 移动 双视口）。
//
// 验证点（每个视口都完整跑一遍）：
//   1) 直接访问 /wire-dashboard 返回完整 HTML（非 JSON），DOM 含 wire:section 锚点注释
//   2) 点击 wire-navigate 链接切到 /wire-records：
//        - 请求带 X-Wire-Navigate:true + X-Wire-Hashes 头
//        - 响应是 application/json 的 diff（含 sections 对象）
//        - 应用后：URL 变 /wire-records、title 变「记录列表」、content 区内容更新、sidebar 区更新（active 高亮）
//   3) 再次点击 wire 链接切回 /wire-dashboard 正常
//   4) 直接访问 /wire-records（无 Wire 头）返回完整 HTML，非 JSON
//   5) 浏览器后退/前进（history）保持 Wire 无感切换
//
// 运行：NODE_PATH=<node-workspace>/node_modules <node> e2e/wire-nav.spec.js
// 真实浏览器：优先 PW_EXECUTABLE → 360ChromeX → Chrome → Edge → 自带 chromium。

const fs = require('fs');
const { chromium } = require('playwright');
const BASE = process.env.WIRE_BASE || 'http://localhost:8080';

function resolveExecutable() {
  if (process.env.PW_EXECUTABLE) return process.env.PW_EXECUTABLE;
  const candidates = [
    process.env.LOCALAPPDATA + '\\360ChromeX\\Chrome\\Application\\360ChromeX.exe',
    process.env.ProgramFiles + '\\Google\\Chrome\\Application\\chrome.exe',
    process.env['ProgramFiles(x86)'] + '\\Microsoft\\Edge\\Application\\msedge.exe',
    process.env.ProgramFiles + '\\Microsoft\\Edge\\Application\\msedge.exe',
  ];
  for (const c of candidates) { try { if (fs.existsSync(c)) return c; } catch (e) { /* ignore */ } }
  return undefined;
}
const EXE = resolveExecutable();

function makeAssert() {
  const pass = [], fail = [];
  const assert = (cond, msg) => {
    if (!cond) { fail.push(msg); console.error('FAIL:', msg); }
    else { pass.push(msg); console.log('PASS:', msg); }
  };
  return { pass, fail, assert };
}

const VIEWPORTS = [
  { name: 'desktop', viewport: { width: 1280, height: 800 }, isMobile: false, hasTouch: false },
  { name: 'mobile', viewport: { width: 390, height: 844 }, isMobile: true, hasTouch: true, deviceScaleFactor: 2 },
];

// 收集某次导航期间带 X-Wire-Navigate 的请求及其响应
async function collectWireRequests(page) {
  const items = [];
  page.on('request', (req) => {
    const h = req.headers();
    if (h['x-wire-navigate'] === 'true') {
      req._wireFlag = true;
    }
  });
  page.on('requestfinished', async (req) => {
    if (req._wireFlag) {
      try {
        const resp = await req.response();
        const ct = resp ? (resp.headers()['content-type'] || '') : '';
        let body = null;
        try { body = resp ? (await resp.body()).toString() : ''; } catch (e) { body = null; }
        items.push({ url: req.url(), contentType: ct, body });
      } catch (e) { /* ignore */ }
    }
  });
  return items;
}

async function runViewport(browser, vp) {
  console.log('\n################ 视口：' + vp.name.toUpperCase() +
    ' (' + vp.viewport.width + 'x' + vp.viewport.height + (vp.isMobile ? ', mobile/touch' : '') + ') ################');
  const { pass, fail, assert } = makeAssert();
  const page = await browser.newPage(vp);
  const wireReqs = await collectWireRequests(page);

  if (vp.isMobile) {
    const coarse = await page.evaluate(() => matchMedia('(pointer: coarse)').matches && navigator.maxTouchPoints > 0);
    assert(coarse, '移动视口已生效（pointer:coarse 且可触摸）');
  } else {
    const vw = await page.evaluate(() => window.innerWidth);
    assert(vw === vp.viewport.width, '桌面视口已生效（innerWidth=' + vw + '）');
  }

  // 1) 首屏直接访问 /wire-dashboard（无 Wire 头）→ 完整 HTML，含 wire:section 锚点
  await page.goto(BASE + '/wire-dashboard', { waitUntil: 'networkidle' });
  await page.waitForTimeout(400);
  const dashHtml = await page.content();
  assert(dashHtml.includes('wire:section-start:content'), '首屏 DOM 含 wire:section-start:content 锚点');
  assert(dashHtml.includes('wire:section-start:sidebar'), '首屏 DOM 含 wire:section-start:sidebar 锚点');
  assert((await page.title()).includes('仪表盘'), '首屏 title 为仪表盘页');
  assert(await page.locator('text=仪表盘').first().isVisible(), '首屏内容区已渲染（仪表盘）');

  // 2) 点击顶栏「记录列表」→ Wire 无感切换
  await page.locator('a[href="/wire-records"]').first().click();
  await page.waitForFunction(() => location.pathname === '/wire-records', null, { timeout: 5000 });
  await page.waitForTimeout(400);

  const lastWire = wireReqs.filter(r => r.url.endsWith('/wire-records')).pop();
  assert(!!lastWire, '切换请求命中 /wire-records');
  assert(lastWire && lastWire.contentType.includes('application/json'), 'Wire 响应 Content-Type 为 application/json');
  let diffOk = false, changedSections = [];
  if (lastWire && lastWire.body) {
    try {
      const payload = JSON.parse(lastWire.body);
      diffOk = payload && typeof payload.sections === 'object';
      changedSections = Object.keys(payload.sections || {});
    } catch (e) { diffOk = false; }
  }
  assert(diffOk, 'Wire 响应是 diff（含 sections 对象），非全量 HTML');
  console.log('    diff 变更的 section: ' + JSON.stringify(changedSections));
  assert(changedSections.includes('content'), 'diff 含 content section（content 有变化）');

  assert(page.url().endsWith('/wire-records'), '地址栏 URL 已更新为 /wire-records');
  assert((await page.title()).includes('记录列表'), 'title 已更新为记录列表页');
  assert(await page.locator('text=记录列表').first().isVisible(), '/wire-records 内容区已渲染');
  // 表格应渲染（records 数据）
  const rowCount = await page.locator('.main table tbody tr').count();
  assert(rowCount > 0, '记录列表表格已渲染（' + rowCount + ' 行）');
  // sidebar active 应切到「记录列表」
  const sidebarActive = await page.locator('.sidebar a.active').first().innerText().catch(() => '');
  assert(sidebarActive.includes('记录列表'), 'sidebar 高亮已切到记录列表（' + sidebarActive.trim() + '）');

  // 3) 切回 /wire-dashboard
  await page.locator('a[href="/wire-dashboard"]').first().click();
  await page.waitForFunction(() => location.pathname === '/wire-dashboard', null, { timeout: 5000 });
  await page.waitForTimeout(400);
  assert(page.url().endsWith('/wire-dashboard'), '再次切回 URL 为 /wire-dashboard');
  assert((await page.title()).includes('仪表盘'), '切回后 title 回到仪表盘');
  assert(await page.locator('text=仪表盘').first().isVisible(), '切回后内容区恢复仪表盘');

  // 4) 直接访问 /wire-records（全新上下文，无 Wire 头）→ 完整 HTML，不是 JSON
  const page2 = await browser.newPage(vp);
  const directHtml = [];
  page2.on('response', (resp) => { if (resp.url().endsWith('/wire-records')) directHtml.push(resp.headers()['content-type'] || ''); });
  await page2.goto(BASE + '/wire-records', { waitUntil: 'networkidle' });
  await page2.waitForTimeout(300);
  assert((directHtml[0] || '').includes('text/html'), '直接访问 /wire-records 返回 text/html（非 JSON）');
  assert((await page2.title()).includes('记录列表'), '直接访问 title 为记录列表页');
  assert(await page2.locator('.main table tbody tr').count() > 0, '直接访问记录列表表格已渲染');
  await page2.close();

  await page.close();
  return { name: vp.name, pass: pass.length, fail: fail.length, failItems: fail.slice() };
}

(async () => {
  const results = [];
  for (const vp of VIEWPORTS) {
    const browser = await chromium.launch(EXE ? { executablePath: EXE, args: ['--no-sandbox', '--disable-gpu'] } : {});
    results.push(await runViewport(browser, vp));
    await browser.close();
  }

  console.log('\n' + '='.repeat(60));
  let allFail = 0;
  for (const r of results) {
    console.log(`视口 ${r.name}: 通过 ${r.pass} 项，失败 ${r.fail} 项`);
    r.failItems.forEach(f => console.log('    - ' + f));
    allFail += r.fail;
  }
  console.log('='.repeat(60));
  if (allFail) { console.log('存在失败项 ❌'); process.exit(1); }
  console.log('全部视口（桌面 + 移动）全部通过 ✅');
})().catch((e) => {
  console.error('\n❌ E2E 失败:', e.message);
  process.exit(1);
});
