/**
 * Wire 命名组件 浏览器端到端验证（桌面 + 移动 双视口）。
 *
 * 验证点（每个视口都完整跑一遍）：
 *   - 首屏 bootstrap 自动挂载
 *   - 四个生命周期顺序 onCreate → onStart →（stop）→ onStop → onDestroy
 *   - Wire 更新下发的组件无感挂载
 *   - 多实例互相隔离（一个 stop 不影响另一个）
 *   - 自定义 wire_outlet() 位置
 *   - 例外路径 /blade-demo 不注入
 *   - Wire 透明导航时组件随 payload.components 一起下发
 *
 * 运行：NODE_PATH=<node-workspace>/node_modules <node> e2e/wire-component.spec.js
 * 真实浏览器：优先 PW_EXECUTABLE → 360ChromeX → Chrome → Edge → 自带 chromium。
 */
const fs = require('fs');
const { chromium } = require('playwright');

const BASE = process.env.JARAVEL_BASE || 'http://localhost:8080';

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

const sleep = ms => new Promise(r => setTimeout(r, ms));

// 桌面 + 移动 双视口；移动端开启触摸与移动仿真，模拟真实手机。
const VIEWPORTS = [
    { name: 'desktop', viewport: { width: 1280, height: 800 }, isMobile: false, hasTouch: false },
    { name: 'mobile', viewport: { width: 390, height: 844 }, isMobile: true, hasTouch: true, deviceScaleFactor: 2 },
];

// 每个视口独立统计
function makeRecorder() {
    const pass = [], fail = [];
    const check = (name, cond, detail) => {
        (cond ? pass : fail).push(name);
        console.log((cond ? '  [OK]   ' : '  [FAIL] ') + name + (detail ? '  -> ' + detail : ''));
    };
    return { pass, fail, check };
}

