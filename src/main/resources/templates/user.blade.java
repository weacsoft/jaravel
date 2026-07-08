@extends('layout')

{{-- 页面标题 --}}
@section('title')
{{ $title ?? 'jaravel 用户控制台 - 插件执行平台' }}
@endsection

{{-- 页面专属样式 --}}
@section('head')
<style>
    /* ===== 登录 / 注册视图 ===== */
    .login-wrap {
        display: flex;
        justify-content: center;
        align-items: center;
        min-height: 70vh;
        padding: 24px 0;
    }
    .login-card {
        width: 100%;
        max-width: 440px;
        padding: 32px;
    }
    .brand-mark { text-align: center; margin-bottom: 16px; }
    .brand-mark .logo {
        display: inline-flex;
        align-items: center;
        justify-content: center;
        width: 56px; height: 56px;
        border-radius: 14px;
        background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
        color: #fff;
        font-size: 26px;
        font-weight: 700;
        box-shadow: 0 8px 20px rgba(102,126,234,.4);
    }
    .login-title { text-align: center; margin: 0 0 4px; }
    .login-subtitle { text-align: center; color: var(--mdui-color-on-surface-variant); margin: 0 0 20px; font-size: 14px; }
    .login-tabs { display: flex; gap: 8px; margin-bottom: 20px; }
    .login-tabs mdui-button { flex: 1; }
    .btn-block { width: 100%; }

    /* ===== 用户信息栏 ===== */
    .user-bar {
        display: flex;
        justify-content: space-between;
        align-items: center;
        padding: 12px 0;
        border-bottom: 1px solid var(--mdui-color-outline-variant);
        margin-bottom: 20px;
        flex-wrap: wrap;
        gap: 12px;
    }
    .user-info { display: flex; align-items: center; gap: 12px; }
    .avatar {
        width: 40px; height: 40px;
        border-radius: 50%;
        background: var(--mdui-color-primary);
        color: var(--mdui-color-on-primary);
        display: flex;
        align-items: center;
        justify-content: center;
        font-weight: 600;
        font-size: 16px;
    }
    .user-meta .uname { font-weight: 600; font-size: 14px; }
    .user-meta .unum { font-size: 12px; color: var(--mdui-color-on-surface-variant); }

    /* ===== 区块导航 ===== */
    .section-nav { display: flex; gap: 8px; margin-bottom: 20px; flex-wrap: wrap; }

    /* ===== 内容卡片 ===== */
    .content-card { padding: 20px; margin-bottom: 20px; }
    .card-header {
        display: flex;
        justify-content: space-between;
        align-items: center;
        flex-wrap: wrap;
        gap: 12px;
        margin-bottom: 16px;
    }
    .card-header .section-title { margin: 0; }

    /* ===== Java 代码编辑器 ===== */
    .editor-wrap {
        border: 1px solid var(--mdui-color-outline-variant);
        border-radius: 12px;
        overflow: hidden;
    }
    .editor-bar {
        display: flex;
        justify-content: space-between;
        align-items: center;
        padding: 8px 14px;
        background: var(--mdui-color-surface-variant);
        font-size: 12px;
        color: var(--mdui-color-on-surface-variant);
    }
    #javaCode {
        width: 100%;
        min-height: 280px;
        border: none;
        border-radius: 0;
        font-family: 'Consolas', 'Monaco', 'Courier New', monospace;
        font-size: 14px;
        line-height: 1.6;
        padding: 16px;
        background: #1e293b;
        color: #e2e8f0;
        resize: vertical;
        box-sizing: border-box;
    }
    #javaCode:focus { outline: none; }

    /* ===== 结果区 ===== */
    .result-meta {
        display: flex;
        flex-wrap: wrap;
        gap: 8px 20px;
        align-items: center;
        margin-bottom: 12px;
        font-size: 13px;
        color: var(--mdui-color-on-surface-variant);
    }
    .result-meta b { color: var(--mdui-color-on-surface); font-weight: 600; }
    .result-label {
        font-size: 13px;
        font-weight: 600;
        color: var(--mdui-color-on-surface-variant);
        margin: 14px 0 6px;
        text-transform: uppercase;
        letter-spacing: .5px;
    }

    /* ===== Jar 表单网格 ===== */
    .jar-form-grid {
        display: grid;
        grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
        gap: 0 16px;
    }

    /* ===== 状态模块 ===== */
    .status-grid {
        display: grid;
        grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
        gap: 20px;
    }
    .stat-row {
        display: flex;
        justify-content: space-between;
        align-items: center;
        padding: 10px 0;
        border-bottom: 1px dashed var(--mdui-color-outline-variant);
        font-size: 14px;
    }
    .stat-row:last-child { border-bottom: none; }
    .stat-row .k { color: var(--mdui-color-on-surface-variant); }
    .stat-row .v { font-weight: 600; }

    /* ===== 提示框 ===== */
    .alert {
        padding: 10px 14px;
        border-radius: 8px;
        font-size: 14px;
        margin-bottom: 8px;
    }
    .alert-error { background: var(--mdui-color-error-container); color: var(--mdui-color-on-error-container); }
    .alert-info { background: var(--mdui-color-primary-container); color: var(--mdui-color-on-primary-container); }

    /* ===== 加载与空状态 ===== */
    .loading {
        display: inline-flex;
        align-items: center;
        gap: 8px;
        font-size: 13px;
        color: var(--mdui-color-on-surface-variant);
        padding: 24px 0;
    }
    .empty {
        text-align: center;
        color: var(--mdui-color-on-surface-variant);
        font-size: 13px;
        padding: 24px 0;
    }
    .table-wrap { overflow-x: auto; }
    .plugin-table { width: 100%; border-collapse: collapse; margin-top: 12px; }
    .plugin-table th, .plugin-table td {
        text-align: left;
        padding: 10px 12px;
        border-bottom: 1px solid var(--mdui-color-outline-variant);
        font-size: 13px;
    }
    .plugin-table th { font-weight: 500; color: var(--mdui-color-on-surface-variant); }
