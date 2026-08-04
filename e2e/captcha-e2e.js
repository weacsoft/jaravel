/**
 * 验证码模块端到端测试（真实浏览器 · 桌面端 + 移动端）
 * ====================================================================
 *
 * 覆盖四项改造：
 *   #1 前端资源自包含        —— /jaravel-captcha.js 由 captcha jar 直接提供，页面不依赖 mdui 即可运行
 *   #2 配置权限边界          —— 前端无法传递任何难度参数，越权写法被前后端双重拦截
 *   #3 全屏弹层模式          —— 遮罩全屏、验证框居中、ESC / 遮罩 / 关闭按钮均可关闭
 *   #4 跨端兼容              —— 同一份代码在鼠标与触摸下均正确，且坐标换算不受缩放影响
 *
 * 其中滑动验证码做<b>真正的闭环校验</b>：
 * 通过模板匹配从图片还原真实缺口 gapX（滑块小块是从「未加暗」的背景裁下的，
 * 而背景上真实缺口被 alpha=130 的黑色覆盖，即像素值 ×0.4902），
 * 拖到该位置后把 complete 事件产出的密文提交给后端 /api/captcha/verify，
 * 要求返回 code=200。移动端若坐标换算有误，此项必然失败。
 */

const { chromium, devices } = require('playwright');

const BASE = process.env.BASE_URL || 'http://localhost:8080';
const DEMO = BASE + '/captcha-demo';

let passed = 0, failed = 0;
const failures = [];

function ok(name, detail) {
    passed++;
    console.log(`  \x1b[32m✓\x1b[0m ${name}${detail ? '  \x1b[90m' + detail + '\x1b[0m' : ''}`);
}
function bad(name, detail) {
    failed++;
    failures.push(name + (detail ? ' — ' + detail : ''));
    console.log(`  \x1b[31m✗\x1b[0m ${name}${detail ? '  \x1b[31m' + detail + '\x1b[0m' : ''}`);
}
function assert(cond, name, detail) {
    cond ? ok(name, detail) : bad(name, detail);
    return cond;
}

// ====================================================================
// 页内注入的工具函数
// ====================================================================

/**
 * 在浏览器里用 canvas 还原滑动验证码的真实缺口 X 坐标。
 * 返回 {gapX, score}，score 越小匹配越好。
 */
const DETECT_GAP = `
(async function(data) {
    const load = (src) => new Promise((res, rej) => {
        const im = new Image();
        im.onload = () => res(im);
        im.onerror = rej;
        im.src = src.startsWith('data:') ? src : 'data:image/png;base64,' + src;
    });

    const bgImg = await load(data.bg);
    const blockImg = await load(data.block);

    const W = bgImg.width, H = bgImg.height;
    const bw = blockImg.width, bh = blockImg.height;
    const gapY = data.gapY;

    const c1 = document.createElement('canvas'); c1.width = W; c1.height = H;
    const x1 = c1.getContext('2d', { willReadFrequently: true });
    x1.drawImage(bgImg, 0, 0);
    const bg = x1.getImageData(0, 0, W, H).data;

    const c2 = document.createElement('canvas'); c2.width = bw; c2.height = bh;
    const x2 = c2.getContext('2d', { willReadFrequently: true });
    x2.drawImage(blockImg, 0, 0);
    const bk = x2.getImageData(0, 0, bw, bh).data;

    // 黑色 alpha=130 覆盖后：new = old * (1 - 130/255)
    const K = 1 - 130 / 255;

    // 只取形状内部、且非白色描边的像素参与匹配
    const samples = [];
    for (let j = 0; j < bh; j++) {
        for (let i = 0; i < bw; i++) {
            const o = (j * bw + i) * 4;
            const a = bk[o + 3];
            if (a < 250) continue;                                  // 形状外/边缘
            const r = bk[o], g = bk[o + 1], b = bk[o + 2];
            if (r > 200 && g > 200 && b > 200) continue;            // 白色描边
            samples.push([i, j, r, g, b]);
        }
    }
    if (samples.length < 30) return { gapX: -1, score: -1, samples: samples.length };

    let best = -1, bestScore = Infinity, second = Infinity;
    for (let x = 0; x <= W - bw; x++) {
        let sum = 0;
        for (let s = 0; s < samples.length; s++) {
            const [i, j, r, g, b] = samples[s];
            const yy = gapY + j;
            if (yy < 0 || yy >= H) { sum = Infinity; break; }
            const o = (yy * W + x + i) * 4;
            sum += Math.abs(bg[o] - r * K)
                 + Math.abs(bg[o + 1] - g * K)
                 + Math.abs(bg[o + 2] - b * K);
        }
        const score = sum / samples.length;
        if (score < bestScore) { second = bestScore; bestScore = score; best = x; }
        else if (score < second) { second = score; }
    }
    return { gapX: best, score: bestScore, second: second, samples: samples.length };
})
`;

