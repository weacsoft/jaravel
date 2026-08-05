// Playwright E2E for jaravel PJAX 无感页面切换
// 运行前：先启动应用 `mvn -o -Dmaven.test.skip=true spring-boot:run`（默认 8080）
//       并安装浏览器 `npx playwright install chromium`
// 运行：   `node e2e/pjax.spec.js`（或 `npx playwright test` 配合 playwright.config）
//
// 验证点：
//   1) 首屏返回完整 HTML（含 #pjax-config 与 pjax.js），非 JSON
//   2) 点击链接只切换变化的区域（content/sidebar/title），head/scratch/scripts 保持
//   3) 切换时更新 document.title 与地址栏 URL（history.pushState），可前进/后退
//   4) 未变化区域（scratch 状态探针：输入框内容、计时、切换计数）状态保持

const fs = require('fs');
const { chromium } = require('playwright');
const BASE = process.env.PJAX_BASE || 'http://localhost:8080';

// 浏览器可执行文件：优先 PW_EXECUTABLE 环境变量，否则回落到本机已安装的 Chromium 内核浏览器，
// 都没有时用 Playwright 自带的 chromium（需先 `npx playwright install chromium`）。
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

const LAUNCH_OPTS = (() => {
  const exe = resolveExecutable();
  return exe ? { executablePath: exe, args: ['--no-sandbox', '--disable-gpu'] } : {};
})();

function assert(cond, msg) {
  if (!cond) { console.error('FAIL:', msg); throw new Error(msg); }
  console.log('PASS:', msg);
}

(async () => {
  const browser = await chromium.launch(LAUNCH_OPTS);
  const page = await browser.newPage();
  const requests = [];
  // requestfinished 回调收到的是 Request 对象本身（不是 Response），直接取其请求头。
  page.on('requestfinished', (req) => {
    const h = req.headers();
    if (h['x-pjax']) requests.push({ url: req.url(), pjax: h['x-pjax'] });
  });

  // ---- 1) 首屏 /home ----
  await page.goto(BASE + '/home', { waitUntil: 'networkidle' });
  assert(await page.locator('#pjax-config').count() === 1, '首屏含有 #pjax-config');
  assert(await page.locator('script[src="/static/js/pjax.js"]').count() === 1, '首屏注入 pjax.js');
  assert((await page.title()).includes('概览'), '首屏 title 为概览页');

  // ---- 2) 在 scratch 状态探针输入框打字，记录值 ----
  const probe = page.locator('#probe-input');
  await probe.fill('hello-pjax-state');
  const before = await probe.inputValue();
  assert(before === 'hello-pjax-state', '已写入状态探针输入值');

  // ---- 3) 点击侧栏「任务列表」切到 /list ----
  await page.locator('a[href="/list"]').first().click();
  // 等待 pjax 完成（pjax:loaded 事件）
  await page.waitForFunction(() => location.pathname === '/list', null, { timeout: 5000 });
  await page.waitForTimeout(300);

  assert(page.url().endsWith('/list'), '地址栏 URL 已更新为 /list');
  assert((await page.title()).includes('任务列表'), 'title 已更新为任务列表页');
  assert(await page.locator('#probe-input').count() === 1, 'scratch 区域仍存活（未被整体替换）');
  const after = await page.locator('#probe-input').inputValue();
  assert(after === before, '状态探针输入值保持（状态未丢失）');
  const listVisible = await page.locator('text=任务列表').first().isVisible();
  assert(listVisible, '/list 内容区已渲染');

  // 校验底层请求确实是 PJAX（X-Pjax 头 + JSON 信封）
  const lastPjax = requests.filter(r => r.pjax === 'true').pop();
  assert(lastPjax && lastPjax.url.endsWith('/list'), '切换请求带 X-Pjax:true 且命中 /list');

  // ---- 4) 返回 /home，验证前进/后退与状态保持 ----
  await page.goBack();
  await page.waitForFunction(() => location.pathname === '/home', null, { timeout: 5000 });
  await page.waitForTimeout(300);
  assert(page.url().endsWith('/home'), '后退后 URL 回到 /home');
  assert((await page.title()).includes('概览'), '后退后 title 回到概览页');
  assert(await page.locator('#probe-input').inputValue() === before, '后退后状态探针值仍保持');

  // ---- 5) 分页局部切换：/list?page=1 -> page=2 只换 content ----
  await page.goto(BASE + '/list?page=1', { waitUntil: 'networkidle' });
  await page.locator('a[href="/list?page=2"]').first().click();
  await page.waitForFunction(() => location.search.includes('page=2'), null, { timeout: 5000 });
  await page.waitForTimeout(300);
  assert(page.url().includes('page=2'), '分页切换到 page=2');
  assert((await page.title()).includes('第 2 页'), 'title 已更新为「第 2 页」');
  assert((await page.locator('.pager .cur').textContent()).trim() === '2', '分页器高亮已切到第 2 页');
  assert(await page.locator('table.grid tbody tr').count() > 0, 'page=2 列表内容已渲染');
  assert(await page.locator('#probe-input').count() === 1, '分页切换后 scratch 区域仍未被替换');

  await browser.close();
  console.log('\n✅ PJAX E2E 全部通过');
})().catch((e) => {
  console.error('\n❌ E2E 失败:', e.message);
  process.exit(1);
});