</style>
@endsection

{{-- 页面主体内容 --}}
@section('content')
{{-- 页面标识，供前端识别当前页 --}}
<div id="pageFlag" data-page="{{ $page ?? 'user' }}" class="hidden"></div>

{{-- ============ 登录 / 注册视图 ============ --}}
<div id="loginView" class="login-wrap">
    <mdui-card variant="outlined" class="login-card">
        <div class="brand-mark">
            <div class="logo">J</div>
        </div>
        <h2 class="login-title">{{ $appName ?? 'jaravel' }} 用户控制台</h2>
        <p class="login-subtitle">多租户 Jar/Java 插件运行平台</p>

        {{-- 登录 / 注册 切换 --}}
        <div class="login-tabs">
            <mdui-button variant="tonal" class="login-tab active" data-tab="login">登录</mdui-button>
            <mdui-button variant="text" class="login-tab" data-tab="register">注册</mdui-button>
        </div>

        {{-- 登录表单 --}}
        <form id="loginForm" autocomplete="off">
            <mdui-text-field label="工号" placeholder="请输入工号" id="loginNumber" required style="display:block;margin-bottom:16px;width:100%;"></mdui-text-field>
            <mdui-text-field label="密码" type="password" placeholder="请输入密码" id="loginPassword" required style="display:block;margin-bottom:8px;width:100%;"></mdui-text-field>
            <div id="loginAlert"></div>
            <mdui-button type="submit" variant="filled" id="loginBtn" class="btn-block" style="margin-top:8px;">登录</mdui-button>
            <div class="hint" style="text-align:center;margin-top:8px;">调用接口：POST /api/auth/user/login</div>
        </form>

        {{-- 注册表单 --}}
        <form id="registerForm" autocomplete="off" class="hidden">
            <mdui-text-field label="姓名" placeholder="请输入姓名" id="regName" required style="display:block;margin-bottom:16px;width:100%;"></mdui-text-field>
            <mdui-text-field label="工号" placeholder="请输入工号" id="regNumber" required style="display:block;margin-bottom:16px;width:100%;"></mdui-text-field>
            <mdui-text-field label="密码" type="password" placeholder="请输入密码" id="regPassword" required style="display:block;margin-bottom:16px;width:100%;"></mdui-text-field>
            <mdui-text-field label="邮箱" placeholder="请输入邮箱" id="regEmail" required style="display:block;margin-bottom:8px;width:100%;"></mdui-text-field>
            <div id="registerAlert"></div>
            <mdui-button type="submit" variant="filled" id="registerBtn" class="btn-block" style="margin-top:8px;">注册</mdui-button>
            <div class="hint" style="text-align:center;margin-top:8px;">调用接口：POST /api/auth/user/register</div>
        </form>
    </mdui-card>