/**
 * 在页面中创建一个受控的验证码实例，并把关键状态挂到 window.__jc 上。
 */
async function createInstance(page, opts) {
    await page.evaluate(({ opts }) => {
        if (window.__jc && window.__jc.instance) {
            window.__jc.instance.destroy();
        }
        let host = document.getElementById('e2e-host');
        if (!host) {
            host = document.createElement('div');
            host.id = 'e2e-host';
            document.body.appendChild(host);
        }
        host.style.display = 'block';
        host.innerHTML = '';

        const state = {
            instance: null,
            completed: null,
            data: null,
            key: null,
            warns: [],
            requests: [],
            shown: 0,
            hidden: 0
        };
        window.__jc = state;

        // 捕获前端告警（用于配置权限边界断言）
        if (!window.__warnHooked) {
            window.__warnHooked = true;
            const orig = console.warn;
            console.warn = function () {
                const msg = Array.prototype.slice.call(arguments).join(' ');
                if (window.__jc) window.__jc.warns.push(msg);
                orig.apply(console, arguments);
            };
        }

        const inst = Captcha.init('e2e-host', opts);
        state.instance = inst;
        inst.on('afterGet', (key, data) => { state.key = key; state.data = data; });
        inst.on('complete', (key, input) => { state.completed = { key, input }; });
        inst.on('show', () => { state.shown++; });
        inst.on('hide', () => { state.hidden++; });
        inst.show();
    }, { opts });

    // 等验证码加载完成
    await page.waitForFunction(() => window.__jc && window.__jc.data, null, { timeout: 15000 });

    // 宿主容器追加在 demo 页末尾，桌面视口下会落在首屏之外。
    // 鼠标事件走的是真实视口坐标，元素不在视口内就点不中（触摸走合成事件不受影响），
    // 因此交互前必须先滚到视口中央。
    await page.evaluate(() => {
        const host = document.getElementById('e2e-host');
        if (host && !document.body.classList.contains('jc-body-locked')) {
            host.scrollIntoView({ block: 'center' });
        }
    });
    await page.waitForTimeout(150);
}

/** 读取页面内实例的运行时快照 */
function snapshot(page) {
    return page.evaluate(() => {
        const s = window.__jc;
        const inst = s.instance;
        const rectOf = (el) => {
            if (!el) return null;
            const r = el.getBoundingClientRect();
            return { x: r.x, y: r.y, w: r.width, h: r.height };
        };
        return {
            key: s.key,
            completed: s.completed,
            warns: s.warns,
            shown: s.shown,
            hidden: s.hidden,
            extra: s.data ? s.data.extra : null,
            stageScale: inst._getStageScale ? inst._getStageScale() : null,
            stageBaseWidth: inst._stageBaseWidth,
            handleLeft: inst._sliderHandleEl ? inst._sliderHandleEl.offsetLeft : null,
            rotateHandleLeft: inst._rotateHandleEl ? inst._rotateHandleEl.offsetLeft : null,
            clickPoints: inst._clickPoints ? inst._clickPoints.slice() : [],
            overlay: rectOf(inst._overlayEl),
            modal: rectOf(inst._modalEl),
            overlayDisplay: inst._overlayEl ? getComputedStyle(inst._overlayEl).display : null,
            overlayOpacity: inst._overlayEl ? parseFloat(getComputedStyle(inst._overlayEl).opacity) : null,
            modalTransform: inst._modalEl ? getComputedStyle(inst._modalEl).transform : null,
            bodyLocked: document.body.classList.contains('jc-body-locked'),
            trackRect: rectOf(inst._sliderTrackEl || inst._rotateTrackEl),
            handleRect: rectOf(inst._sliderHandleEl || inst._rotateHandleEl),
            imgRect: rectOf(inst._clickImgEl)
        };
    });
}

/**
 * 用鼠标或触摸把手柄从当前位置拖到目标屏幕 X。
 * @param {boolean} touch true=触摸事件，false=鼠标事件
 */
// 后端 TrajectoryValidator 要求：轨迹点 ≥5、总时长 ≥500ms、相邻跳变 ≤80px、
// 速度非匀速、且拖动距离 >20px 时必须同时出现正负加速度（先加速后减速）。
// 因此这里用 easeInOutQuad 曲线 + 真实逐帧延时，模拟人手的加速—减速过程。
const DRAG_STEPS = 24;
const DRAG_STEP_MS = 25;   // 24 × 25ms ≈ 600ms > 后端最小时长 500ms
const easeInOutQuad = (p) => (p < 0.5 ? 2 * p * p : 1 - Math.pow(-2 * p + 2, 2) / 2);

