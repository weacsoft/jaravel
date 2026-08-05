/**
 * Wire 命名组件 浏览器端到端验证：
 *   - 首屏 bootstrap 自动挂载
 *   - 四个生命周期顺序 onCreate → onStart →（stop）→ onStop → onDestroy
 *   - Wire 更新下发的组件无感挂载
 *   - 多实例互相隔离（一个 stop 不影响另一个）
 *   - 自定义 wire_outlet() 位置
 */
const fs = require('fs');
const { chromium } = require('playwright');

const BASE = process.env.PJAX_BASE || 'http://localhost:8080';

// 浏览器可执行文件：优先 PW_EXECUTABLE，其次本机已装的 Chromium 内核浏览器，最后回落 Playwright 自带。
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

const pass = [], fail = [];
function check(name, cond, detail) {
    (cond ? pass : fail).push(name);
    console.log((cond ? '  [OK]   ' : '  [FAIL] ') + name + (detail ? '  -> ' + detail : ''));
}
const sleep = ms => new Promise(r => setTimeout(r, ms));

(async () => {
    const browser = await chromium.launch(Object.assign(
        { headless: true, args: ['--no-sandbox', '--disable-gpu'] },
        EXE ? { executablePath: EXE } : {}
    ));
    const page = await browser.newPage();
    const logs = [];
    page.on('console', m => logs.push(m.text()));
    page.on('pageerror', e => logs.push('PAGEERROR: ' + e.message));

    // ---------------------------------------------------- 1. 首屏自动挂载
    console.log('\n=== 1. 首屏 bootstrap 自动挂载 ===');
    await page.goto(BASE + '/wire-component-demo', { waitUntil: 'networkidle' });
    await sleep(400);

    check('window.WireComponent 已加载',
        await page.evaluate(() => typeof window.WireComponent === 'object'));
    check('outlet 容器存在且唯一',
        await page.evaluate(() => document.querySelectorAll('[wire\\:outlet]').length) === 1);
    const mounted = await page.evaluate(() =>
        Array.from(document.querySelectorAll('[wire\\:outlet] > *')).map(e => e.id));
    check('首屏 toast 已挂载到 outlet', mounted.length === 1 && /^wc-toast-\d+$/.test(mounted[0]),
        JSON.stringify(mounted));
    check('bootstrap <script wire:components> 挂载后已移除',
        await page.evaluate(() => document.querySelectorAll('script[wire\\:components]').length) === 0);
    check('onCreate → onStart 已按序触发',
        logs.some(l => l.includes('onCreate')) && logs.some(l => l.includes('onStart')) &&
        logs.findIndex(l => l.includes('onCreate')) < logs.findIndex(l => l.includes('onStart')),
        JSON.stringify(logs.slice(0, 4)));

    // ---------------------------------------------------- 2. ttl 到期自动 stop
    console.log('\n=== 2. ttl 到期自动走完 onStop → onDestroy ===');
    logs.length = 0;
    await sleep(4600); // 首屏 toast ttl=4000 + 淡出 280
    check('onStop 已触发', logs.some(l => l.includes('onStop')), JSON.stringify(logs));
    check('onDestroy 已触发', logs.some(l => l.includes('onDestroy')));
    check('onStop 早于 onDestroy',
        logs.findIndex(l => l.includes('onStop')) < logs.findIndex(l => l.includes('onDestroy')));
    check('DOM 已被移除',
        await page.evaluate(() => document.querySelectorAll('[wire\\:outlet] > *').length) === 0);

    // ---------------------------------------------------- 3. Wire 更新下发
    console.log('\n=== 3. Wire 更新下发 toast（effects.components 无感挂载）===');
    logs.length = 0;
    await page.click('button[wire\\:click="toast"]');
    await sleep(900);
    const t2 = await page.evaluate(() =>
        Array.from(document.querySelectorAll('[wire\\:outlet] > *')).map(e => ({
            id: e.id, cls: e.className, text: e.textContent.trim().slice(0, 30)
        })));
    check('Wire 更新后 toast 已挂载', t2.length === 1, JSON.stringify(t2));
    check('是 success 级别', t2.length === 1 && t2[0].cls.includes('wc-toast--success'),
        t2.length ? t2[0].cls : '');
    check('生命周期日志再次出现',
        logs.some(l => l.includes('onCreate')) && logs.some(l => l.includes('onStart')));

    // ---------------------------------------------------- 4. 多实例隔离
    console.log('\n=== 4. 多实例隔离（连续 3 条）===');
    await sleep(4200); // 等上一条消失
    logs.length = 0;
    await page.click('button[wire\\:click="multi"]');
    await sleep(900);
    let ids = await page.evaluate(() =>
        Array.from(document.querySelectorAll('[wire\\:outlet] > *')).map(e => e.id));
    check('同时挂载 3 个实例', ids.length === 3, JSON.stringify(ids));
    check('3 个实例 id 各不相同', new Set(ids).size === ids.length, JSON.stringify(ids));

    // 单独 stop 中间那个，验证不影响其余两个
    if (ids.length === 3) {
        const target = ids[1];
        await page.evaluate((id) => {
            const el = document.getElementById(id);
            if (el && typeof el.__wcStop === 'function') el.__wcStop();
        }, target);
        await sleep(600);
        const left = await page.evaluate(() =>
            Array.from(document.querySelectorAll('[wire\\:outlet] > *')).map(e => e.id));
        check('单独 stop 只移除目标实例',
            left.length === 2 && !left.includes(target),
            'target=' + target + ' left=' + JSON.stringify(left));
        check('其余实例仍存活（闭包未串扰）',
            left.includes(ids[0]) && left.includes(ids[2]), JSON.stringify(left));
    }
    await sleep(4200);
    check('剩余实例 ttl 到期后全部清空',
        await page.evaluate(() => document.querySelectorAll('[wire\\:outlet] > *').length) === 0);

    // ---------------------------------------------------- 5. confirm 交互
    console.log('\n=== 5. confirm 组件 wire.stop() 主动结束 ===');
    logs.length = 0;
    await page.click('button[wire\\:click="confirm"]');
    await sleep(800);
    check('confirm 已挂载',
        await page.evaluate(() => document.querySelectorAll('.wc-confirm-mask').length) === 1);
    const evt = await page.evaluate(() => new Promise(res => {
        document.addEventListener('wc:confirm', e => res(e.detail), { once: true });
        const btn = document.querySelector('.wc-confirm-mask [data-wc-ok]') ||
            document.querySelector('.wc-confirm-mask button');
        btn && btn.click();
        setTimeout(() => res(null), 1500);
    }));
    check('点击后派发 wc:confirm 事件', evt !== null, JSON.stringify(evt));
    await sleep(600);
    check('confirm 调用 wire.stop() 后 DOM 已移除',
        await page.evaluate(() => document.querySelectorAll('.wc-confirm-mask').length) === 0);
    check('走完 onStop → onDestroy',
        logs.some(l => l.includes('onStop')) && logs.some(l => l.includes('onDestroy')),
        JSON.stringify(logs.slice(-4)));

    // ---------------------------------------------------- 6. 自定义位置
    console.log('\n=== 6. 自定义 wire_outlet() 位置 ===');
    await page.goto(BASE + '/wire-component-plain', { waitUntil: 'networkidle' });
    await sleep(500);
    check('outlet 唯一',
        await page.evaluate(() => document.querySelectorAll('[wire\\:outlet]').length) === 1);
    check('toast 挂载在 custom-outlet-area 虚线框内',
        await page.evaluate(() => {
            const el = document.querySelector('.custom-outlet-area [wire\\:outlet] > *');
            return !!el && el.className.includes('wc-toast');
        }));
    check('outlet 不在 body 末尾（父节点是自定义区域）',
        await page.evaluate(() => {
            const o = document.querySelector('[wire\\:outlet]');
            return o && o.parentElement && o.parentElement.classList.contains('custom-outlet-area');
        }));

    // ---------------------------------------------------- 7. 例外路径
    console.log('\n=== 7. 例外路径 /blade-demo ===');
    await page.goto(BASE + '/blade-demo', { waitUntil: 'networkidle' });
    await sleep(300);
    check('未注入 outlet',
        await page.evaluate(() => document.querySelectorAll('[wire\\:outlet]').length) === 0);
    check('未加载 WireComponent 运行时',
        await page.evaluate(() => typeof window.WireComponent === 'undefined'));

    // ---------------------------------------------------- 8. PJAX 导航联动
    // 注：Wire 页面（/wire-component-demo）自带局部更新能力，不纳入 PJAX；
    //     这里从 PJAX 页面 /home 出发，验证 PJAX 局部导航时命名组件随 payload.components 无感挂载。
    console.log('\n=== 8. PJAX 局部导航时组件随 payload.components 一起下发 ===');
    await page.goto(BASE + '/home', { waitUntil: 'networkidle' });
    await sleep(500);
    check('/home 已启用 PJAX', await page.evaluate(() => !!(window.Pjax && window.Pjax.__installed)));

    const pjaxReqs = [];
    page.on('request', r => { if (r.headers()['x-pjax']) pjaxReqs.push(r.url()); });
    const beforeUrl = page.url();

    await page.evaluate(() => window.Pjax.visit('/wire-component-plain'));
    await sleep(1500);

    check('确实走了 PJAX 局部请求（非整页刷新）',
        pjaxReqs.some(u => u.includes('/wire-component-plain')), JSON.stringify(pjaxReqs));
    check('URL 已切换到 /wire-component-plain',
        page.url().endsWith('/wire-component-plain'), page.url());
    check('前后 URL 不同（确认发生了导航）', beforeUrl !== page.url(),
        beforeUrl + ' -> ' + page.url());
    check('PJAX 后 outlet 存在且唯一',
        await page.evaluate(() => document.querySelectorAll('[wire\\:outlet]').length) === 1);
    const pjaxMounted = await page.evaluate(() =>
        Array.from(document.querySelectorAll('[wire\\:outlet] > *')).map(e => e.className));
    check('PJAX 后新页面的 toast 已无感挂载',
        pjaxMounted.length === 1 && pjaxMounted[0].includes('wc-toast--warning'),
        JSON.stringify(pjaxMounted));

    await browser.close();

    console.log('\n' + '='.repeat(60));
    console.log('通过 ' + pass.length + ' 项，失败 ' + fail.length + ' 项');
    if (fail.length) {
        console.log('失败项：');
        fail.forEach(f => console.log('  - ' + f));
        process.exit(1);
    }
    console.log('全部通过 ✅');
})().catch(e => { console.error('EXEC ERROR:', e); process.exit(2); });