</div>

{{-- ============ 主应用视图 ============ --}}
<div id="appView" class="hidden">
    {{-- 用户信息栏 --}}
    <div class="user-bar">
        <div class="user-info">
            <div class="avatar" id="userAvatar">U</div>
            <div class="user-meta">
                <div class="uname" id="userName">用户</div>
                <div class="unum" id="userNumber">工号：--</div>
            </div>
        </div>
        <mdui-button variant="outlined" id="logoutBtn">退出登录</mdui-button>
    </div>

    {{-- 区块导航 --}}
    <div class="section-nav">
        <mdui-button variant="tonal" class="nav-btn active" data-section="java">Java 在线编译</mdui-button>
        <mdui-button variant="text" class="nav-btn" data-section="jar">Jar 插件执行</mdui-button>
        <mdui-button variant="text" class="nav-btn" data-section="status">插件状态</mdui-button>
    </div>

    {{-- ===== Java 在线编译 ===== --}}
    <section id="section-java" class="page-section">
        <mdui-card variant="outlined" class="content-card">
            <div class="card-header">
                <h3 class="section-title">Java 源码在线编译执行</h3>
                <div class="header-controls">
                    <mdui-select id="javaInMemory" value="true">
                        <mdui-menu-item value="true">纯内存编译</mdui-menu-item>
                        <mdui-menu-item value="false">文件编译</mdui-menu-item>
                    </mdui-select>
                    <mdui-button variant="filled" id="runJavaBtn">运行</mdui-button>
                </div>
            </div>
            <div class="editor-wrap">
                <div class="editor-bar">
                    <span>● Hello.java</span>
                    <span>支持 run() 或 main() 方法</span>
                </div>
                <textarea id="javaCode" spellcheck="false">public class Hello {
    public String run() {
        return "Hello from jaravel!";
    }
}</textarea>
            </div>
            <p class="hint">
                说明：支持编译执行含 <code>run()</code> 或 <code>main()</code> 方法的 Java 类。
                纯内存编译使用 <code>DynamicJavaCompiler</code>（MemoryJavaFileManager），文件编译使用标准 <code>javac</code>。
                请求：<code>POST /api/plugin/java/run</code>，请求体 <code>{ code, in_memory }</code>
            </p>

            <div class="result-block hidden" id="javaResult">
                <div class="result-meta">
                    <span>执行状态：<span id="javaSuccess"></span></span>
                    <span>编译状态：<span id="javaCompile"></span></span>
                    <span>类名：<b id="javaClassName">--</b></span>
                    <span>用户ID：<b id="javaUserId">--</b></span>
                </div>
                <div class="result-label hidden" id="javaOutputLabel">输出结果</div>
                <div class="result-box" id="javaOutput"></div>
                <div class="result-label hidden" id="javaErrorLabel" style="color:var(--mdui-color-error);">错误信息</div>
                <div class="result-box error hidden" id="javaError"></div>
            </div>
        </mdui-card>
    </section>

    {{-- ===== Jar 插件执行 ===== --}}
    <section id="section-jar" class="page-section hidden">
        <mdui-card variant="outlined" class="content-card">
            <div class="card-header">
                <h3 class="section-title">Jar 插件反射调用</h3>
            </div>
            <form id="jarForm" autocomplete="off">
                <div class="jar-form-grid">
                    <mdui-text-field label="Jar 文件名 (jar_name)" placeholder="例如：demo-plugin.jar" id="jarName" required style="display:block;margin-bottom:16px;width:100%;"></mdui-text-field>
                    <mdui-text-field label="主类全限定名 (main_class)" placeholder="例如：com.example.HelloPlugin" id="mainClass" required style="display:block;margin-bottom:16px;width:100%;"></mdui-text-field>
                    <mdui-text-field label="方法名 (method，默认 run)" placeholder="run" id="methodName" value="run" style="display:block;margin-bottom:16px;width:100%;"></mdui-text-field>
                    <div style="margin-bottom:16px;">
                        <label style="display:block;font-size:13px;color:var(--mdui-color-on-surface-variant);margin-bottom:4px;">加载方式</label>
                        <mdui-select id="inMemory" value="true" style="width:100%;">
                            <mdui-menu-item value="true">纯内存加载（不落盘）</mdui-menu-item>
                            <mdui-menu-item value="false">文件加载（URLClassLoader）</mdui-menu-item>
                        </mdui-select>
                    </div>
                </div>
                <mdui-button type="submit" variant="filled" id="runJarBtn">运行 Jar 插件</mdui-button>
            </form>
            <p class="hint">
                说明：通过反射加载 Jar 插件，调用指定类的指定方法。
                纯内存加载使用 <code>InMemoryJarClassLoader</code>（不落盘），文件加载使用 <code>URLClassLoader</code>。
                请求：<code>POST /api/plugin/jar/run</code>，请求体
                <code>{ jar_name, main_class, method, in_memory }</code>
            </p>

            <div class="result-block hidden" id="jarResult">
                <div class="result-meta">
                    <span>执行状态：<span id="jarSuccess"></span></span>
                    <span>Jar：<b id="jarNameOut">--</b></span>
                    <span>主类：<b id="jarMainClassOut">--</b></span>
                    <span>方法：<b id="jarMethodOut">--</b></span>
                </div>
                <div class="result-label hidden" id="jarOutputLabel">输出结果</div>
                <div class="result-box" id="jarOutput"></div>
                <div class="result-label hidden" id="jarErrorLabel" style="color:var(--mdui-color-error);">错误信息</div>
                <div class="result-box error hidden" id="jarError"></div>
            </div>
        </mdui-card>
    </section>

    {{-- ===== 插件状态 ===== --}}
    <section id="section-status" class="page-section hidden">
        <div class="status-grid">
            {{-- Java 状态 --}}
            <mdui-card variant="outlined" class="content-card">
                <div class="card-header">
                    <h3 class="section-title">Java 插件状态</h3>
                    <mdui-button variant="tonal" id="refreshJavaStatusBtn">刷新</mdui-button>
                </div>
                <div id="javaStatusBody">
                    <div class="loading"><mdui-circular-progress style="width:16px;height:16px;"></mdui-circular-progress> 加载中...</div>
                </div>
            </mdui-card>

            {{-- Jar 状态 --}}
            <mdui-card variant="outlined" class="content-card">
                <div class="card-header">
                    <h3 class="section-title">Jar 插件状态</h3>
                    <mdui-button variant="tonal" id="refreshJarStatusBtn">刷新</mdui-button>
                </div>
                <div id="jarStatusBody">
                    <div class="loading"><mdui-circular-progress style="width:16px;height:16px;"></mdui-circular-progress> 加载中...</div>
                </div>
            </mdui-card>
        </div>
    </section>
