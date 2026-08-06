// Playwright E2E：PJAX 区域脚本运行时（Region Script Runtime）· 桌面 + 移动 双视口。
//
// 背景：PJAX 只替换变化区域。新片段里「进入即触发」的内联脚本必须被重新解析执行，
// 但历史实现同时用 createContextualFragment（插入即执行）和 runScripts（再执行一次），
// 导致每次切换双执行；且旧区域脚本注册在 document/window 上的监听器与定时器从不回收，
// 来回切换会线性叠加；DOMContentLoaded 之类的就绪回调则永远不再触发。
//
// 本套件针对修复后的运行时逐条验证：
//   1) 首屏内联脚本恰好执行 1 次（无双执行）
//   2) 切换后新区域脚本恰好执行 1 次；旧页脚本不被重复触发
//   3) document-ready 类回调每次切换回放恰好 1 次
//   4) 旧区域注册的 document 监听器被回收 —— 一次点击只计 1 次
//   5) 旧区域的 setInterval 被清除 —— 心跳停止
//   6) <script src> 跨切换不重复加载
//   7) data-pjax-run="once" 全会话只执行 1 次（含首屏预热）
//   8) 顶层 function/var 回填 window —— onclick="rtHello()" 仍可用
//   9) 未变化区域（scripts）的监听器不被误回收 —— pjax:loaded 计数正常累加
//  10) 全程无 pageerror（重复声明会以 SyntaxError 静默吃掉整段脚本）
//
// 运行：NODE_PATH=<node-workspace>/node_modules <node> e2e/pjax-script-runtime.spec.js

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
  const assert = (cond, msg, extra) => {
    const line = msg + (extra === undefined ? '' : '  -> ' + JSON.stringify(extra));
    if (!cond) { fail.push(line); console.error('  [FAIL] ' + line); }
    else { pass.push(line); console.log('  [OK]   ' + line); }
  };
  return { pass, fail, assert };
}

const VIEWPORTS = [
  { name: 'desktop', viewport: { width: 1280, height: 800 }, isMobile: false, hasTouch: false },
  { name: 'mobile', viewport: { width: 390, height: 844 }, isMobile: true, hasTouch: true, deviceScaleFactor: 2 },
];

const snap = (page) => page.evaluate(() => {
  const rt = window.__rt || {};
  const stats = (window.Pjax && window.Pjax.stats) ? window.Pjax.stats() : {};
  return {
    exec: Object.assign({}, rt.exec),
    tick: Object.assign({}, rt.tick),
    ready: rt.ready || 0,
    readyLog: (rt.readyLog || []).slice(),
    once: rt.once || 0,
    ext: rt.ext || 0,
    docClicks: rt.docClicks || 0,
    helloCalls: rt.helloCalls || 0,
    helloIsFn: typeof window.rtHello === 'function',
    probeCount: document.querySelectorAll('#rt-probe').length,
    extScriptTags: document.querySelectorAll('script[src*="pjax-probe-ext.js"]').length,
    extSkipped: document.querySelectorAll('script[src*="pjax-probe-ext.js"][data-pjax-skipped]').length,
    onceSkipped: document.querySelectorAll('script[data-pjax-skipped="once"]').length,
    visits: (document.getElementById('probe-visits') || {}).textContent || '',
    stats,
  };
});

// 点击页面空白处（避开按钮/链接），只用于触发 document 上的 click 委托计数
async function clickBlank(page) {
  await page.evaluate(() => {
    const t = document.querySelector('.mdui-toolbar-spacer') || document.body;
    t.dispatchEvent(new MouseEvent('click', { bubbles: true }));
  });
  await page.waitForTimeout(80);
}

// 探针面板每 100ms 重绘一次（心跳），真实鼠标点击会被布局抖动判定为「被遮挡」。
// 这里用元素自身的 click()：同样会触发内联 onclick 属性并冒泡到 document，
// 正是本用例要验证的两条链路。
async function clickButton(page, selector) {
  await page.locator(selector).evaluate((el) => el.click());
  await page.waitForTimeout(120);
}

