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
</style>
@endsection

@section('content')
<div class="mdui-container">
    <h2 class="mdui-text-center">验证码演示</h2>
    <p class="mdui-text-center mdui-text-color-theme-secondary">OOP API — Captcha.init(containerId, options) 一行代码集成</p>

    <!-- OOP API 说明 -->
    <div class="mdui-card captcha-card encryption-doc mdui-m-b-2">
        <div class="mdui-card-primary">
            <div class="mdui-card-primary-title">
                <i class="mdui-icon material-icons" style="font-size:20px;vertical-align:middle;">lock</i>
                OOP API 使用方式
            </div>
        </div>
        <div class="mdui-card-content mdui-typography">
            <p><strong>一行代码初始化：</strong><code>Captcha.init('divId', {type:'number', onSuccess:fn, onFail:fn})</code></p>
            <p><strong>返回对象方法：</strong><code>show()</code> / <code>hide()</code> / <code>refresh()</code> / <code>verify()</code> / <code>destroy()</code></p>
            <p><strong>回调：</strong><code>onSuccess(captchaKey, captchaInput)</code> / <code>onFail()</code> / <code>onComplete(captchaKey, captchaInput)</code></p>
            <p><strong>加密模式：</strong>none / aes / rsa（Web Crypto 不可用时自动降级为 none）</p>
            <p><strong>防复用：</strong>同一 captchaKey 验证成功后即失效，不可重复使用</p>
        </div>
    </div>

    <!-- 1. 数字验证码 -->
    <div class="mdui-card captcha-card">
        <div class="mdui-card-primary">
            <div class="mdui-card-primary-title">1. 数字验证码</div>
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
            <div class="mdui-card-primary-title">2. 算术验证码</div>
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
            <div class="mdui-card-primary-subtitle">拖动滑块将拼图块滑入缺口位置，松开自动验证</div>
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
            <div class="mdui-card-primary-subtitle">拖动滑块将圆盘旋转至图案对齐，松开自动验证</div>
        </div>
        <div class="mdui-card-content">
            <div id="rotate-container" class="captcha-container" style="display:none;"></div>
            <div id="rotate-result"></div>
            <button id="rotate-start" class="mdui-btn mdui-btn-raised mdui-ripple mdui-color-theme mdui-m-t-2" onclick="startCaptcha('rotate')">
                <i class="mdui-icon material-icons">rotate_right</i> 开始旋转验证
            </button>
        </div>
    </div>

    <!-- 5. 文字点选验证码 -->
    <div class="mdui-card captcha-card">
        <div class="mdui-card-primary">
            <div class="mdui-card-primary-title">5. 文字点选验证码</div>
            <div class="mdui-card-primary-subtitle">按提示顺序依次点击图中的文字，点击足够数量自动验证</div>
        </div>
        <div class="mdui-card-content">
            <div id="click-container" class="captcha-container" style="display:none;"></div>
            <div id="click-result"></div>
            <button id="click-start" class="mdui-btn mdui-btn-raised mdui-ripple mdui-color-theme mdui-m-t-2" onclick="startCaptcha('click')">
                <i class="mdui-icon material-icons">touch_app</i> 开始文字点选验证
            </button>
        </div>
    </div>

    <!-- 配置项说明 -->
    <div class="mdui-card captcha-card api-section mdui-m-b-4">
        <div class="mdui-card-primary">
            <div class="mdui-card-primary-title">
                <i class="mdui-icon material-icons" style="font-size:20px;vertical-align:middle;">settings</i>
                验证码模块可配置项（application.yml）
            </div>
        </div>
        <div class="mdui-card-content mdui-typography">
            <p><code>jaravel.captcha.enabled</code> — 是否启用验证码（默认 true）</p>
            <p><code>jaravel.captcha.width</code> — 图片宽度（默认 300）</p>
            <p><code>jaravel.captcha.height</code> — 图片高度（默认 150）</p>
            <p><code>jaravel.captcha.length</code> — 数字/算术验证码字符数量（默认 4）</p>
            <p><code>jaravel.captcha.expire-seconds</code> — 验证码过期时间，秒（默认 300 = 5分钟）</p>
            <p><code>jaravel.captcha.tolerance</code> — 滑动/旋转容差像素/角度（默认 8.0）</p>
            <p><code>jaravel.captcha.trajectory-enabled</code> — 是否启用轨迹验证（默认 true）</p>
            <p><code>jaravel.captcha.interference-level</code> — 干扰强度级别 1-5（默认 3）</p>
            <p><code>jaravel.captcha.click-target-count</code> — 需要点选的目标文字数量（默认 3）</p>
            <p><code>jaravel.captcha.click-decoy-count</code> — 干扰文字数量（默认 3）</p>
            <p><code>jaravel.captcha.encryption-type</code> — 加密模式 none/aes/rsa（默认 none）</p>
            <p><code>jaravel.captcha.encryption-key</code> — 加密密钥（AES: 对称密钥; RSA: Base64公钥）</p>
            <p><code>jaravel.captcha.background-images</code> — 自定义背景图路径列表（滑动/旋转/点选验证码）</p>
        </div>
    </div>
</div>
@endsection

@section('scripts')
<script>
// ====================================================================
// OOP API 演示 — 每种验证码只需一个 startCaptcha() 调用
// ====================================================================
var instances = {};

function startCaptcha(type) {
    // 销毁旧实例
    if (instances[type]) {
        instances[type].destroy();
    }

    // 隐藏开始按钮，显示容器
    document.getElementById(type + '-start').style.display = 'none';
    document.getElementById(type + '-container').style.display = 'block';
    document.getElementById(type + '-result').innerHTML = '';

    // 一行代码初始化验证码
    instances[type] = Captcha.init(type + '-container', {
        type: type,
        encryptionType: 'aes',
        encryptionKey: 'jaravel-captcha-default-key',
        onSuccess: function(captchaKey, captchaInput) {
            document.getElementById(type + '-result').innerHTML =
                '<div class="result-chip result-ok"><i class="mdui-icon material-icons">check_circle</i> 验证通过</div>';
        },
        onFail: function() {
            document.getElementById(type + '-result').innerHTML =
                '<div class="result-chip result-fail"><i class="mdui-icon material-icons">cancel</i> 验证失败，请重试</div>';
        }
    });

    // 构造函数已自动加载验证码，不需要再调 show()/refresh()
    instances[type].show();
}
</script>
@endsection