</div>
@endsection

{{-- 页面专属脚本 --}}
@section('scripts')
<script>
(function () {
    'use strict';

    /* ====================== 工具函数 ====================== */

    var TOKEN_KEY = 'user_token';

    function getToken() { return localStorage.getItem(TOKEN_KEY); }
    function setToken(t) { localStorage.setItem(TOKEN_KEY, t); }
    function clearToken() { localStorage.removeItem(TOKEN_KEY); }

    /** 兼容两种响应结构：{code,message,data} 信封 或 直接返回数据 */
    function unwrap(respJson) {
        if (respJson && typeof respJson === 'object' && 'code' in respJson && 'data' in respJson) {
            return respJson;
        }
        return { code: 200, message: '', data: respJson };
    }

    /** 统一 fetch 封装：自动携带 Authorization 头，自动解析信封 */
    function apiFetch(url, options) {
        options = options || {};
        options.headers = options.headers || {};
        var token = getToken();
        if (token) {
            options.headers['Authorization'] = 'Bearer ' + token;
        }
        if (options.body && typeof options.body !== 'string') {
            options.headers['Content-Type'] = 'application/json';
            options.body = JSON.stringify(options.body);
        } else if (options.body && !options.headers['Content-Type']) {
            options.headers['Content-Type'] = 'application/json';
        }
        return fetch(url, options).then(function (resp) {
            var ct = resp.headers.get('content-type') || '';
            if (ct.indexOf('application/json') !== -1) {
                return resp.json().then(function (json) {
                    return { ok: resp.ok, status: resp.status, json: unwrap(json) };
                });
            }
            return resp.text().then(function (txt) {
                return { ok: resp.ok, status: resp.status, json: unwrap({ message: txt, data: txt }) };
            });
        });
    }

    function el(id) { return document.getElementById(id); }
    function esc(str) {
        if (str === null || str === undefined) return '';
        return String(str)
            .replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;').replace(/'/g, '&#39;');
    }
    function pretty(obj) {
        if (obj === null || obj === undefined) return '';
        if (typeof obj === 'string') return obj;
        try { return JSON.stringify(obj, null, 2); } catch (e) { return String(obj); }
    }

    /** 状态徽章 */
    function stateBadge(state) {
        var s = String(state || '').toUpperCase();
        var cls = 'badge-success';
        if (s.indexOf('ENABL') !== -1) cls = 'badge-success';
        else if (s.indexOf('DISABL') !== -1 || s.indexOf('ERROR') !== -1 || s.indexOf('FAIL') !== -1) cls = 'badge-error';
        else if (s.indexOf('LOAD') !== -1 || s.indexOf('PEND') !== -1) cls = 'badge-success';
        return '<span class="badge ' + cls + '">' + esc(state || '--') + '</span>';
    }
    function boolBadge(val) {
        return val
            ? '<span class="badge badge-success">可用</span>'
            : '<span class="badge badge-error">不可用</span>';
    }
    function successBadge(val) {
        return val
            ? '<span class="badge badge-success">成功</span>'
            : '<span class="badge badge-error">失败</span>';
    }

    /** 按钮 loading 状态切换 */
    function setBtnLoading(btn, loading, loadingText) {
        if (!btn) return;
        btn.disabled = loading;
        if (loading) {
            if (!btn.dataset.origText) btn.dataset.origText = btn.textContent;
            btn.textContent = loadingText || '运行中...';
        } else {
            btn.textContent = btn.dataset.origText || '运行';
        }
    }

    /* ====================== 视图切换 ====================== */

    function showLogin() {
        el('loginView').classList.remove('hidden');
        el('appView').classList.add('hidden');
    }
    function showApp() {
        el('loginView').classList.add('hidden');
        el('appView').classList.remove('hidden');
    }

    /** 填充用户信息 */
    function fillUserInfo(user) {
        user = user || {};
        el('userName').textContent = user.name || '用户';
        el('userNumber').textContent = '工号：' + (user.number || '--');
        el('userAvatar').textContent = (user.name || 'U').charAt(0).toUpperCase();
    }

    /* ====================== 登录 / 注册 ====================== */

    /** 登录/注册 tab 切换 */
    document.querySelectorAll('.login-tab').forEach(function (tab) {
        tab.addEventListener('click', function () {
            document.querySelectorAll('.login-tab').forEach(function (t) {
                t.variant = 'text';
                t.classList.remove('active');
            });
            tab.variant = 'tonal';
            tab.classList.add('active');
            var isLogin = tab.dataset.tab === 'login';
            el('loginForm').classList.toggle('hidden', !isLogin);
            el('registerForm').classList.toggle('hidden', isLogin);
        });
    });

    /** 显示提示信息 */
    function showAlert(containerId, msg, type) {
        var c = el(containerId);
        if (!c) return;
        if (!msg) { c.innerHTML = ''; return; }
        c.innerHTML = '<div class="alert alert-' + (type || 'error') + '">' + esc(msg) + '</div>';
    }

    /** 登录提交 */
    el('loginForm').addEventListener('submit', function (e) {
        e.preventDefault();
        showAlert('loginAlert', '');
        var btn = el('loginBtn');
        setBtnLoading(btn, true, '登录中...');
        var payload = {
            number: el('loginNumber').value.trim(),
            password: el('loginPassword').value
        };
        apiFetch('/api/auth/user/login', { method: 'POST', body: payload })
            .then(function (r) {
                var d = r.json.data || {};
                if (r.ok && r.json.code === 200 && d.token) {
                    setToken(d.token);
                    fillUserInfo(d.user || {});
                    showApp();
                    switchSection('java');
                } else {
                    showAlert('loginAlert', d.message || r.json.message || '登录失败，请检查工号和密码');
                }
            })
            .catch(function (err) {
                showAlert('loginAlert', '请求失败：' + (err && err.message ? err.message : err));
            })
            .finally(function () { setBtnLoading(btn, false); });
    });

    /** 注册提交 */
    el('registerForm').addEventListener('submit', function (e) {
        e.preventDefault();
        showAlert('registerAlert', '');
        var btn = el('registerBtn');
        setBtnLoading(btn, true, '注册中...');
        var payload = {
            name: el('regName').value.trim(),
            number: el('regNumber').value.trim(),
            password: el('regPassword').value,
            email: el('regEmail').value.trim()
        };
        apiFetch('/api/auth/user/register', { method: 'POST', body: payload })
            .then(function (r) {
                var d = r.json.data || {};
                if (r.ok && r.json.code === 200 && d.token) {
                    setToken(d.token);
                    fillUserInfo(d.user || {});
                    showApp();
                    switchSection('java');
                } else {
                    showAlert('registerAlert', d.message || r.json.message || '注册失败');
                }
            })
            .catch(function (err) {
                showAlert('registerAlert', '请求失败：' + (err && err.message ? err.message : err));
            })
            .finally(function () { setBtnLoading(btn, false); });
    });

    /** 退出登录 */
    el('logoutBtn').addEventListener('click', function () {
        var btn = el('logoutBtn');
        setBtnLoading(btn, true, '退出中...');
        apiFetch('/api/auth/user/logout', { method: 'POST' })
            .catch(function () {})
            .finally(function () {
                clearToken();
                setBtnLoading(btn, false);
                showLogin();
                el('loginPassword').value = '';
            });
    });

    /* ====================== 区块切换 ====================== */

    var SECTION_TITLES = {
        java: 'Java 在线编译',
        jar: 'Jar 插件执行',
        status: '插件状态'
    };

    function switchSection(name) {
        document.querySelectorAll('.nav-btn').forEach(function (a) {
            var active = a.dataset.section === name;
            a.variant = active ? 'tonal' : 'text';
            a.classList.toggle('active', active);
        });
        document.querySelectorAll('.page-section').forEach(function (s) { s.classList.add('hidden'); });
        var sec = el('section-' + name);
        if (sec) sec.classList.remove('hidden');
        if (name === 'status') {
            loadJavaStatus();
            loadJarStatus();
        }
    }

    document.querySelectorAll('.nav-btn').forEach(function (a) {
        a.addEventListener('click', function () { switchSection(a.dataset.section); });
    });

    /* ====================== Java 在线编译 ====================== */

    /** 渲染 Java 运行结果 */
    function renderJavaResult(d) {
        var block = el('javaResult');
        block.classList.remove('hidden');
        el('javaSuccess').innerHTML = successBadge(d.success);
        el('javaCompile').innerHTML = d.compile_success === undefined
            ? '<span class="badge badge-success">--</span>'
            : successBadge(d.compile_success);
        el('javaClassName').textContent = d.class_name || '--';
        el('javaUserId').textContent = d.user_id !== undefined ? d.user_id : '--';

        // 输出结果
        if (d.output !== undefined && d.output !== null && d.output !== '') {
            el('javaOutputLabel').classList.remove('hidden');
            el('javaOutput').classList.remove('hidden');
            el('javaOutput').textContent = d.output;
        } else {
            el('javaOutputLabel').classList.add('hidden');
            el('javaOutput').classList.add('hidden');
        }

        // 错误信息
        if (d.error) {
            el('javaErrorLabel').classList.remove('hidden');
            el('javaError').classList.remove('hidden');
            el('javaError').textContent = d.error;
        } else {
            el('javaErrorLabel').classList.add('hidden');
            el('javaError').classList.add('hidden');
        }
    }

    el('runJavaBtn').addEventListener('click', function () {
        var code = el('javaCode').value;
        if (!code.trim()) { alert('请输入 Java 代码'); return; }
        var inMemory = el('javaInMemory').value;
        var btn = el('runJavaBtn');
        setBtnLoading(btn, true, '编译运行中...');
        el('javaResult').classList.add('hidden');
        apiFetch('/api/plugin/java/run', { method: 'POST', body: { code: code, in_memory: inMemory } })
            .then(function (r) {
                var d = r.json.data || {};
                if (r.ok && r.json.code === 200) {
                    renderJavaResult(d);
                } else {
                    renderJavaResult({ success: false, error: d.message || r.json.message || '运行失败' });
                }
            })
            .catch(function (err) {
                renderJavaResult({ success: false, error: '请求失败：' + (err && err.message ? err.message : err) });
            })
            .finally(function () { setBtnLoading(btn, false); });
    });

    /* ====================== Jar 插件执行 ====================== */

    /** 渲染 Jar 运行结果 */
    function renderJarResult(d) {
        var block = el('jarResult');
        block.classList.remove('hidden');
        el('jarSuccess').innerHTML = successBadge(d.success);
        el('jarNameOut').textContent = d.jar_name || '--';
        el('jarMainClassOut').textContent = d.main_class || '--';
        el('jarMethodOut').textContent = d.method || '--';

        if (d.output !== undefined && d.output !== null && d.output !== '') {
            el('jarOutputLabel').classList.remove('hidden');
            el('jarOutput').classList.remove('hidden');
            el('jarOutput').textContent = d.output;
        } else {
            el('jarOutputLabel').classList.add('hidden');
            el('jarOutput').classList.add('hidden');
        }
        if (d.error) {
            el('jarErrorLabel').classList.remove('hidden');
            el('jarError').classList.remove('hidden');
            el('jarError').textContent = d.error;
        } else {
            el('jarErrorLabel').classList.add('hidden');
            el('jarError').classList.add('hidden');
        }
    }

    el('jarForm').addEventListener('submit', function (e) {
        e.preventDefault();
        var payload = {
            jar_name: el('jarName').value.trim(),
            main_class: el('mainClass').value.trim(),
            method: el('methodName').value.trim() || 'run',
            in_memory: el('inMemory').value
        };
        if (!payload.jar_name || !payload.main_class) {
            alert('请填写 Jar 文件名和主类全限定名');
            return;
        }
        var btn = el('runJarBtn');
        setBtnLoading(btn, true, '运行中...');
        el('jarResult').classList.add('hidden');
        apiFetch('/api/plugin/jar/run', { method: 'POST', body: payload })
            .then(function (r) {
                var d = r.json.data || {};
                if (r.ok && r.json.code === 200) {
                    renderJarResult(d);
                } else {
                    renderJarResult({ success: false, error: d.message || r.json.message || '运行失败', jar_name: payload.jar_name, main_class: payload.main_class, method: payload.method });
                }
            })
            .catch(function (err) {
                renderJarResult({ success: false, error: '请求失败：' + (err && err.message ? err.message : err), jar_name: payload.jar_name, main_class: payload.main_class, method: payload.method });
            })
            .finally(function () { setBtnLoading(btn, false); });
    });

    /* ====================== 插件状态 ====================== */

    /** 加载 Java 插件状态 */
    function loadJavaStatus() {
        el('javaStatusBody').innerHTML = '<div class="loading"><mdui-circular-progress style="width:16px;height:16px;"></mdui-circular-progress> 加载中...</div>';
        apiFetch('/api/plugin/java/status', { method: 'GET' })
            .then(function (r) {
                var d = r.json.data || {};
                if (!r.ok) {
                    el('javaStatusBody').innerHTML = '<div class="alert alert-error">' + esc(d.message || r.json.message || '加载失败') + '</div>';
                    return;
                }
                var plugins = d.plugins || [];
                var html = '';
                html += '<div class="stat-row"><span class="k">编译器是否可用</span><span class="v">' + boolBadge(d.compiler_available) + '</span></div>';
                html += '<div class="stat-row"><span class="k">Java 版本</span><span class="v">' + esc(d.java_version || '--') + '</span></div>';
                html += '<div class="stat-row"><span class="k">已注册 Java 插件数</span><span class="v">' + esc(d.total !== undefined ? d.total : plugins.length) + '</span></div>';
                if (d.message) {
                    html += '<div class="alert alert-info" style="margin-top:12px;">' + esc(d.message) + '</div>';
                }
                html += '<div class="result-label">Java 插件列表</div>';
                if (plugins.length === 0) {
                    html += '<div class="empty">暂无已注册的 Java 插件</div>';
                } else {
                    html += '<div class="table-wrap"><table class="plugin-table"><thead><tr>'
                        + '<th>pluginId</th><th>状态</th><th>错误信息</th>'
                        + '</tr></thead><tbody>';
                    plugins.forEach(function (p) {
                        html += '<tr>'
                            + '<td>' + esc(p.pluginId) + '</td>'
                            + '<td>' + stateBadge(p.state) + '</td>'
                            + '<td>' + esc(p.errorMessage || '-') + '</td>'
                            + '</tr>';
                    });
                    html += '</tbody></table></div>';
                }
                el('javaStatusBody').innerHTML = html;
            })
            .catch(function (err) {
                el('javaStatusBody').innerHTML = '<div class="alert alert-error">请求失败：' + esc(err && err.message ? err.message : err) + '</div>';
            });
    }

    /** 加载 Jar 插件状态 */
    function loadJarStatus() {
        el('jarStatusBody').innerHTML = '<div class="loading"><mdui-circular-progress style="width:16px;height:16px;"></mdui-circular-progress> 加载中...</div>';
        apiFetch('/api/plugin/jar/status', { method: 'GET' })
            .then(function (r) {
                var d = r.json.data || {};
                if (!r.ok) {
                    el('jarStatusBody').innerHTML = '<div class="alert alert-error">' + esc(d.message || r.json.message || '加载失败') + '</div>';
                    return;
                }
                var plugins = d.plugins || [];
                var html = '';
                html += '<div class="stat-row"><span class="k">已加载 Jar 插件数</span><span class="v">' + esc(d.total !== undefined ? d.total : plugins.length) + '</span></div>';
                html += '<div class="result-label">Jar 插件列表</div>';
                if (plugins.length === 0) {
                    html += '<div class="empty">暂无已加载的 Jar 插件</div>';
                } else {
                    html += '<div class="table-wrap"><table class="plugin-table"><thead><tr>'
                        + '<th>pluginId</th><th>版本</th><th>状态</th><th>路由数</th><th>Bean数</th>'
                        + '</tr></thead><tbody>';
                    plugins.forEach(function (p) {
                        html += '<tr>'
                            + '<td>' + esc(p.pluginId) + '</td>'
                            + '<td>' + esc(p.version || '-') + '</td>'
                            + '<td>' + stateBadge(p.state) + '</td>'
                            + '<td>' + esc(p.routeCount !== undefined ? p.routeCount : '-') + '</td>'
                            + '<td>' + esc(p.beanCount !== undefined ? p.beanCount : '-') + '</td>'
                            + '</tr>';
                    });
                    html += '</tbody></table></div>';
                }
                el('jarStatusBody').innerHTML = html;
            })
            .catch(function (err) {
                el('jarStatusBody').innerHTML = '<div class="alert alert-error">请求失败：' + esc(err && err.message ? err.message : err) + '</div>';
            });
    }

    el('refreshJavaStatusBtn').addEventListener('click', loadJavaStatus);
    el('refreshJarStatusBtn').addEventListener('click', loadJarStatus);

    /* ====================== 初始化 ====================== */

    function init() {
        var token = getToken();
        if (!token) {
            showLogin();
            return;
        }
        // 已有 token，尝试获取用户信息验证有效性
        apiFetch('/api/auth/user/me', { method: 'GET' })
            .then(function (r) {
                var d = r.json.data || {};
                if (r.ok && r.json.code === 200 && d.id !== undefined) {
                    fillUserInfo(d);
                    showApp();
                    switchSection('java');
                } else {
                    clearToken();
                    showLogin();
                }
            })
            .catch(function () {
                clearToken();
                showLogin();
            });
    }

    init();
})();
</script>
@endsection