async function dragHandle(page, touch, targetClientX) {
    const s = await snapshot(page);
    const hx = s.handleRect.x + s.handleRect.w / 2;
    const hy = s.handleRect.y + s.handleRect.h / 2;

    if (!touch) {
        await page.mouse.move(hx, hy);
        await page.mouse.down();
        for (let i = 1; i <= DRAG_STEPS; i++) {
            const e = easeInOutQuad(i / DRAG_STEPS);
            await page.mouse.move(hx + (targetClientX - hx) * e, hy + (Math.random() * 2 - 1));
            await page.waitForTimeout(DRAG_STEP_MS);
        }
        await page.mouse.up();
    } else {
        // 真实触摸事件序列：touchstart → 多次 touchmove → touchend
        await page.evaluate(async ({ hx, hy, tx, steps, stepMs }) => {
            const el = document.querySelector('.jc-drag-handle');
            const sleep = (ms) => new Promise((r) => setTimeout(r, ms));
            const ease = (p) => (p < 0.5 ? 2 * p * p : 1 - Math.pow(-2 * p + 2, 2) / 2);
            const mk = (type, x, y) => {
                const t = new Touch({ identifier: 1, target: el, clientX: x, clientY: y });
                return new TouchEvent(type, {
                    touches: type === 'touchend' ? [] : [t],
                    targetTouches: type === 'touchend' ? [] : [t],
                    changedTouches: [t],
                    bubbles: true, cancelable: true
                });
            };
            el.dispatchEvent(mk('touchstart', hx, hy));
            await sleep(30);
            for (let i = 1; i <= steps; i++) {
                const x = hx + (tx - hx) * ease(i / steps);
                document.dispatchEvent(mk('touchmove', x, hy));
                await sleep(stepMs);
            }
            document.dispatchEvent(mk('touchend', tx, hy));
        }, { hx, hy, tx: targetClientX, steps: DRAG_STEPS, stepMs: DRAG_STEP_MS });
    }
}

/** 调用后端校验接口 */
async function verifyOnServer(page, type, key, input) {
    return page.evaluate(async ({ type, key, input, base }) => {
        const r = await fetch(base + '/api/captcha/verify', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ type, captchaKey: key, input })
        });
        let body = null;
        try { body = await r.json(); } catch (e) { body = { parseError: true }; }
        return { status: r.status, body };
    }, { type, key, input, base: BASE });
}

// ====================================================================
// 各项测试
// ====================================================================

/** 滑动验证码：闭环校验（还原缺口 → 拖动 → 提交后端 → 期望通过） */
async function testSlider(page, ctxName, touch) {
    console.log(`\n  [滑动验证码 · 闭环校验]`);
    await createInstance(page, {
        type: 'slider', scene: 'login',
        encryptionType: 'aes', encryptionKey: 'jaravel-captcha-default-key'
    });

    const s0 = await snapshot(page);
    const gap = await page.evaluate(async ({ fnSrc, arg }) => {
        const fn = eval(fnSrc);
        return await fn(arg);
    }, {
        fnSrc: DETECT_GAP,
        arg: await page.evaluate(() => ({
            bg: window.__jc.data.imageBase64,
            block: window.__jc.data.extra.sliderImage,
            gapY: window.__jc.data.extra.gapY
        }))
    });

    assert(gap.gapX >= 0, `${ctxName} 缺口还原成功`, `gapX=${gap.gapX} score=${gap.score.toFixed(1)} 次优=${gap.second.toFixed(1)}`);

    // 舞台缩放系数
    const scale = s0.stageScale;
    // 目标：把手柄左边缘移动到 gapX（布局像素）→ 屏幕位移 = gapX * scale
    const targetLayoutLeft = gap.gapX;
    const deltaLayout = targetLayoutLeft - s0.handleLeft;
    const targetClientX = s0.handleRect.x + s0.handleRect.w / 2 + deltaLayout * scale;

    await dragHandle(page, touch, targetClientX);
    await page.waitForTimeout(300);

    const s1 = await snapshot(page);
    assert(Math.abs(s1.handleLeft - targetLayoutLeft) <= 2,
        `${ctxName} 拖动落点精确（缩放系数 ${scale.toFixed(3)}）`,
        `期望 ${targetLayoutLeft}px 实际 ${s1.handleLeft}px`);

    assert(!!s1.completed, `${ctxName} complete 事件已触发`);

    if (s1.completed) {
        const res = await verifyOnServer(page, 'slider', s1.completed.key, s1.completed.input);
        assert(res.status === 200 && res.body.code === 200,
            `${ctxName} 后端校验通过（真闭环）`,
            `HTTP ${res.status} ${JSON.stringify(res.body)}`);

        // 防复用：同一个 key 再验一次必须被拒
        const again = await verifyOnServer(page, 'slider', s1.completed.key, s1.completed.input);
        assert(again.status === 410,
            `${ctxName} 防复用生效（重复提交返回 410）`,
            `HTTP ${again.status}`);
    }
}