async function runViewport(browser, vp) {
    console.log('\n################ 视口：' + vp.name.toUpperCase() +
        ' (' + vp.viewport.width + 'x' + vp.viewport.height + (vp.isMobile ? ', mobile/touch' : '') + ') ################');
    const { pass, fail, check } = makeRecorder();
    const page = await browser.newPage(vp);
    const logs = [];
    page.on('console', m => logs.push(m.text()));
    page.on('pageerror', e => logs.push('PAGEERROR: ' + e.message));

    // 视口生效校验：桌面用 innerWidth，移动用「触摸/粗指针」媒体特性
    // （360ChromeX 的 isMobile 上下文 innerWidth 仍为 980，不能用像素宽度判定）。
    if (vp.isMobile) {
        const coarse = await page.evaluate(() => matchMedia('(pointer: coarse)').matches && navigator.maxTouchPoints > 0);
        check('移动视口已生效（pointer:coarse 且可触摸）', coarse, String(coarse));
    } else {
        const vw = await page.evaluate(() => window.innerWidth);
        check('桌面视口已生效（innerWidth=' + vw + '）', vw === vp.viewport.width, String(vw));
    }

    // 1. 首屏自动挂载
    console.log('\n=== 1. 首屏 bootstrap 自动挂载 ===');
    await page.goto(BASE + '/wire-component-demo', { waitUntil: 'networkidle' });
    await sleep(400);
    check('window.WireComponent 已加载', await page.evaluate(() => typeof window.WireComponent === 'object'));
    check('outlet 容器存在且唯一', await page.evaluate(() => document.querySelectorAll('[wire\\:outlet]').length) === 1);
    const mounted = await page.evaluate(() => Array.from(document.querySelectorAll('[wire\\:outlet] > *')).map(e => e.id));
    check('首屏 toast 已挂载到 outlet', mounted.length === 1 && /^wc-toast-\d+$/.test(mounted[0]), JSON.stringify(mounted));
    check('bootstrap <script wire:components> 挂载后已移除', await page.evaluate(() => document.querySelectorAll('script[wire\\:components]').length) === 0);
    check('onCreate → onStart 已按序触发',
        logs.some(l => l.includes('onCreate')) && logs.some(l => l.includes('onStart')) &&
        logs.findIndex(l => l.includes('onCreate')) < logs.findIndex(l => l.includes('onStart')),
        JSON.stringify(logs.slice(0, 4)));

    // 2. ttl 到期自动 stop
    console.log('\n=== 2. ttl 到期自动走完 onStop → onDestroy ===');
    logs.length = 0;
    await sleep(4600);
    check('onStop 已触发', logs.some(l => l.includes('onStop')), JSON.stringify(logs));
    check('onDestroy 已触发', logs.some(l => l.includes('onDestroy')));
    check('onStop 早于 onDestroy', logs.findIndex(l => l.includes('onStop')) < logs.findIndex(l => l.includes('onDestroy')));
    check('DOM 已被移除', await page.evaluate(() => document.querySelectorAll('[wire\\:outlet] > *').length) === 0);

    // 3. Wire 更新下发
    console.log('\n=== 3. Wire 更新下发 toast（effects.components 无感挂载）===');
    logs.length = 0;
    await page.click('button[wire\\:click="toast"]');
    await sleep(900);
    const t2 = await page.evaluate(() => Array.from(document.querySelectorAll('[wire\\:outlet] > *')).map(e => ({ id: e.id, cls: e.className, text: e.textContent.trim().slice(0, 30) })));
    check('Wire 更新后 toast 已挂载', t2.length === 1, JSON.stringify(t2));
    check('是 success 级别', t2.length === 1 && t2[0].cls.includes('wc-toast--success'), t2.length ? t2[0].cls : '');
    check('生命周期日志再次出现', logs.some(l => l.includes('onCreate')) && logs.some(l => l.includes('onStart')));

    // 4. 多实例隔离
    console.log('\n=== 4. 多实例隔离（连续 3 条）===');
    await sleep(4200);
    logs.length = 0;
    await page.click('button[wire\\:click="multi"]');
    await sleep(900);
    let ids = await page.evaluate(() => Array.from(document.querySelectorAll('[wire\\:outlet] > *')).map(e => e.id));
    check('同时挂载 3 个实例', ids.length === 3, JSON.stringify(ids));
    check('3 个实例 id 各不相同', new Set(ids).size === ids.length, JSON.stringify(ids));
    if (ids.length === 3) {
        const target = ids[1];
        await page.evaluate((id) => { const el = document.getElementById(id); if (el && typeof el.__wcStop === 'function') el.__wcStop(); }, target);
        await sleep(600);
        const left = await page.evaluate(() => Array.from(document.querySelectorAll('[wire\\:outlet] > *')).map(e => e.id));
        check('单独 stop 只移除目标实例', left.length === 2 && !left.includes(target), 'target=' + target + ' left=' + JSON.stringify(left));
        check('其余实例仍存活（闭包未串扰）', left.includes(ids[0]) && left.includes(ids[2]), JSON.stringify(left));
    }
    await sleep(4200);
    check('剩余实例 ttl 到期后全部清空', await page.evaluate(() => document.querySelectorAll('[wire\\:outlet] > *').length) === 0);

    // 5. confirm 交互
    console.log('\n=== 5. confirm 组件 wire.stop() 主动结束 ===');
    logs.length = 0;
    await page.click('button[wire\\:click="confirm"]');
    await sleep(800);
    check('confirm 已挂载', await page.evaluate(() => document.querySelectorAll('.wc-confirm-mask').length) === 1);
    const evt = await page.evaluate(() => new Promise(res => {
        document.addEventListener('wc:confirm', e => res(e.detail), { once: true });
        const btn = document.querySelector('.wc-confirm-mask [data-wc-ok]') || document.querySelector('.wc-confirm-mask button');
        btn && btn.click();
        setTimeout(() => res(null), 1500);
    }));
    check('点击后派发 wc:confirm 事件', evt !== null, JSON.stringify(evt));
    await sleep(600);
    check('confirm 调用 wire.stop() 后 DOM 已移除', await page.evaluate(() => document.querySelectorAll('.wc-confirm-mask').length) === 0);
    check('走完 onStop → onDestroy', logs.some(l => l.includes('onStop')) && logs.some(l => l.includes('onDestroy')), JSON.stringify(logs.slice(-4)));

    // 6. 自定义位置
    console.log('\n=== 6. 自定义 wire_outlet() 位置 ===');
    await page.goto(BASE + '/wire-component-plain', { waitUntil: 'networkidle' });
    await sleep(500);
    check('outlet 唯一', await page.evaluate(() => document.querySelectorAll('[wire\\:outlet]').length) === 1);
    check('toast 挂载在 custom-outlet-area 虚线框内', await page.evaluate(() => { const el = document.querySelector('.custom-outlet-area [wire\\:outlet] > *'); return !!el && el.className.includes('wc-toast'); }));
    check('outlet 不在 body 末尾（父节点是自定义区域）', await page.evaluate(() => { const o = document.querySelector('[wire\\:outlet]'); return o && o.parentElement && o.parentElement.classList.contains('custom-outlet-area'); }));

    // 7. 例外路径
    console.log('\n=== 7. 例外路径 /blade-demo ===');
    await page.goto(BASE + '/blade-demo', { waitUntil: 'networkidle' });
    await sleep(300);
    check('未注入 outlet', await page.evaluate(() => document.querySelectorAll('[wire\\:outlet]').length) === 0);
    check('未加载 WireComponent 运行时', await page.evaluate(() => typeof window.WireComponent === 'undefined'));

    // 8. Wire 透明导航联动
    console.log('\n=== 8. Wire 透明导航时组件随 payload.components 一起下发 ===');
    await page.goto(BASE + '/wire-dashboard', { waitUntil: 'networkidle' });
    await sleep(500);
    check('/wire-dashboard 首屏正常', await page.evaluate(() => !!window.__wireHashes));
    // 点击导航链接（带 wire-navigate 属性），触发 Wire 局部切换
    const beforeUrl = page.url();
    await page.click('a[href="/wire-records"]');
    await sleep(1000);
    check('URL 已切换到 /wire-records', page.url().endsWith('/wire-records'), page.url());
    check('前后 URL 不同（确认发生了导航）', beforeUrl !== page.url(), beforeUrl + ' -> ' + page.url());
    check('Wire 导航后 outlet 存在且唯一', await page.evaluate(() => document.querySelectorAll('[wire\\:outlet]').length) === 1);
    const wireMounted = await page.evaluate(() => Array.from(document.querySelectorAll('[wire\\:outlet] > *')).map(e => e.className));
    check('Wire 导航后 toast 已无感挂载', wireMounted.length === 1 && wireMounted[0].includes('wc-toast--warning'), JSON.stringify(wireMounted));

    await page.close();
    return { name: vp.name, pass: pass.length, fail: fail.length, failItems: fail.slice() };
}

(async () => {
    // 每个视口各自启动一个浏览器实例：
    // 360ChromeX 在单一浏览器会话里开第二个 tab 会报 "Failed to open a new tab"，
    // 分进程启动可避免该限制，且更贴近「真实独立浏览器」语义。
    const results = [];
    for (const vp of VIEWPORTS) {
        const browser = await chromium.launch(Object.assign(
            { headless: true, args: ['--no-sandbox', '--disable-gpu'] },
            EXE ? { executablePath: EXE } : {}
        ));
        results.push(await runViewport(browser, vp));
        await browser.close();
    }

    console.log('\n' + '='.repeat(60));
    let allFail = 0;
    for (const r of results) {
        console.log(`视口 ${r.name}: 通过 ${r.pass} 项，失败 ${r.fail} 项`);
        if (r.fail) r.failItems.forEach(f => console.log('    - ' + f));
        allFail += r.fail;
    }
    console.log('='.repeat(60));
    if (allFail) { console.log('存在失败项 ❌'); process.exit(1); }
    console.log('全部视口（桌面 + 移动）全部通过 ✅');
})().catch(e => { console.error('EXEC ERROR:', e); process.exit(2); });
