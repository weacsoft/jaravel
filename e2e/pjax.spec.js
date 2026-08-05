// Playwright E2E for jaravel PJAX 无感页面切换（桌面 + 移动 双视口）。
//
// 验证点（每个视口都完整跑一遍）：
//   1) 首屏返回完整 HTML（含 #pjax-config 与 pjax.js），非 JSON
//   2) 点击链接只切换变化的区域（content/sidebar/title），head/scratch/scripts 保持
//   3) 切换时更新 document.title 与地址栏 URL（history.pushState），可前进/后退
//   4) 未变化区域（scratch 状态探针：输入框内容、计数）状态保持
//
// 运行：NODE_PATH=<node-workspace>/node_modules <node> e2e/pjax.spec.js
// 真实浏览器：优先 PW_EXECUTABLE → 360ChromeX → Chrome → Edge → 自带 chromium。

const fs = require('fs');
const { chromium } = require('playwright');
const BASE = process.env.PJAX_BASE || 'http://localhost:8080';

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

// 桌面 + 移动 双视口；移动端开启触摸与移动仿真。
const VIEWPORTS = [
  { name: 'desktop', viewport: { width: 1280, height: 800 }, isMobile: false, hasTouch: false },
  { name: 'mobile', viewport: { width: 390, height: 844 }, isMobile: true, hasTouch: true, deviceScaleFactor: 2 },
];

async function runViewport(browser, vp) {
  console.log('\n################ 视口：' + vp.name.toUpperCase() +
    ' (' + vp.viewport.width + 'x' + vp.viewport.height + (vp.isMobile ? ', mobile/touch' : '') + ') ################');
  const { pass, fail, assert } = makeAssert();
  const page = await browser.newPage(vp);
  const requests = [];
  page.on('requestfinished', (req) => {
    const h = req.headers();
    if (h['x-pjax']) requests.push({ url: req.url(), pjax: h['x-pjax'] });
  });

  // 视口生效校验：桌面用 innerWidth，移动用「触摸/粗指针」媒体特性
  // （360ChromeX 的 isMobile 上下文 innerWidth 仍为 980，不能用像素宽度判定）。
  if (vp.isMobile) {
    const coarse = await page.evaluate(() => matchMedia('(pointer: coarse)').matches && navigator.maxTouchPoints > 0);
    assert(coarse, '移动视口已生效（pointer:coarse 且可触摸）');
  } else {
    const vw = await page.evaluate(() => window.innerWidth);
    assert(vw === vp.viewport.width, '桌面视口已生效（innerWidth=' + vw + '）');
  }

  // 1) 首屏 /home
  await page.goto(BASE + '/home', { waitUntil: 'networkidle' });
  assert(await page.locator('#pjax-config').count() === 1, '首屏含有 #pjax-config');
  assert(await page.locator('script[src="/static/js/pjax.js"]').count() === 1, '首屏注入 pjax.js');
  assert((await page.title()).includes('概览'), '首屏 title 为概览页');

  // 2) 状态探针
  const probe = page.locator('#probe-input');
  await probe.fill('hello-pjax-state');
  const before = await probe.inputValue();
  assert(before === 'hello-pjax-state', '已写入状态探针输入值');

  // 3) 点击侧栏「任务列表」切到 /list
  await page.locator('a[href="/list"]').first().click();
  await page.waitForFunction(() => location.pathname === '/list', null, { timeout: 5000 });
  await page.waitForTimeout(300);
  assert(page.url().endsWith('/list'), '地址栏 URL 已更新为 /list');
  assert((await page.title()).includes('任务列表'), 'title 已更新为任务列表页');
  assert(await page.locator('#probe-input').count() === 1, 'scratch 区域仍存活（未被整体替换）');
  const after = await page.locator('#probe-input').inputValue();
  assert(after === before, '状态探针输入值保持（状态未丢失）');
  assert(await page.locator('text=任务列表').first().isVisible(), '/list 内容区已渲染');
  const lastPjax = requests.filter(r => r.pjax === 'true').pop();
  assert(lastPjax && lastPjax.url.endsWith('/list'), '切换请求带 X-Pjax:true 且命中 /list');

  // 4) 返回 /home
  await page.goBack();
  await page.waitForFunction(() => location.pathname === '/home', null, { timeout: 5000 });
  await page.waitForTimeout(300);
  assert(page.url().endsWith('/home'), '后退后 URL 回到 /home');
  assert((await page.title()).includes('概览'), '后退后 title 回到概览页');
  assert(await page.locator('#probe-input').inputValue() === before, '后退后状态探针值仍保持');

  // 5) 分页局部切换
  await page.goto(BASE + '/list?page=1', { waitUntil: 'networkidle' });
  await page.locator('a[href="/list?page=2"]').first().click();
  await page.waitForFunction(() => location.search.includes('page=2'), null, { timeout: 5000 });
  await page.waitForTimeout(300);
  assert(page.url().includes('page=2'), '分页切换到 page=2');
  assert((await page.title()).includes('第 2 页'), 'title 已更新为「第 2 页」');
  assert((await page.locator('.pager .cur').textContent()).trim() === '2', '分页器高亮已切到第 2 页');
  assert(await page.locator('table.grid tbody tr').count() > 0, 'page=2 列表内容已渲染');
  assert(await page.locator('#probe-input').count() === 1, '分页切换后 scratch 区域仍未被替换');

  await page.close();
  return { name: vp.name, pass: pass.length, fail: fail.length, failItems: fail.slice() };
}

(async () => {
  // 每个视口各自启动一个浏览器实例（360ChromeX 不允许单会话开第二个 tab）。
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