/** 旋转验证码：坐标换算精度 */
async function testRotate(page, ctxName, touch) {
    console.log(`\n  [旋转验证码 · 坐标换算]`);
    await createInstance(page, { type: 'rotate' });
    const s0 = await snapshot(page);

    // 拖到轨道 60% 处
    const trackLayoutW = await page.evaluate(() => window.__jc.instance._rotateTrackEl.offsetWidth);
    const handleLayoutW = await page.evaluate(() => window.__jc.instance._rotateHandleEl.offsetWidth);
    const maxLeft = trackLayoutW - handleLayoutW;
    const targetLayout = Math.round(maxLeft * 0.6);
    const scale = s0.stageScale;
    const targetClientX = s0.handleRect.x + s0.handleRect.w / 2 + (targetLayout - s0.rotateHandleLeft) * scale;

    await dragHandle(page, touch, targetClientX);
    await page.waitForTimeout(300);

    const s1 = await snapshot(page);
    assert(Math.abs(s1.rotateHandleLeft - targetLayout) <= 2,
        `${ctxName} 旋转拖动落点精确`,
        `期望 ${targetLayout}px 实际 ${s1.rotateHandleLeft}px（scale=${scale.toFixed(3)}）`);
    assert(!!s1.completed, `${ctxName} 旋转 complete 已触发`);
}

/** 点选验证码：点击坐标换算精度（图片像素） */
async function testClick(page, ctxName, touch) {
    console.log(`\n  [点选验证码 · 坐标换算]`);
    await createInstance(page, { type: 'click' });
    const s0 = await snapshot(page);

    const imgW = s0.extra.width, imgH = s0.extra.height;
    // 选三个已知的图片像素坐标
    const targets = [
        { x: Math.round(imgW * 0.25), y: Math.round(imgH * 0.30) },
        { x: Math.round(imgW * 0.50), y: Math.round(imgH * 0.55) },
        { x: Math.round(imgW * 0.75), y: Math.round(imgH * 0.40) }
    ];
    const need = Math.min(targets.length, s0.extra.clickCount || 3);

    for (let i = 0; i < need; i++) {
        const t = targets[i];
        const r = (await snapshot(page)).imgRect;
        // 图片像素 → 屏幕坐标
        const cx = r.x + (t.x / imgW) * r.w;
        const cy = r.y + (t.y / imgH) * r.h;
        if (touch) {
            await page.evaluate(({ cx, cy }) => {
                const el = document.querySelector('.jc-click-img') || window.__jc.instance._clickImgEl;
                const tch = new Touch({ identifier: 1, target: el, clientX: cx, clientY: cy });
                el.dispatchEvent(new TouchEvent('touchend', {
                    touches: [], targetTouches: [], changedTouches: [tch],
                    bubbles: true, cancelable: true
                }));
            }, { cx, cy });
        } else {
            await page.mouse.click(cx, cy);
        }
        await page.waitForTimeout(80);
    }

    const s1 = await snapshot(page);
    assert(s1.clickPoints.length === need,
        `${ctxName} 点选次数正确（无幽灵点击重复计数）`,
        `期望 ${need} 实际 ${s1.clickPoints.length}`);

    let maxErr = 0;
    for (let i = 0; i < Math.min(need, s1.clickPoints.length); i++) {
        maxErr = Math.max(maxErr,
            Math.abs(s1.clickPoints[i].x - targets[i].x),
            Math.abs(s1.clickPoints[i].y - targets[i].y));
    }
    assert(maxErr <= 2,
        `${ctxName} 点选坐标换算精确（图片像素）`,
        `最大误差 ${maxErr}px，scale=${s0.stageScale.toFixed(3)}`);
}