async function runViewport(browser, vp) {
  console.log('\n################ 视口：' + vp.name.toUpperCase() +
    ' (' + vp.viewport.width + 'x' + vp.viewport.height + (vp.isMobile ? ', mobile/touch' : '') + ') ################');
  const { pass, fail, assert } = makeAssert();
  const page = await browser.newPage(vp);
  const pageErrors = [];
  page.on('pageerror', (e) => pageErrors.push(String(e && e.message ? e.message : e)));

  // ---------- 1. 首屏 /home ----------
  console.log('\n=== 1. 首屏 /home：进入即触发脚本只跑一次 ===');
  await page.goto(BASE + '/home', { waitUntil: 'networkidle' });
  await page.waitForFunction(() => !!window.__rt && !!window.__rt.exec, null, { timeout: 5000 });
  let s = await snap(page);
  assert(s.exec.home === 1, '首屏 home 内联脚本执行 1 次（无双执行）', s.exec);
  assert(s.ready === 1, '首屏 document-ready 回调触发 1 次', s.ready);
  assert(s.once === 1, 'once 脚本首屏执行 1 次', s.once);
  assert(s.ext === 1, '外部脚本首屏加载 1 次', s.ext);
  assert(s.probeCount === 1, '探针面板唯一', s.probeCount);
  assert(s.helloIsFn, '顶层函数 rtHello 已回填到 window');

  await clickButton(page, '#rt-hello-btn');
  s = await snap(page);
  assert(s.helloCalls === 1, 'onclick="rtHello()" 可正常调用', s.helloCalls);
  assert((await page.locator('#rt-hello').textContent()).includes('top-level-var'), '顶层 var 也已回填（文案含 top-level-var）');
  assert(s.docClicks === 1, '首屏 document click 监听命中 1 次（仅 1 份监听器）', s.docClicks);

  const listenersAfterFirst = s.stats.listeners;

  // ---------- 2. /home -> /list ----------
  console.log('\n=== 2. 切换 /home -> /list：新区域脚本执行、旧区域资源回收 ===');
  const tickHomeBefore = (await snap(page)).tick.home;
  await page.locator('a[href="/list"]').first().click();
  await page.waitForFunction(() => location.pathname === '/list', null, { timeout: 5000 });
  await page.waitForFunction(() => window.__rt && window.__rt.exec && window.__rt.exec.list >= 1, null, { timeout: 5000 });
  await page.waitForTimeout(400);
  s = await snap(page);
  assert(s.exec.list === 1, '/list 内联脚本执行 1 次（无双执行）', s.exec);
  assert(s.exec.home === 1, '/home 脚本未被重复触发', s.exec);
  assert(s.ready === 2, 'document-ready 在本次切换后回放 1 次（累计 2）', { ready: s.ready, log: s.readyLog });
  assert(s.readyLog.join(',') === 'home,list', 'ready 回放顺序正确', s.readyLog);
  assert(s.once === 1, 'once 脚本未再执行', s.once);
  assert(s.ext === 1, '外部脚本未被重复加载', s.ext);
  assert(s.extScriptTags === 1, '文档中外部脚本标签唯一（旧标签随区域移除）', s.extScriptTags);
  assert(s.extSkipped === 1, '重复外部脚本被中和为不可执行（data-pjax-skipped=duplicate-source）', s.extSkipped);
  assert(s.onceSkipped === 1, 'once 脚本被中和为不可执行（data-pjax-skipped=once）', s.onceSkipped);
  assert(s.probeCount === 1, '切换后探针面板仍唯一（未叠加）', s.probeCount);

  // 旧页定时器必须已停
  const tickHomeMid = s.tick.home;
  await page.waitForTimeout(500);
  const s2 = await snap(page);
  assert(s2.tick.home === tickHomeMid, '/home 的 setInterval 已被回收（心跳停止）', { before: tickHomeBefore, mid: tickHomeMid, after: s2.tick.home });
  assert(s2.tick.list > 0, '/list 的 setInterval 正常运行', s2.tick.list);

  // 旧页 document 监听器必须已回收：一次点击只 +1
  const clicksBefore = s2.docClicks;
  await clickBlank(page);
  let s3 = await snap(page);
  assert(s3.docClicks === clicksBefore + 1, '一次点击只计 1 次（旧区域 document 监听器已回收）', { before: clicksBefore, after: s3.docClicks });
  assert(s3.visits === '1', '未变化的 scripts 区域监听器未被误回收（pjax:loaded 计数=1）', s3.visits);

  // ---------- 3. /list -> /home（回到已访问过的页面） ----------
  console.log('\n=== 3. 切回 /home：重复进入不应产生重复声明或监听叠加 ===');
  const tickListBefore = s3.tick.list;
  await page.locator('a[href="/home"]').first().click();
  await page.waitForFunction(() => location.pathname === '/home', null, { timeout: 5000 });
  await page.waitForFunction(() => window.__rt && window.__rt.exec && window.__rt.exec.home >= 2, null, { timeout: 5000 });
  await page.waitForTimeout(400);
  s = await snap(page);
  assert(s.exec.home === 2, '重新进入 /home 时脚本重跑恰好 1 次（累计 2）', s.exec);
  assert(s.exec.list === 1, '/list 脚本未被重复触发', s.exec);
  assert(s.ready === 3, 'document-ready 再回放 1 次（累计 3）', { ready: s.ready, log: s.readyLog });
  assert(s.once === 1, 'once 脚本全程仅执行 1 次', s.once);
  assert(s.ext === 1, '外部脚本全程仅加载 1 次', s.ext);
  assert(s.visits === '2', 'scripts 区域 pjax:loaded 计数正常累加到 2', s.visits);

  const tickListMid = s.tick.list;
  await page.waitForTimeout(500);
  const s4 = await snap(page);
  assert(s4.tick.list === tickListMid, '/list 的 setInterval 已被回收（心跳停止）', { before: tickListBefore, mid: tickListMid, after: s4.tick.list });
  assert(s4.tick.home > 0, '/home 新一轮 setInterval 正常运行', s4.tick.home);

  const clicksBefore2 = s4.docClicks;
  await clickBlank(page);
  const s5 = await snap(page);
  assert(s5.docClicks === clicksBefore2 + 1, '两轮切换后一次点击仍只计 1 次（监听器无叠加）', { before: clicksBefore2, after: s5.docClicks });

  // 顶层函数在区域替换后仍然可用
  await clickButton(page, '#rt-hello-btn');
  const s6 = await snap(page);
  assert(s6.helloCalls === s5.helloCalls + 1, '区域替换后 onclick="rtHello()" 依然可用', { before: s5.helloCalls, after: s6.helloCalls });

  // 监听器总量稳态：不随切换次数线性增长
  assert(s6.stats.listeners <= listenersAfterFirst + 2,
    '监听器总量保持稳态（未随切换线性增长）', { first: listenersAfterFirst, now: s6.stats.listeners });
  assert(s6.stats.once === 1, 'Pjax.stats().once 记录 1 条 once 指纹', s6.stats.once);

  // ---------- 4. 无脚本错误 ----------
  console.log('\n=== 4. 全程无 JS 运行时错误 ===');
  assert(pageErrors.length === 0, '全程无 pageerror（无重复声明 SyntaxError）', pageErrors.slice(0, 5));

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
})();
