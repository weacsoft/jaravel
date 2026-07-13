@extends('layout')

@section('title', '验证码演示 - jaravel')

@section('head')
<script src="/js/jaravel-captcha.js"></script>
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
.config-badge {
    display: inline-block; padding: 2px 8px; border-radius: 10px;
    background: #e3f2fd; color: #1565c0; font-size: 12px; margin-left: 4px;
}
</style>
@endsection

@section('content')
<div class="mdui-container">
    <h2 class="mdui-text-center">验证码演示</h2>
    <p class="mdui-text-center mdui-text-color-theme-secondary">OOP 事件驱动 API — on() 事件监听 + config 差异化配置</p>

    <!-- OOP API 说明 -->
    <div class="mdui-card captcha-card encryption-doc mdui-m-b-2">
        <div class="mdui-card-primary">
            <div class="mdui-card-primary-title">
                <i class="mdui-icon material-icons" style="font-size:20px;vertical-align:middle;">lock</i>
                事件驱动 API 使用方式
            </div>
        </div>
        <div class="mdui-card-content mdui-typography">
            <p><strong>初始化：</strong><code>Captcha.init('divId', {type:'number', config:{length:6}})</code></p>
            <p><strong>事件监听：</strong><code>.on('beforeGet', fn)</code> / <code>.on('afterGet', fn)</code> / <code>.on('complete', fn)</code></p>
            <p><strong>beforeGet</strong> — 获取验证码前（含刷新），参数：<code>(type)</code></p>
            <p><strong>afterGet</strong> — 验证码加载完成后，参数：<code>(captchaKey, captchaData)</code></p>
            <p><strong>complete</strong> — 用户完成前端验证操作，参数：<code>(captchaKey, captchaInput, rawInput)</code></p>
            <p><strong>config 差异化：</strong>通过 <code>config: {clickTargetCount: 6, length: 6}</code> 实现 per-instance 配置覆盖</p>
            <p><strong>注意：</strong>前端不提交验证到后端，由业务方在 complete 事件中决定后续处理</p>
        </div>
    </div>

    <!-- 1. 数字验证码 -->
    <div class="mdui-card captcha-card">
        <div class="mdui-card-primary">
            <div class="mdui-card-primary-title">1. 数字验证码 <span class="config-badge">4位字符</span></div>
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
            <div class="mdui-card-primary-title">2. 算术验证码 <span class="config-badge">5位结果</span></div>
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
            <div class="mdui-card-primary-title">3. 滑动验证码</div>
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

    <!-- 5. 文字点选验证码 (3个目标) -->
    <div class="mdui-card captcha-card">
        <div class="mdui-card-primary">
            <div class="mdui-card-primary-title">5. 文字点选验证码 <span class="config-badge">点3个文字</span></div>
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

    <!-- 6. 文字点选验证码 (6个目标 - 差异化配置演示) -->
    <div class="mdui-card captcha-card">
        <div class="mdui-card-primary">
            <div class="mdui-card-primary-title">6. 文字点选验证码 <span class="config-badge" style="background:#fff3e0;color:#e65100">点6个文字</span></div>
            <div class="mdui-card-primary-subtitle">差异化配置：config: {clickTargetCount: 6, clickDecoyCount: 4}</div>
        </div>
        <div class="mdui-card-content">
            <div id="click6-container" class="captcha-container" style="display:none;"></div>
            <div id="click6-result"></div>
            <button id="click6-start" class="mdui-btn mdui-btn-raised mdui-ripple mdui-color-deep-orange mdui-m-t-2" onclick="startCaptcha('click6')">
                <i class="mdui-icon material-icons">touch_app</i> 开始6文字点选验证
            </button>
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
            <p><strong>前端 config（per-instance 覆盖）：</strong></p>
            <p><code>config: {clickTargetCount: 6}</code> — 点选目标数量</p>
            <p><code>config: {clickDecoyCount: 4}</code> — 干扰文字数量</p>
            <p><code>config: {length: 6}</code> — 数字/算术字符数量</p>
            <p><code>config: {width: 300, height: 150}</code> — 图片尺寸</p>
            <p><strong>后端 application.yml（全局默认）：</strong></p>
            <p><code>jaravel.captcha.enabled</code> — 是否启用验证码（默认 true）</p>
            <p><code>jaravel.captcha.click-target-count</code> — 需要点选的目标文字数量（默认 3）</p>
            <p><code>jaravel.captcha.encryption-type</code> — 加密模式 none/aes/rsa（默认 none）</p>
            <p><code>jaravel.captcha.encryption-key</code> — 加密密钥</p>
        </div>
    </div>
</div>
@endsection

@section('scripts')
<script>
// ====================================================================
// 事件驱动 API 演示 — on() 事件监听 + config 差异化配置
// ====================================================================
var instances = {};

// 每种验证码的差异化配置
var captchaConfigs = {
    number:     { length: 4 },
    arithmetic: { length: 5 },
    slider:     {},
    rotate:     {},
    click:      { clickTargetCount: 3, clickDecoyCount: 3 },
    click6:     { clickTargetCount: 6, clickDecoyCount: 4 }
};

// click6 实际使用 click 类型，但配置不同
function getCaptchaType(key) {
    return key === 'click6' ? 'click' : key;
}

function startCaptcha(key) {
    var type = getCaptchaType(key);

    // 销毁旧实例
    if (instances[key]) {
        instances[key].destroy();
    }

    // 隐藏开始按钮，显示容器
    document.getElementById(key + '-start').style.display = 'none';
    document.getElementById(key + '-container').style.display = 'block';
    document.getElementById(key + '-result').innerHTML = '';

    // 初始化验证码，传入 config 差异化配置
    var instance = Captcha.init(key + '-container', {
        type: type,
        encryptionType: 'aes',
        encryptionKey: 'jaravel-captcha-default-key',
        config: captchaConfigs[key]
    });

    // 注册事件监听器
    instance.on('beforeGet', function(captchaType) {
        console.log('[Demo] beforeGet: type=' + captchaType);
    });

    instance.on('afterGet', function(captchaKey, captchaData) {
        console.log('[Demo] afterGet: key=' + captchaKey);
    });

    instance.on('complete', function(captchaKey, captchaInput) {
        // 用户已完成前端验证操作
        // captchaKey: 验证码标识
        // captchaInput: 加密后的用户输入
        document.getElementById(key + '-result').innerHTML =
            '<div class="result-chip result-ok"><i class="mdui-icon material-icons">check_circle</i> 已完成前端验证</div>' +
            '<div style="margin-top:8px;font-size:13px;color:#666;">captchaKey: ' + captchaKey.substring(0, 32) + '...</div>';
        console.log('[Demo] complete: key=' + captchaKey + ', input=' + captchaInput);
    });

    instances[key] = instance;
    instance.show();
}
</script>
@endsection