/** 字符型验证码：输入与提交链路 */
async function testNumber(page, ctxName) {
    console.log(`\n  [数字验证码 · 输入链路]`);
    await createInstance(page, {
        type: 'number', scene: 'comment',
        encryptionType: 'aes', encryptionKey: 'jaravel-captcha-default-key'
    });
    await page.fill('.jc-input', 'ab12');
    await page.press('.jc-input', 'Enter');
    await page.waitForTimeout(200);

    const s = await snapshot(page);
    assert(!!s.completed, `${ctxName} 字符输入触发 complete`);
    if (s.completed) {
        const res = await verifyOnServer(page, 'number', s.completed.key, s.completed.input);
        // 随便输的答案几乎必错，但链路必须打通（403 而不是 400/500）
        assert(res.status === 403 || res.status === 200,
            `${ctxName} 校验链路连通`,
            `HTTP ${res.status} ${JSON.stringify(res.body)}`);
    }
}

/** 全屏弹层模式 */
async function testModal(page, ctxName, viewport) {
    console.log(`\n  [全屏弹层模式]`);
    await createInstance(page, {
        type: 'slider', modal: true, modalTitle: 'E2E 安全验证', autoCloseDelay: 0
    });
    await page.waitForTimeout(500);   // 等淡入过渡（180ms）彻底跑完再测量几何

    const s = await snapshot(page);
    assert(s.overlayDisplay === 'flex', `${ctxName} 遮罩已展开`, `display=${s.overlayDisplay}`);
    // display:flex 但 opacity:0 时用户其实什么都看不见，必须单独断言可见性
    assert(s.overlayOpacity >= 0.99,
        `${ctxName} 遮罩真实可见（opacity 已过渡到 1）`,
        `opacity=${s.overlayOpacity}`);
    assert(s.overlay && Math.abs(s.overlay.w - viewport.width) <= 1 && Math.abs(s.overlay.h - viewport.height) <= 1,
        `${ctxName} 遮罩全屏覆盖`,
        `${s.overlay ? s.overlay.w + 'x' + s.overlay.h : 'null'} vs 视口 ${viewport.width}x${viewport.height}`);

    // 居中判定
    const cxOverlay = s.overlay.x + s.overlay.w / 2;
    const cyOverlay = s.overlay.y + s.overlay.h / 2;
    const cxModal = s.modal.x + s.modal.w / 2;
    const cyModal = s.modal.y + s.modal.h / 2;
    assert(Math.abs(cxModal - cxOverlay) <= 2 && Math.abs(cyModal - cyOverlay) <= 2,
        `${ctxName} 验证框屏幕居中`,
        `弹层中心 (${cxModal.toFixed(0)}, ${cyModal.toFixed(0)}) vs 屏幕中心 (${cxOverlay.toFixed(0)}, ${cyOverlay.toFixed(0)})`);

    assert(s.modal.w <= viewport.width && s.modal.h <= viewport.height,
        `${ctxName} 弹层未溢出视口`,
        `弹层 ${s.modal.w.toFixed(0)}x${s.modal.h.toFixed(0)}`);

    assert(s.bodyLocked, `${ctxName} 页面滚动已锁定`);

    // 挂载点必须是 body 直接子元素，避免被父级 overflow/transform 裁剪
    const parentIsBody = await page.evaluate(() => window.__jc.instance._overlayEl.parentElement === document.body);
    assert(parentIsBody, `${ctxName} 遮罩挂载于 body（不受父级层叠上下文影响）`);

    // ESC 关闭
    await page.keyboard.press('Escape');
    await page.waitForTimeout(400);   // 覆盖 200ms 淡出延迟
    let s2 = await snapshot(page);
    assert(s2.overlayDisplay === 'none' && !s2.bodyLocked,
        `${ctxName} ESC 可关闭并解锁滚动`,
        `display=${s2.overlayDisplay} locked=${s2.bodyLocked}`);

    // 再次打开 → 点击遮罩空白处关闭
    await page.evaluate(() => window.__jc.instance.show());
    await page.waitForTimeout(400);
    await page.mouse.click(6, 6);   // 遮罩左上角空白
    await page.waitForTimeout(400);
    s2 = await snapshot(page);
    assert(s2.overlayDisplay === 'none', `${ctxName} 点击遮罩空白可关闭`);

    // 再次打开 → 关闭按钮
    await page.evaluate(() => window.__jc.instance.show());
    await page.waitForTimeout(400);
    await page.click('.jc-modal-close');
    await page.waitForTimeout(400);
    s2 = await snapshot(page);
    assert(s2.overlayDisplay === 'none', `${ctxName} 关闭按钮可关闭`);

    // destroy 后遮罩必须从 DOM 移除，且滚动锁解除
    await page.evaluate(() => window.__jc.instance.show());
    await page.waitForTimeout(400);
    const cleanup = await page.evaluate(() => {
        const inst = window.__jc.instance;
        const ov = inst._overlayEl;
        inst.destroy();
        return {
            detached: !ov.parentNode,
            locked: document.body.classList.contains('jc-body-locked'),
            leftover: document.querySelectorAll('.jc-overlay').length
        };
    });
    assert(cleanup.detached && !cleanup.locked && cleanup.leftover === 0,
        `${ctxName} destroy 彻底清理遮罩与滚动锁`,
        JSON.stringify(cleanup));
}

