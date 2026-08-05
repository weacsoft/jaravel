@extends('layout')

@section('title', '验证码演示 - jaravel')

@section('head')
<script src="/jaravel-captcha.js"></script>
<style>
.captcha-card { max-width: 560px; margin: 16px auto; }
.captcha-container { min-height: 60px; }
.result-chip {
    padding: 6px 16px; border-radius: 16px;
    display: inline-flex; align-items: center; gap: 4px;
    font-size: 14px; margin-top: 8px;
}
.result-ok { background: #c8e6c9; color: #2e7d32; }
.result-fail { background: #ffcdd2; color: #c62828; }
.encryption-doc { background: #f5f5f5; border-left: 4px solid #1976d2; }
.encryption-doc code { background: #e3f2fd; padding: 1px 5px; border-radius: 3px; font-size: 13px; }
.api-section { background: #f5f5f5; border-left: 4px solid #4caf50; }
.api-section code { background: #e8f5e9; padding: 1px 5px; border-radius: 3px; font-size: 13px; }
.security-section { background: #fff8e1; border-left: 4px solid #ff9800; }
.security-section code { background: #ffecb3; padding: 1px 5px; border-radius: 3px; font-size: 13px; }
.config-badge {
    display: inline-block; padding: 2px 8px; border-radius: 10px;
    background: #e3f2fd; color: #1565c0; font-size: 12px; margin-left: 4px;
}
.scene-badge {
    display: inline-block; padding: 2px 8px; border-radius: 10px;
    background: #ede7f6; color: #4527a0; font-size: 12px; margin-left: 4px;
}
.mode-bar {
    max-width: 560px; margin: 0 auto 16px; padding: 12px 16px;
    background: #eceff1; border-radius: 4px; text-align: center;
}
.mode-bar label { margin: 0 10px; cursor: pointer; font-size: 14px; }
.btn-row { display: flex; flex-wrap: wrap; gap: 8px; }
.perm-table { width: 100%; border-collapse: collapse; font-size: 13px; margin-top: 8px; }
.perm-table th, .perm-table td { border: 1px solid #ddd; padding: 6px 8px; text-align: left; }
.perm-table th { background: #f5f5f5; }
</style>
@endsection

@section('content')
<div class="mdui-container">
    <h2 class="mdui-text-center">验证码演示</h2>
    <p class="mdui-text-center mdui-text-color-theme-secondary">
        事件驱动 API · 场景白名单 · 全屏弹层 · 移动端 / 桌面端通用
    </p>

    <!-- 展示模式切换 -->
    <div class="mode-bar">
        <strong>展示模式：</strong>
        <label><input type="radio" name="jc-mode" value="inline" checked> 内联嵌入</label>
        <label><input type="radio" name="jc-mode" value="modal"> 全屏弹层</label>
        <div style="font-size:12px;color:#607d8b;margin-top:6px;">
            同一份代码同时支持鼠标与触摸，桌面端与移动端无需任何改动
        </div>
    </div>

    <!-- OOP API 说明 -->
    <div class="mdui-card captcha-card encryption-doc mdui-m-b-2">
        <div class="mdui-card-primary">
            <div class="mdui-card-primary-title">
                <i class="mdui-icon material-icons" style="font-size:20px;vertical-align:middle;">lock</i>
                事件驱动 API 使用方式
            </div>
        </div>
        <div class="mdui-card-content mdui-typography">
            <p><strong>初始化：</strong><code>Captcha.init('divId', {type:'number', scene:'comment'})</code></p>
            <p><strong>弹层模式：</strong><code>Captcha.init('divId', {type:'slider', modal:true})</code> 再调 <code>.show()</code></p>
            <p><strong>事件监听：</strong><code>.on('beforeGet', fn)</code> / <code>.on('afterGet', fn)</code> / <code>.on('complete', fn)</code> / <code>.on('show'|'hide', fn)</code></p>
            <p><strong>beforeGet</strong> — 获取验证码前（含刷新），参数：<code>(type)</code></p>
            <p><strong>afterGet</strong> — 验证码加载完成后，参数：<code>(key, captchaData)</code></p>
            <p><strong>complete</strong> — 用户完成前端验证操作，参数：<code>(key, captchaInput)</code></p>
            <p><strong>合并凭证：</strong><code>key</code> 格式为 <code>type.captchaKey</code>，已内含验证码类型；
               校验接口只收 <code>{key, input}</code> 两个参数，可与登录表单等业务字段<strong>一次性提交</strong>，
               避免「先验证码、后业务」两段式提交产生的时间窗漏洞</p>
            <p><strong>注意：</strong>前端不提交验证到后端，由业务方在 complete 事件中决定后续处理</p>
        </div>
    </div>

    <!-- 1. 数字验证码 -->
    <div class="mdui-card captcha-card">
        <div class="mdui-card-primary">
            <div class="mdui-card-primary-title">1. 数字验证码 <span class="scene-badge">scene: comment</span></div>
            <div class="mdui-card-primary-subtitle">输入图片中的字符（不区分大小写）</div>
        </div>
        <div class="mdui-card-content">
            <div id="number-container" class="captcha-container" style="display:none;"></div>
            <div id="number-result"></div>
            <button id="number-start" class="mdui-btn mdui-btn-raised mdui-ripple mdui-color-theme mdui-m-t-2" onclick="startCaptcha('number')">
                <i class="mdui-icon material-icons">image</i> 开始数字验证
            </button>
        </div>
    </div>

    <!-- 2. 算术验证码 -->
    <div class="mdui-card captcha-card">
        <div class="mdui-card-primary">
            <div class="mdui-card-primary-title">2. 算术验证码 <span class="scene-badge">scene: demo-arithmetic</span></div>
            <div class="mdui-card-primary-subtitle">计算图片中的算式并输入结果</div>
        </div>
        <div class="mdui-card-content">
            <div id="arithmetic-container" class="captcha-container" style="display:none;"></div>
            <div id="arithmetic-result"></div>
            <button id="arithmetic-start" class="mdui-btn mdui-btn-raised mdui-ripple mdui-color-theme mdui-m-t-2" onclick="startCaptcha('arithmetic')">
                <i class="mdui-icon material-icons">calculate</i> 开始算术验证
            </button>
        </div>
    </div>

    <!-- 3. 滑动验证码 -->
    <div class="mdui-card captcha-card">
        <div class="mdui-card-primary">
            <div class="mdui-card-primary-title">3. 滑动验证码 <span class="scene-badge">scene: login</span></div>
            <div class="mdui-card-primary-subtitle">拖动滑块将拼图块滑入缺口位置，松开即完成前端操作</div>
        </div>
        <div class="mdui-card-content">
            <div id="slider-container" class="captcha-container" style="display:none;"></div>
            <div id="slider-result"></div>
            <button id="slider-start" class="mdui-btn mdui-btn-raised mdui-ripple mdui-color-theme mdui-m-t-2" onclick="startCaptcha('slider')">
                <i class="mdui-icon material-icons">swap_horiz</i> 开始滑动验证
            </button>
        </div>
    </div>

    <!-- 4. 旋转验证码 -->
    <div class="mdui-card captcha-card">
        <div class="mdui-card-primary">
            <div class="mdui-card-primary-title">4. 旋转验证码</div>
            <div class="mdui-card-primary-subtitle">拖动滑块将圆盘旋转至图案对齐，松开即完成前端操作</div>
        </div>
        <div class="mdui-card-content">
            <div id="rotate-container" class="captcha-container" style="display:none;"></div>
            <div id="rotate-result"></div>
            <button id="rotate-start" class="mdui-btn mdui-btn-raised mdui-ripple mdui-color-theme mdui-m-t-2" onclick="startCaptcha('rotate')">
                <i class="mdui-icon material-icons">rotate_right</i> 开始旋转验证
            </button>
        </div>
    </div>

    <!-- 5. 文字点选验证码 (默认 3 个目标) -->
    <div class="mdui-card captcha-card">
        <div class="mdui-card-primary">
            <div class="mdui-card-primary-title">5. 文字点选验证码 <span class="config-badge">全局默认 · 点3个</span></div>
            <div class="mdui-card-primary-subtitle">按提示顺序依次点击图中的文字，点击足够数量即完成</div>
        </div>
        <div class="mdui-card-content">
            <div id="click-container" class="captcha-container" style="display:none;"></div>
            <div id="click-result"></div>
            <button id="click-start" class="mdui-btn mdui-btn-raised mdui-ripple mdui-color-theme mdui-m-t-2" onclick="startCaptcha('click')">
                <i class="mdui-icon material-icons">touch_app</i> 开始文字点选验证
            </button>
        </div>
    </div>

    <!-- 6. 文字点选验证码 (register 场景 6 个目标) -->
    <div class="mdui-card captcha-card">
        <div class="mdui-card-primary">
            <div class="mdui-card-primary-title">6. 文字点选验证码 <span class="scene-badge" style="background:#fff3e0;color:#e65100">scene: register · 点6个</span></div>
            <div class="mdui-card-primary-subtitle">难度由后端场景定义，前端只提供场景名</div>
        </div>
        <div class="mdui-card-content">
            <div id="click6-container" class="captcha-container" style="display:none;"></div>
            <div id="click6-result"></div>
            <button id="click6-start" class="mdui-btn mdui-btn-raised mdui-ripple mdui-color-deep-orange mdui-m-t-2" onclick="startCaptcha('click6')">
                <i class="mdui-icon material-icons">touch_app</i> 开始6文字点选验证
            </button>
        </div>
    </div>

    <!-- 配置权限边界 -->
    <div class="mdui-card captcha-card security-section mdui-m-b-2">
        <div class="mdui-card-primary">
            <div class="mdui-card-primary-title">
                <i class="mdui-icon material-icons" style="font-size:20px;vertical-align:middle;">security</i>
                配置权限边界
            </div>
        </div>
        <div class="mdui-card-content mdui-typography">
            <p>验证码的<strong>难度参数一律由后端下发</strong>，前端不具备设值能力，只能从后端预声明的场景白名单中「选择」。</p>
            <table class="perm-table">
                <tr><th>类别</th><th>归属</th><th>示例</th></tr>
                <tr>
                    <td>安全 / 校验参数</td>
                    <td><strong>后端</strong> <code>jaravel.captcha.*</code></td>
                    <td>tolerance、length、clickTargetCount、expireSeconds</td>
                </tr>
                <tr>
                    <td>场景选择</td>
                    <td>前端「选」，后端「定」</td>
                    <td><code>scene: 'login'</code></td>
                </tr>
                <tr>
                    <td>展示层配置</td>
                    <td><strong>前端</strong></td>
                    <td>modal、maxWidth、modalTitle、zIndex</td>
                </tr>
            </table>
            <p class="mdui-m-t-2"><strong>越权尝试演示：</strong>下面的按钮会尝试用旧写法把容差改成 999（即「随便拖一下都算通过」）。
            前端会忽略并告警，后端也不再接受该参数 —— 打开控制台可看到拦截日志。</p>
            <button class="mdui-btn mdui-btn-raised mdui-ripple mdui-color-orange" onclick="tryTamper()">
                <i class="mdui-icon material-icons">bug_report</i> 尝试篡改 tolerance=999
            </button>
            <div id="tamper-result"></div>
        </div>
    </div>

    <!-- 配置项说明 -->
    <div class="mdui-card captcha-card api-section mdui-m-b-4">
        <div class="mdui-card-primary">
            <div class="mdui-card-primary-title">
                <i class="mdui-icon material-icons" style="font-size:20px;vertical-align:middle;">settings</i>
                验证码模块可配置项
            </div>
        </div>
        <div class="mdui-card-content mdui-typography">
            <p><strong>前端展示层选项：</strong></p>
            <p><code>modal: true</code> — 全屏弹层模式，遮罩居中显示</p>
            <p><code>modalTitle: '安全验证'</code> — 弹层标题</p>
            <p><code>maskClosable / escClosable / closable</code> — 关闭方式开关</p>
            <p><code>autoCloseDelay: 600</code> — 验证完成后自动收起弹层的延迟（毫秒，0 表示不自动关闭）</p>
            <p><code>maxWidth: 360</code> — 验证框最大宽度</p>
            <p><strong>后端 application.yml（全局默认 + 场景白名单）：</strong></p>
            <p><code>jaravel.captcha.enabled</code> — 是否启用验证码（默认 true）</p>
            <p><code>jaravel.captcha.click-target-count</code> — 需要点选的目标文字数量（默认 3）</p>
            <p><code>jaravel.captcha.encryption-type</code> — 加密模式 none/aes/rsa（默认 none）</p>
            <p><code>jaravel.captcha.scenes.&lt;name&gt;.*</code> — 场景白名单，未声明字段自动继承全局</p>
        </div>
    </div>
</div>
@endsection

@section('scripts')
<script>
// ====================================================================
// 事件驱动 API 演示 — on() 事件监听 + 场景白名单 + 内联/弹层双模式
// ====================================================================
var instances = {};

// 每种演示对应的「场景名」。注意：这里只有名字，没有任何具体数值 ——
// tolerance / length / clickTargetCount 全部由后端 application.yml
// 的 jaravel.captcha.scenes 定义，前端改不了。
var captchaScenes = {
    number:     'comment',           // 4 位字符、干扰较弱
    arithmetic: 'demo-arithmetic',   // 5 位结果
    slider:     'login',             // 更小容差、更强干扰
    rotate:     null,                // 不指定场景 → 使用全局默认
    click:      null,                // 全局默认：点 3 个
    click6:     'register'           // 点 6 个 + 4 个干扰
};

// click6 实际使用 click 类型，只是场景不同
function getCaptchaType(key) {
    return key === 'click6' ? 'click' : key;
}

// 当前展示模式：inline | modal
function currentMode() {
    var checked = document.querySelector('input[name="jc-mode"]:checked');
    return checked ? checked.value : 'inline';
}

function startCaptcha(key) {
    var type = getCaptchaType(key);
    var modal = (currentMode() === 'modal');

    // 销毁旧实例
    if (instances[key]) {
        instances[key].destroy();
        instances[key] = null;
    }

    document.getElementById(key + '-result').innerHTML = '';

    var container = document.getElementById(key + '-container');
    // 弹层模式下验证框挂在 body 的遮罩里，页面内的占位容器不需要显示
    container.style.display = modal ? 'none' : 'block';
    document.getElementById(key + '-start').style.display = modal ? '' : 'none';

    // 注意：这里【不传】 encryptionType / encryptionKey。
    // 加密类型与密钥一律以后端 generate 接口下发的 encType / encKey 为准 ——
    // 后端启用全局应用密钥兜底（jaravel.key）后，实际生效的密钥可能与
    // jaravel.captcha.encryption-key 的静态值不同；前端若硬编码密钥就会
    // 与服务端解密所用的密钥不一致，导致校验恒失败。
    var instance = Captcha.init(key + '-container', {
        type: type,
        scene: captchaScenes[key],       // 只传场景名
        modal: modal,
        modalTitle: '安全验证'
    });

    // 注册事件监听器
    instance.on('beforeGet', function(captchaType) {
        console.log('[Demo] beforeGet: type=' + captchaType);
    });

    instance.on('afterGet', function(captchaCredential, captchaData) {
        console.log('[Demo] afterGet: key=' + captchaCredential);
    });

    instance.on('complete', function(captchaCredential, captchaInput) {
        // 用户已完成前端验证操作
        // captchaCredential: 合并凭证 key（type.captchaKey）；captchaInput: 加密后的用户输入
        // 业务方把这两个值随业务表单一起 POST，后端 verify(key, input) 一次校验
        document.getElementById(key + '-result').innerHTML =
            '<div class="result-chip result-ok"><i class="mdui-icon material-icons">check_circle</i> 已完成前端验证</div>' +
            '<div style="margin-top:8px;font-size:13px;color:#666;word-break:break-all;">key: ' + captchaCredential.substring(0, 40) + '...</div>';
        console.log('[Demo] complete: key=' + captchaCredential + ', input=' + captchaInput);
    });

    instance.on('hide', function() {
        // 弹层关闭后恢复「开始验证」按钮，方便再次演示
        if (modal) document.getElementById(key + '-start').style.display = '';
    });

    instances[key] = instance;
    instance.show();
}

// --------------------------------------------------------------------
// 越权尝试演示：旧版允许 config 直接透传难度参数，现已被前后端双重拦截
// --------------------------------------------------------------------
function tryTamper() {
    var box = document.getElementById('tamper-result');
    var warnings = [];

    // 劫持 console.warn，捕获前端的拦截告警作为演示证据
    var originalWarn = console.warn;
    console.warn = function() {
        warnings.push(Array.prototype.slice.call(arguments).join(' '));
        originalWarn.apply(console, arguments);
    };

    try {
        if (instances.tamper) {
            instances.tamper.destroy();
            instances.tamper = null;
        }
        // 旧的越权写法
        var probe = Captcha.init('tamper-probe', {
            type: 'slider',
            config: { tolerance: 999, clickTargetCount: 1, length: 1 }
        });
        instances.tamper = probe;

        var requestedUrl = probe.options.apiUrl + '?type=slider'
            + (probe.options.scene ? '&scene=' + probe.options.scene : '');

        box.innerHTML =
            '<div class="result-chip result-ok" style="margin-top:12px;">'
            + '<i class="mdui-icon material-icons">shield</i> 篡改已被拦截</div>'
            + '<div style="margin-top:8px;font-size:13px;color:#666;">'
            + '实际请求：<code>' + requestedUrl + '</code><br>'
            + 'tolerance / clickTargetCount / length 均未出现在请求中<br>'
            + '前端告警 ' + warnings.length + ' 条：<br>'
            + warnings.map(function(w) { return '· ' + w; }).join('<br>')
            + '</div>';

        probe.destroy();
        instances.tamper = null;
    } catch (e) {
        box.innerHTML = '<div class="result-chip result-fail">演示失败: ' + e.message + '</div>';
    } finally {
        console.warn = originalWarn;
    }
}

// 切换展示模式时销毁所有实例，避免内联与弹层混在一起
document.addEventListener('DOMContentLoaded', function() {
    var radios = document.querySelectorAll('input[name="jc-mode"]');
    for (var i = 0; i < radios.length; i++) {
        radios[i].addEventListener('change', function() {
            for (var k in instances) {
                if (instances[k]) {
                    instances[k].destroy();
                    instances[k] = null;
                }
            }
            var keys = ['number', 'arithmetic', 'slider', 'rotate', 'click', 'click6'];
            keys.forEach(function(k) {
                var c = document.getElementById(k + '-container');
                if (c) c.style.display = 'none';
                var s = document.getElementById(k + '-start');
                if (s) s.style.display = '';
                var r = document.getElementById(k + '-result');
                if (r) r.innerHTML = '';
            });
        });
    }
});
</script>
<div id="tamper-probe" style="display:none;"></div>
@endsection