/** 配置权限边界 */
async function testPermissionBoundary(page, ctxName) {
    console.log(`\n  [配置权限边界]`);

    // 记录实际发出的 generate 请求
    const urls = [];
    const handler = (req) => {
        if (req.url().includes('/api/captcha/generate')) urls.push(req.url());
    };
    page.on('request', handler);

    await createInstance(page, {
        type: 'slider',
        config: { tolerance: 999, clickTargetCount: 1, length: 1, width: 9999 }
    });
    await page.waitForTimeout(200);
    page.off('request', handler);

    const last = urls[urls.length - 1] || '';
    const leaked = ['tolerance', 'clickTargetCount', 'length', 'width', 'noise', 'interfereLines']
        .filter((k) => last.toLowerCase().includes(k.toLowerCase()));
    assert(leaked.length === 0,
        `${ctxName} 难度参数未出现在请求中`,
        `URL=${last.replace(BASE, '')}`);

    const s = await snapshot(page);
    const warned = s.warns.some((w) => w.includes('config 已废弃'));
    assert(warned, `${ctxName} 旧 config 写法触发废弃告警`,
        s.warns.length ? s.warns[0].slice(0, 80) + '...' : '(无告警)');

    // 后端：直接构造越权请求，难度不应被改变
    const direct = await page.evaluate(async (base) => {
        const r = await fetch(base + '/api/captcha/generate?type=click&clickTargetCount=1&clickDecoyCount=0&tolerance=999');
        const j = await r.json();
        return j.data ? j.data.extra : null;
    }, BASE);
    assert(direct && direct.clickCount === 3,
        `${ctxName} 后端忽略越权参数（clickCount 仍为全局默认 3）`,
        `clickCount=${direct ? direct.clickCount : 'null'}`);

    // 场景白名单：命中的场景生效
    const reg = await page.evaluate(async (base) => {
        const r = await fetch(base + '/api/captcha/generate?type=click&scene=register');
        const j = await r.json();
        return j.data ? j.data.extra : null;
    }, BASE);
    assert(reg && reg.clickCount === 6,
        `${ctxName} 白名单场景 register 生效（clickCount=6）`,
        `clickCount=${reg ? reg.clickCount : 'null'}`);

    // 非法/未知场景名：静默回落全局默认，不得报错也不得降低难度
    const bogus = await page.evaluate(async (base) => {
        const out = {};
        for (const s of ['does-not-exist', '../../etc/passwd', 'a'.repeat(80), 'drop; table']) {
            const r = await fetch(base + '/api/captcha/generate?type=click&scene=' + encodeURIComponent(s));
            const j = await r.json();
            out[s.slice(0, 16)] = j.data ? j.data.extra.clickCount : ('ERR:' + j.code);
        }
        return out;
    }, BASE);
    const allDefault = Object.values(bogus).every((v) => v === 3);
    assert(allDefault, `${ctxName} 非法场景名安全回落全局默认`, JSON.stringify(bogus));
}

/** 静态资源自包含 */
async function testStaticAsset(page, ctxName) {
    console.log(`\n  [前端资源自包含]`);
    const res = await page.evaluate(async (base) => {
        const r = await fetch(base + '/jaravel-captcha.js');
        const t = await r.text();
        return { status: r.status, len: t.length, hasClass: t.includes('class Captcha'), mdui: /mdui/i.test(t) };
    }, BASE);
    assert(res.status === 200 && res.hasClass,
        `${ctxName} /jaravel-captcha.js 由 captcha jar 直接提供`,
        `HTTP ${res.status}, ${res.len} 字节`);
    assert(!res.mdui, `${ctxName} 脚本不含任何 mdui 依赖`);

    // 旧的 app 端副本必须已经不存在（避免两份分叉）
    const oldCopy = await page.evaluate(async (base) => {
        const r = await fetch(base + '/js/jaravel-captcha.js');
        return r.status;
    }, BASE);
    assert(oldCopy === 404, `${ctxName} app 端陈旧副本已移除`, `HTTP ${oldCopy}`);

    // 纯净页面（无 mdui）中也能独立工作
    const standalone = await page.evaluate(async (base) => {
        const w = document.createElement('iframe');
        w.style.cssText = 'position:fixed;left:-9999px;width:420px;height:600px;';
        document.body.appendChild(w);
        const d = w.contentDocument;
        d.open();
        d.write('<!DOCTYPE html><html><head><meta charset="utf-8"></head><body><div id="c"></div></body></html>');
        d.close();
        await new Promise((res) => {
            const s = d.createElement('script');
            s.src = base + '/jaravel-captcha.js';
            s.onload = res;
            d.head.appendChild(s);
        });
        const inst = w.contentWindow.Captcha.init('c', { type: 'number' });
        const okLoad = await new Promise((res) => {
            const timer = setTimeout(() => res(false), 8000);
            inst.on('afterGet', () => { clearTimeout(timer); res(true); });
        });
        const hasStyle = !!d.querySelector('style');
        const rendered = !!d.querySelector('.jc-wrapper');
        inst.destroy();
        w.remove();
        return { okLoad, hasStyle, rendered };
    }, BASE);
    assert(standalone.okLoad && standalone.rendered && standalone.hasStyle,
        `${ctxName} 无 mdui 的纯净页面中可独立运行（样式自注入）`,
        JSON.stringify(standalone));
}

/** 舞台缩放：窄屏必须自动缩小且不溢出 */
async function testResponsiveStage(page, ctxName, viewport) {
    console.log(`\n  [响应式舞台]`);
    await createInstance(page, { type: 'slider' });
    await page.waitForTimeout(300);
    const s = await snapshot(page);

    assert(s.stageBaseWidth > 0, `${ctxName} 舞台基准宽度已按后端图片设定`, `${s.stageBaseWidth}px`);

    const overflow = await page.evaluate(() => {
        const inst = window.__jc.instance;
        const wrap = inst._stageWrapEl;
        const stage = inst._stageEl;
        return {
            wrapW: wrap.getBoundingClientRect().width,
            stageW: stage.getBoundingClientRect().width,
            docOverflow: document.documentElement.scrollWidth - document.documentElement.clientWidth
        };
    });
    assert(overflow.stageW <= overflow.wrapW + 1,
        `${ctxName} 舞台未溢出容器`,
        `舞台 ${overflow.stageW.toFixed(1)}px ≤ 容器 ${overflow.wrapW.toFixed(1)}px（scale=${s.stageScale.toFixed(3)}）`);
    assert(overflow.docOverflow <= 0,
        `${ctxName} 页面无横向滚动条`,
        `溢出 ${overflow.docOverflow}px`);

    // 主动把容器压到小于图片原始宽度，验证舞台确实等比缩小而不是撑破容器。
    // （仅靠视口宽度不够：390px 视口下容器仍有 328px > 图片 300px，自然不会触发缩放）
    const squeezed = await page.evaluate(async () => {
        const host = document.getElementById('e2e-host');
        host.style.width = '200px';
        await new Promise((r) => setTimeout(r, 250));   // 等 ResizeObserver 回调 + 布局
        const inst = window.__jc.instance;
        const stage = inst._stageEl.getBoundingClientRect();
        const wrap = inst._stageWrapEl.getBoundingClientRect();
        const result = {
            scale: inst._getStageScale(),
            stageW: stage.width,
            wrapW: wrap.width,
            docOverflow: document.documentElement.scrollWidth - document.documentElement.clientWidth
        };
        host.style.width = '';
        await new Promise((r) => setTimeout(r, 200));
        return result;
    });
    assert(squeezed.scale < 1 && squeezed.stageW <= squeezed.wrapW + 1,
        `${ctxName} 容器变窄时舞台等比缩小且不溢出`,
        `scale=${squeezed.scale.toFixed(3)} 舞台 ${squeezed.stageW.toFixed(1)}px ≤ 容器 ${squeezed.wrapW.toFixed(1)}px`);
    assert(squeezed.docOverflow <= 0,
        `${ctxName} 窄容器下仍无横向滚动条`,
        `溢出 ${squeezed.docOverflow}px`);
}

// ====================================================================
// 主流程
// ====================================================================

async function runSuite(browser, profile) {
    console.log(`\n\x1b[36m${'='.repeat(70)}\x1b[0m`);
    console.log(`\x1b[36m${profile.title}\x1b[0m  (${profile.ctx.viewport.width}x${profile.ctx.viewport.height}, touch=${!!profile.ctx.hasTouch})`);
    console.log(`\x1b[36m${'='.repeat(70)}\x1b[0m`);

    const context = await browser.newContext(profile.ctx);
    const page = await context.newPage();

    const pageErrors = [];
    page.on('pageerror', (e) => pageErrors.push(e.message));
    page.on('console', (m) => {
        if (m.type() !== 'error') return;
        const text = m.text();
        // "Failed to load resource" 不带 URL，无从判断是否预期内，
        // 统一交给下面的 response 监听按 URL 精确判定
        if (/Failed to load resource/.test(text)) return;
        pageErrors.push('console: ' + text);
    });

    // 意外的失败请求（预期内的除外：旧路径探测必须 404，校验接口会返回 403/410）
    const EXPECTED_BAD = [
        /\/js\/jaravel-captcha\.js$/,      // testStaticAsset 主动探测陈旧副本是否已移除
        /\/api\/captcha\/verify$/          // 校验失败 403 / 防复用 410 属业务语义
    ];
    const badResponses = [];
    page.on('response', (res) => {
        if (res.status() < 400) return;
        const url = res.url();
        if (EXPECTED_BAD.some((re) => re.test(url))) return;
        badResponses.push(res.status() + ' ' + url);
    });

    await page.goto(DEMO, { waitUntil: 'domcontentloaded' });
    await page.waitForFunction(() => typeof window.Captcha === 'function', null, { timeout: 15000 });

    const tag = profile.tag;
    await testStaticAsset(page, tag);
    await testPermissionBoundary(page, tag);
    await testResponsiveStage(page, tag, profile.ctx.viewport);
    await testNumber(page, tag);
    await testSlider(page, tag, !!profile.ctx.hasTouch);
    await testRotate(page, tag, !!profile.ctx.hasTouch);
    await testClick(page, tag, !!profile.ctx.hasTouch);
    await testModal(page, tag, profile.ctx.viewport);

    // 截图存档
    await page.evaluate(() => {
        if (window.__jc && window.__jc.instance) { try { window.__jc.instance.destroy(); } catch (e) {} }
        const h = document.getElementById('e2e-host');
        if (h) h.style.display = 'none';
    });
    await page.evaluate(() => {
        const r = document.querySelector('input[name="jc-mode"][value="modal"]');
        if (r) { r.checked = true; r.dispatchEvent(new Event('change', { bubbles: true })); }
    });
    await page.click('#slider-start');
    await page.waitForTimeout(1500);
    await page.screenshot({ path: `e2e/shot-${profile.tag}-modal.png` });

    await page.keyboard.press('Escape');
    await page.evaluate(() => {
        const r = document.querySelector('input[name="jc-mode"][value="inline"]');
        if (r) { r.checked = true; r.dispatchEvent(new Event('change', { bubbles: true })); }
    });
    await page.click('#slider-start');
    await page.waitForTimeout(1200);
    await page.screenshot({ path: `e2e/shot-${profile.tag}-inline.png`, fullPage: false });

    assert(pageErrors.length === 0, `${tag} 运行期无 JS 报错`,
        pageErrors.length ? pageErrors.slice(0, 3).join(' | ') : '');
    assert(badResponses.length === 0, `${tag} 无意外的失败请求`,
        badResponses.length ? badResponses.slice(0, 3).join(' | ') : '');

    await context.close();
}

(async () => {
    const browser = await chromium.launch();

    const profiles = [
        {
            tag: 'desktop',
            title: '桌面端 · 鼠标事件',
            ctx: { viewport: { width: 1280, height: 900 }, hasTouch: false, isMobile: false }
        },
        {
            tag: 'mobile',
            title: '移动端 · 触摸事件（iPhone 12 尺寸）',
            ctx: {
                viewport: { width: 390, height: 844 },
                hasTouch: true,
                isMobile: true,
                deviceScaleFactor: 3,
                userAgent: devices['iPhone 12'].userAgent
            }
        },
        {
            tag: 'tablet',
            title: '平板端 · 触摸事件',
            ctx: { viewport: { width: 768, height: 1024 }, hasTouch: true, isMobile: true, deviceScaleFactor: 2 }
        }
    ];

    for (const p of profiles) {
        try {
            await runSuite(browser, p);
        } catch (e) {
            bad(`${p.tag} 套件异常中断`, e.message);
            console.error(e);
        }
    }

    await browser.close();

    console.log(`\n\x1b[36m${'='.repeat(70)}\x1b[0m`);
    console.log(`总计: \x1b[32m${passed} 通过\x1b[0m, ${failed > 0 ? '\x1b[31m' : ''}${failed} 失败\x1b[0m`);
    if (failures.length) {
        console.log('\n失败项：');
        failures.forEach((f) => console.log('  · ' + f));
    }
    console.log(`\x1b[36m${'='.repeat(70)}\x1b[0m`);
    process.exit(failed > 0 ? 1 : 0);
})();
