@extends('layout')

@section('title', $title)
@section('page', $page)
@section('appName', $appName)

{{-- ============================================================
     管理后台页面（jblade 模板）
     - 继承 layout 布局（已引入 mdui 2 的 css / global.js）
     - 使用 mdui Material Design 3 Web Components
     - 全部接口调用使用 fetch，token 存于 localStorage
============================================================ --}}
@section('content')
<style>
    /* ===== 管理后台专属样式（基于 mdui 设计令牌） ===== */
    .admin-layout {
        display: flex;
        min-height: 100vh;
        background: var(--mdui-color-surface);
    }
    /* 侧边栏 */
    .sidebar {
        width: 248px;
        flex-shrink: 0;
        background: var(--mdui-color-surface-container-low);
        border-right: 1px solid var(--mdui-color-outline-variant);
        padding: 16px 12px;
        box-sizing: border-box;
        position: sticky;
        top: 0;
        height: 100vh;
        overflow-y: auto;
    }
    .sidebar-header { padding: 8px 12px 16px; }
    .sidebar-header h1 {
        margin: 0;
        font-size: 20px;
        color: var(--mdui-color-primary);
        font-weight: 700;
    }
    .sidebar-header p {
        margin: 4px 0 0;
        font-size: 12px;
        color: var(--mdui-color-on-surface-variant);
    }
    .sidebar-nav mdui-list-item { margin-bottom: 4px; }
    /* 主内容区 */
    .main {
        flex: 1;
        min-width: 0;
        padding: 20px 24px;
        box-sizing: border-box;
    }
    .topbar {
        display: flex;
        align-items: center;
        justify-content: space-between;
        margin-bottom: 20px;
        flex-wrap: wrap;
        gap: 12px;
    }
    .topbar h2 {
        margin: 0;
        font-size: 22px;
        color: var(--mdui-color-on-surface);
    }
    .topbar-user { display: flex; align-items: center; gap: 12px; }
    .topbar-user .meta { text-align: right; line-height: 1.2; }
    .topbar-user .meta .name { font-weight: 600; font-size: 14px; color: var(--mdui-color-on-surface); }
    .topbar-user .meta .role { font-size: 12px; color: var(--mdui-color-on-surface-variant); }
    /* 区块切换 */
    .section { display: none; }
    .section.active { display: block; }
    /* 面板卡片 */
    .panel {
        width: 100%;
        display: block;
        margin-bottom: 16px;
        box-sizing: border-box;
    }
    .panel-inner { padding: 20px; box-sizing: border-box; }
    .panel-header {
        display: flex;
        align-items: center;
        justify-content: space-between;
        margin-bottom: 16px;
        gap: 12px;
        flex-wrap: wrap;
    }
    .panel-header h3 { margin: 0; font-size: 16px; color: var(--mdui-color-on-surface); }
    .panel-header .actions { display: flex; gap: 8px; flex-wrap: wrap; }
    /* 仪表盘统计卡片 */
    .stats-grid {
        display: grid;
        grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
        gap: 16px;
    }
    .stat-card { display: block; width: 100%; box-sizing: border-box; }
    .stat-card .inner { padding: 16px; }
    .stat-card .label { font-size: 13px; color: var(--mdui-color-on-surface-variant); }
    .stat-card .value {
        font-size: 26px;
        font-weight: 700;
        color: var(--mdui-color-on-surface);
        margin-top: 6px;
    }
    /* 表格 */
    .table-wrap { overflow-x: auto; border-radius: 12px; }
    table { width: 100%; border-collapse: collapse; font-size: 14px; }
    thead th {
        text-align: left;
        padding: 12px;
        background: var(--mdui-color-surface-container);
        color: var(--mdui-color-on-surface-variant);
        font-weight: 600;
        border-bottom: 1px solid var(--mdui-color-outline-variant);
        white-space: nowrap;
    }
    tbody td {
        padding: 12px;
        border-bottom: 1px solid var(--mdui-color-outline-variant);
        color: var(--mdui-color-on-surface);
        vertical-align: middle;
    }
    tbody tr:hover { background: var(--mdui-color-surface-container); }
    .empty-row td { text-align: center; color: var(--mdui-color-on-surface-variant); padding: 32px; }
    .code-cell { max-width: 320px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
    /* 登录页 */
    .login-wrap {
        min-height: 100vh;
        display: flex;
        align-items: center;
        justify-content: center;
        background: var(--mdui-color-surface-container-low);
        padding: 20px;
    }
    .login-card { width: 100%; max-width: 400px; display: block; }
    .login-card .login-inner { padding: 32px; }
    .login-card h2 { margin: 0 0 8px; font-size: 24px; color: var(--mdui-color-on-surface); }
    .login-card .subtitle { margin: 0 0 24px; color: var(--mdui-color-on-surface-variant); font-size: 14px; }
    #loginForm mdui-text-field { display: block; width: 100%; margin-bottom: 16px; }
    #loginForm mdui-button { width: 100%; margin-top: 8px; }
    .alert-box { margin: 8px 0 0; }
    .alert-error {
        padding: 10px 12px;
        border-radius: 10px;
        background: var(--mdui-color-error-container);
        color: var(--mdui-color-on-error-container);
        font-size: 13px;
    }
    /* 状态标签：用 mdui-chip 的 CSS Part 着色 */
    mdui-chip.badge-ok::part(button) {
        background-color: var(--mdui-color-tertiary-container);
        color: var(--mdui-color-on-tertiary-container);
    }
    mdui-chip.badge-bad::part(button) {
        background-color: var(--mdui-color-error-container);
        color: var(--mdui-color-on-error-container);
    }
    /* 工具栏与行内输入 */
    .section-toolbar { display: flex; gap: 12px; align-items: center; flex-wrap: wrap; margin-bottom: 16px; }
    .inline-input {
        padding: 8px 12px;
        border: 1px solid var(--mdui-color-outline-variant);
        border-radius: 8px;
        font-size: 14px;
        background: var(--mdui-color-surface-container);
        color: var(--mdui-color-on-surface);
        min-width: 220px;
    }
    .inline-input:focus { outline: none; border-color: var(--mdui-color-primary); }
    /* 模态对话框表单 */
    #modalForm { display: flex; flex-direction: column; gap: 16px; }
    #modalForm mdui-text-field { display: block; width: 100%; }
    #modalForm .form-group label {
        display: block;
        font-size: 13px;
        color: var(--mdui-color-on-surface-variant);
        margin-bottom: 6px;
    }
    .native-input {
        padding: 10px;
        border: 1px solid var(--mdui-color-outline-variant);
        border-radius: 8px;
        background: var(--mdui-color-surface-container);
        color: var(--mdui-color-on-surface);
        width: 100%;
        box-sizing: border-box;
    }
    /* 状态网格 */
    .status-grid {
        display: grid;
        grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
        gap: 16px;
    }
    .info-row {
        display: flex;
        justify-content: space-between;
        padding: 10px 0;
        border-bottom: 1px solid var(--mdui-color-outline-variant);
        font-size: 14px;
    }
    .info-row:last-child { border-bottom: none; }
    .info-row .k { color: var(--mdui-color-on-surface-variant); }
    .info-row .v { font-weight: 500; color: var(--mdui-color-on-surface); }
    /* Toast 提示 */
    .toast {
        position: fixed;
        top: 20px;
        right: 20px;
        z-index: 3000;
        padding: 12px 20px;
        border-radius: 10px;
        font-size: 14px;
        opacity: 0;
        transform: translateX(30px);
        transition: all .3s;
        box-shadow: 0 4px 12px rgba(0,0,0,.2);
        max-width: 380px;
        pointer-events: none;
    }
    .toast.show { opacity: 1; transform: translateX(0); }
    .toast.success { background: var(--mdui-color-tertiary); color: var(--mdui-color-on-tertiary); }
    .toast.error { background: var(--mdui-color-error); color: var(--mdui-color-on-error); }
    .toast.info { background: var(--mdui-color-primary); color: var(--mdui-color-on-primary); }
    @media (max-width: 840px) {
        .admin-layout { flex-direction: column; }
        .sidebar { width: 100%; height: auto; position: relative; }
    }
</style>

{{-- ===================== 登录界面 ===================== --}}
<div id="loginView" class="login-wrap">
    <mdui-card class="login-card" variant="outlined">
        <div class="login-inner">
            <h2>管理员登录</h2>
            <p class="subtitle">{{ $appName }} 后台管理系统</p>
            <form id="loginForm">
                <mdui-text-field id="loginUsername" name="username" label="用户名"
                                 placeholder="请输入管理员用户名" variant="outlined" required
                                 autocomplete="username"></mdui-text-field>
                <mdui-text-field id="loginPassword" name="password" type="password" toggle-password
                                 label="密码" placeholder="请输入密码" variant="outlined" required
                                 autocomplete="current-password"></mdui-text-field>
                <div id="loginAlert" class="alert-box"></div>
                <mdui-button type="button" variant="filled" id="loginBtn">登 录</mdui-button>
            </form>
        </div>
    </mdui-card>
</div>

{{-- ===================== 主界面 ===================== --}}
<div id="mainView" class="admin-layout" style="display:none">
    {{-- 侧边栏 --}}
    <aside class="sidebar">
        <div class="sidebar-header">
            <h1>{{ $appName }}</h1>
            <p>管理后台</p>
        </div>
        <mdui-list class="sidebar-nav">
            <mdui-list-item data-section="sec-dashboard" icon="dashboard" rounded>仪表盘</mdui-list-item>
            <mdui-list-item data-section="sec-admins" icon="manage_accounts" rounded>管理员管理</mdui-list-item>
            <mdui-list-item data-section="sec-roles" icon="shield" rounded>角色管理</mdui-list-item>
            <mdui-list-item data-section="sec-permissions" icon="key" rounded>权限管理</mdui-list-item>
            <mdui-list-item data-section="sec-users" icon="group" rounded>用户管理</mdui-list-item>
            <mdui-list-item data-section="sec-jar" icon="inventory_2" rounded>Jar 插件管理</mdui-list-item>
            <mdui-list-item data-section="sec-java" icon="coffee" rounded>Java 插件管理</mdui-list-item>
            <mdui-list-item data-section="sec-tenant" icon="corporate_fare" rounded>多租户管理</mdui-list-item>
            <mdui-list-item data-section="sec-remote" icon="public" rounded>远程执行</mdui-list-item>
        </mdui-list>
    </aside>

    {{-- 主内容区 --}}
    <main class="main">
        {{-- 顶部栏 --}}
        <div class="topbar">
            <h2 id="pageTitle">仪表盘</h2>
            <div class="topbar-user">
                <div class="meta">
                    <div class="name" id="adminName">管理员</div>
                    <div class="role">已登录</div>
                </div>
                <mdui-avatar id="adminAvatar" label="A"></mdui-avatar>
                <mdui-button variant="tonal" id="logoutBtn">退出登录</mdui-button>
            </div>
        </div>

        {{-- ===== 仪表盘 ===== --}}
        <section id="sec-dashboard" class="section active">
            <mdui-card class="panel" variant="outlined">
                <div class="panel-inner">
                    <div class="panel-header">
                        <h3>插件系统总览</h3>
                        <div class="actions">
                            <mdui-button variant="tonal" onclick="loadDashboard()">刷新</mdui-button>
                        </div>
                    </div>
                    <div class="stats-grid" id="dashboardStats">
                        <mdui-card class="stat-card" variant="filled"><div class="inner"><div class="label">Java 编译器</div><div class="value" id="stCompiler" style="font-size:18px">--</div></div></mdui-card>
                        <mdui-card class="stat-card" variant="filled"><div class="inner"><div class="label">Java 版本</div><div class="value" id="stJavaVersion" style="font-size:18px">--</div></div></mdui-card>
                        <mdui-card class="stat-card" variant="filled"><div class="inner"><div class="label">Jar 插件总数</div><div class="value" id="stJarTotal">--</div></div></mdui-card>
                        <mdui-card class="stat-card" variant="filled"><div class="inner"><div class="label">Jar 启用数</div><div class="value" id="stJarEnabled">--</div></div></mdui-card>
                        <mdui-card class="stat-card" variant="filled"><div class="inner"><div class="label">Java 插件系统</div><div class="value" id="stJavaSystem" style="font-size:18px">--</div></div></mdui-card>
                        <mdui-card class="stat-card" variant="filled"><div class="inner"><div class="label">Java 插件数</div><div class="value" id="stJavaTotal">--</div></div></mdui-card>
                    </div>
                </div>
            </mdui-card>
        </section>

        {{-- ===== 管理员管理 ===== --}}
        <section id="sec-admins" class="section">
            <mdui-card class="panel" variant="outlined">
                <div class="panel-inner">
                    <div class="panel-header">
                        <h3>管理员列表</h3>
                        <div class="actions">
                            <mdui-button variant="tonal" onclick="loadAdmins()">刷新</mdui-button>
                            <mdui-button variant="filled" onclick="openAdminModal()">+ 新增管理员</mdui-button>
                        </div>
                    </div>
                    <div class="table-wrap">
                        <table>
                            <thead><tr><th>ID</th><th>用户名</th><th>昵称</th><th>状态</th><th>创建时间</th><th>操作</th></tr></thead>
                            <tbody id="adminsBody"><tr class="empty-row"><td colspan="6">加载中...</td></tr></tbody>
                        </table>
                    </div>
                </div>
            </mdui-card>
        </section>

        {{-- ===== 角色管理 ===== --}}
        <section id="sec-roles" class="section">
            <mdui-card class="panel" variant="outlined">
                <div class="panel-inner">
                    <div class="panel-header">
                        <h3>角色列表</h3>
                        <div class="actions">
                            <mdui-button variant="tonal" onclick="loadRoles()">刷新</mdui-button>
                            <mdui-button variant="filled" onclick="openRoleModal()">+ 新增角色</mdui-button>
                        </div>
                    </div>
                    <div class="table-wrap">
                        <table>
                            <thead><tr><th>ID</th><th>名称</th><th>编码</th><th>描述</th><th>操作</th></tr></thead>
                            <tbody id="rolesBody"><tr class="empty-row"><td colspan="5">加载中...</td></tr></tbody>
                        </table>
                    </div>
                </div>
            </mdui-card>
        </section>

        {{-- ===== 权限管理 ===== --}}
        <section id="sec-permissions" class="section">
            <mdui-card class="panel" variant="outlined">
                <div class="panel-inner">
                    <div class="panel-header">
                        <h3>权限列表</h3>
                        <div class="actions">
                            <mdui-button variant="tonal" onclick="loadPermissions()">刷新</mdui-button>
                            <mdui-button variant="filled" onclick="openPermissionModal()">+ 新增权限</mdui-button>
                        </div>
                    </div>
                    <div class="table-wrap">
                        <table>
                            <thead><tr><th>ID</th><th>名称</th><th>编码</th><th>父ID</th><th>路由</th><th>描述</th></tr></thead>
                            <tbody id="permissionsBody"><tr class="empty-row"><td colspan="6">加载中...</td></tr></tbody>
                        </table>
                    </div>
                </div>
            </mdui-card>
        </section>

        {{-- ===== 用户管理 ===== --}}
        <section id="sec-users" class="section">
            <mdui-card class="panel" variant="outlined">
                <div class="panel-inner">
                    <div class="panel-header">
                        <h3>平台用户列表</h3>
                        <div class="actions">
                            <mdui-button variant="tonal" onclick="loadUsers()">刷新</mdui-button>
                            <mdui-button variant="filled" onclick="openUserModal()">+ 新增用户</mdui-button>
                        </div>
                    </div>
                    <div class="table-wrap">
                        <table>
                            <thead><tr><th>ID</th><th>姓名</th><th>工号</th><th>邮箱</th><th>操作</th></tr></thead>
                            <tbody id="usersBody"><tr class="empty-row"><td colspan="5">加载中...</td></tr></tbody>
                        </table>
                    </div>
                </div>
            </mdui-card>
        </section>

        {{-- ===== Jar 插件管理 ===== --}}
        <section id="sec-jar" class="section">
            <mdui-card class="panel" variant="outlined">
                <div class="panel-inner">
                    <div class="panel-header">
                        <h3>Jar 插件列表</h3>
                        <div class="actions">
                            <mdui-button variant="tonal" onclick="loadJarPlugins()">刷新</mdui-button>
                            <mdui-button variant="filled" onclick="openJarUploadModal()">上传 Jar</mdui-button>
                        </div>
                    </div>
                    <div class="table-wrap">
                        <table>
                            <thead><tr><th>pluginId</th><th>版本</th><th>状态</th><th>路由数</th><th>Bean数</th><th>操作</th></tr></thead>
                            <tbody id="jarBody"><tr class="empty-row"><td colspan="6">加载中...</td></tr></tbody>
                        </table>
                    </div>
                </div>
            </mdui-card>
        </section>

        {{-- ===== Java 插件管理 ===== --}}
        <section id="sec-java" class="section">
            <mdui-card class="panel" variant="outlined">
                <div class="panel-inner">
                    <div class="panel-header">
                        <h3>Java 插件列表</h3>
                        <div class="actions">
                            <mdui-button variant="tonal" onclick="loadJavaPlugins()">刷新</mdui-button>
                            <mdui-button variant="filled" onclick="openJavaRegisterModal()">注册插件</mdui-button>
                        </div>
                    </div>
                    <div class="table-wrap">
                        <table>
                            <thead><tr><th>pluginId</th><th>状态</th><th>错误信息</th><th>操作</th></tr></thead>
                            <tbody id="javaBody"><tr class="empty-row"><td colspan="4">加载中...</td></tr></tbody>
                        </table>
                    </div>
                </div>
            </mdui-card>
        </section>

        {{-- ===== 多租户管理 ===== --}}
        <section id="sec-tenant" class="section">
            <mdui-card class="panel" variant="outlined">
                <div class="panel-inner">
                    <div class="panel-header">
                        <h3>多租户状态</h3>
                        <mdui-button variant="tonal" onclick="loadTenantStatus()">刷新</mdui-button>
                    </div>
                    <div id="tenantStatusBox" class="status-grid">加载中...</div>
                </div>
            </mdui-card>
            <mdui-card class="panel" variant="outlined">
                <div class="panel-inner">
                    <div class="panel-header">
                        <h3>租户插件列表</h3>
                        <div class="section-toolbar">
                            <mdui-text-field id="tenantIdInput" label="租户ID" placeholder="请输入租户ID (tenantId)" variant="outlined" class="inline-input" style="min-width:220px"></mdui-text-field>
                            <mdui-button variant="tonal" onclick="loadTenantPlugins()">查询</mdui-button>
                            <mdui-button variant="filled" onclick="openTenantPluginModal()">注册插件</mdui-button>
                        </div>
                    </div>
                    <div class="table-wrap">
                        <table>
                            <thead><tr><th>pluginId</th><th>状态</th><th>操作</th></tr></thead>
                            <tbody id="tenantPluginsBody"><tr class="empty-row"><td colspan="3">请输入租户ID后查询</td></tr></tbody>
                        </table>
                    </div>
                </div>
            </mdui-card>
        </section>

        {{-- ===== 远程执行 ===== --}}
        <section id="sec-remote" class="section">
            <mdui-card class="panel" variant="outlined">
                <div class="panel-inner">
                    <div class="panel-header">
                        <h3>远程执行状态</h3>
                        <mdui-button variant="tonal" onclick="loadRemoteStatus()">刷新</mdui-button>
                    </div>
                    <div id="remoteStatusBox" class="status-grid">加载中...</div>
                </div>
            </mdui-card>
            <mdui-card class="panel" variant="outlined">
                <div class="panel-inner">
                    <div class="panel-header">
                        <h3>子节点列表</h3>
                        <div class="actions">
                            <mdui-button variant="tonal" onclick="loadSubServers()">刷新</mdui-button>
                            <mdui-button variant="filled" onclick="openSubServerModal()">注册子节点</mdui-button>
                        </div>
                    </div>
                    <div class="table-wrap">
                        <table>
                            <thead><tr><th>subServerId</th><th>host</th><th>port</th><th>连接状态</th><th>操作</th></tr></thead>
                            <tbody id="subServersBody"><tr class="empty-row"><td colspan="5">加载中...</td></tr></tbody>
                        </table>
                    </div>
                </div>
            </mdui-card>
        </section>
    </main>
</div>

{{-- ===================== 通用对话框 ===================== --}}
<mdui-dialog id="modalDialog" close-on-overlay-click close-on-esc headline="标题">
    <form id="modalForm"></form>
    <mdui-button slot="action" variant="text" id="modalCancelBtn">取消</mdui-button>
    <mdui-button slot="action" variant="filled" id="modalSubmitBtn">确定</mdui-button>
</mdui-dialog>

{{-- ===================== Toast ===================== --}}
<div id="toast" class="toast"></div>
@endsection

@section('scripts')
<script>
/* ===================== 全局状态 ===================== */
let token = localStorage.getItem('admin_token');
let currentAdmin = JSON.parse(localStorage.getItem('admin_info') || 'null');
let modalSubmitHandler = null;

/* ===================== 工具函数 ===================== */
// 从对象中按优先键取值，兼容 snake_case / camelCase
function pick(obj, ...keys) {
    if (!obj || typeof obj !== 'object') return undefined;
    for (const k of keys) {
        if (obj[k] !== undefined && obj[k] !== null) return obj[k];
    }
    return undefined;
}

// 时间格式化
function fmtTime(v) {
    if (!v && v !== 0) return '--';
    const d = (typeof v === 'number') ? new Date(v) : new Date(v);
    if (isNaN(d.getTime())) return String(v);
    const p = n => String(n).padStart(2, '0');
    return d.getFullYear() + '-' + p(d.getMonth() + 1) + '-' + p(d.getDate()) + ' ' + p(d.getHours()) + ':' + p(d.getMinutes()) + ':' + p(d.getSeconds());
}

// HTML 转义
function esc(v) {
    if (v === undefined || v === null) return '';
    return String(v).replace(/[&<>"']/g, c => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
}

// Toast 提示
let toastTimer = null;
function toast(msg, type) {
    type = type || 'info';
    const el = document.getElementById('toast');
    el.textContent = msg;
    el.className = 'toast ' + type + ' show';
    clearTimeout(toastTimer);
    toastTimer = setTimeout(() => el.classList.remove('show'), 3000);
}

// 确认对话框（优先使用 mdui.confirm，回退原生 confirm）
async function confirmDialog(msg) {
    if (typeof mdui !== 'undefined' && typeof mdui.confirm === 'function') {
        try {
            await mdui.confirm({
                headline: '请确认',
                description: msg,
                confirmText: '确定',
                cancelText: '取消'
            });
            return true;
        } catch (e) { return false; }
    }
    return confirm(msg);
}

/* ===================== API 封装 ===================== */
async function api(url, options) {
    options = options || {};
    options.headers = options.headers || {};
    if (token && !options.headers['Authorization']) {
        options.headers['Authorization'] = 'Bearer ' + token;
    }
    let resp;
    try {
        resp = await fetch(url, options);
    } catch (e) {
        throw new Error('网络请求失败：' + e.message);
    }
    if (resp.status === 401) {
        handleUnauthorized();
        throw new Error('未授权，请重新登录');
    }
    const text = await resp.text();
    let data;
    try { data = text ? JSON.parse(text) : null; } catch (e) { data = text; }
    // 包装响应 {code, message, data}
    if (data && typeof data === 'object' && 'code' in data) {
        if (data.code !== 200 && data.code !== 0) {
            throw new Error(data.message || ('请求失败 (code=' + data.code + ')'));
        }
        return data.data !== undefined ? data.data : data;
    }
    // HTTP 错误但返回了 JSON
    if (!resp.ok && data && typeof data === 'object' && data.message) {
        throw new Error(data.message);
    }
    if (!resp.ok) {
        throw new Error('请求失败 HTTP ' + resp.status);
    }
    return data;
}

function apiPost(url, body) {
    return api(url, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(body)
    });
}

function handleUnauthorized() {
    token = null;
    currentAdmin = null;
    localStorage.removeItem('admin_token');
    localStorage.removeItem('admin_info');
    showLogin();
    toast('登录已过期，请重新登录', 'error');
}

/* ===================== 登录 / 退出 ===================== */
function showLogin() {
    document.getElementById('loginView').style.display = 'flex';
    document.getElementById('mainView').style.display = 'none';
}

function showMain() {
    document.getElementById('loginView').style.display = 'none';
    document.getElementById('mainView').style.display = 'flex';
    updateAdminUI();
    showSection('sec-dashboard');
}

// 更新顶部栏管理员信息
function updateAdminUI() {
    const name = pick(currentAdmin, 'nickname', 'username', 'name') || '管理员';
    document.getElementById('adminName').textContent = name;
    const initial = (String(name).charAt(0) || 'A').toUpperCase();
    document.getElementById('adminAvatar').setAttribute('label', initial);
}

// 获取当前登录管理员信息（GET /api/auth/admin/me）
async function loadAdminMe() {
    try {
        const me = await api('/api/auth/admin/me');
        if (me) {
            currentAdmin = me;
            localStorage.setItem('admin_info', JSON.stringify(currentAdmin));
            if (document.getElementById('mainView').style.display !== 'none') {
                updateAdminUI();
            }
        }
    } catch (e) {
        // 忽略：接口可能不存在，沿用登录返回的信息
    }
}

// 登录表单提交
document.getElementById('loginForm').addEventListener('submit', async (e) => {
    e.preventDefault();
    const username = document.getElementById('loginUsername').value.trim();
    const password = document.getElementById('loginPassword').value;
    const alertBox = document.getElementById('loginAlert');
    const btn = document.getElementById('loginBtn');
    alertBox.innerHTML = '';
    btn.disabled = true;
    const oldText = btn.textContent;
    btn.textContent = '登录中...';
    try {
        const data = await api('/api/auth/admin/login', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ username: username, password: password })
        });
        token = pick(data, 'token', 'accessToken', 'access_token');
        if (!token) throw new Error('登录返回数据异常：未获取到 token');
        localStorage.setItem('admin_token', token);
        currentAdmin = pick(data, 'admin', 'adminInfo', 'user') || data;
        localStorage.setItem('admin_info', JSON.stringify(currentAdmin));
        // 登录后调用 /me 拉取最新管理员信息（best effort）
        await loadAdminMe();
        toast('登录成功', 'success');
        showMain();
    } catch (err) {
        alertBox.innerHTML = '<div class="alert-error">' + esc(err.message) + '</div>';
    } finally {
        btn.disabled = false;
        btn.textContent = oldText;
    }
});

// 登录按钮触发提交（mdui-button 默认不提交表单，手动 requestSubmit）
document.getElementById('loginBtn').addEventListener('click', (e) => {
    e.preventDefault();
    document.getElementById('loginForm').requestSubmit();
});

async function logout() {
    if (!await confirmDialog('确定退出登录吗？')) return;
    try { await apiPost('/api/auth/admin/logout', {}); } catch (e) { /* 忽略退出接口错误 */ }
    token = null;
    currentAdmin = null;
    localStorage.removeItem('admin_token');
    localStorage.removeItem('admin_info');
    showLogin();
    toast('已退出登录', 'info');
}
document.getElementById('logoutBtn').addEventListener('click', logout);

/* ===================== 导航 / Section 切换 ===================== */
const sectionTitles = {
    'sec-dashboard': '仪表盘',
    'sec-admins': '管理员管理',
    'sec-roles': '角色管理',
    'sec-permissions': '权限管理',
    'sec-users': '用户管理',
    'sec-jar': 'Jar 插件管理',
    'sec-java': 'Java 插件管理',
    'sec-tenant': '多租户管理',
    'sec-remote': '远程执行'
};

const sectionLoaders = {
    'sec-dashboard': loadDashboard,
    'sec-admins': loadAdmins,
    'sec-roles': loadRoles,
    'sec-permissions': loadPermissions,
    'sec-users': loadUsers,
    'sec-jar': loadJarPlugins,
    'sec-java': loadJavaPlugins,
    'sec-tenant': function () { loadTenantStatus(); },
    'sec-remote': function () { loadRemoteStatus(); loadSubServers(); }
};

function showSection(id) {
    document.querySelectorAll('.section').forEach(s => s.classList.remove('active'));
    const sec = document.getElementById(id);
    if (sec) sec.classList.add('active');
    document.querySelectorAll('.sidebar-nav mdui-list-item').forEach(a => {
        a.active = (a.dataset.section === id);
    });
    document.getElementById('pageTitle').textContent = sectionTitles[id] || '';
    if (sectionLoaders[id]) {
        try { sectionLoaders[id](); } catch (e) { console.error(e); }
    }
}

document.querySelectorAll('.sidebar-nav mdui-list-item').forEach(item => {
    item.addEventListener('click', () => showSection(item.dataset.section));
});

/* ===================== 通用模态对话框 ===================== */
// 构建单个表单字段的 HTML（mdui-text-field；文件类型用原生 input）
function fieldHtml(f) {
    const req = f.required ? ' required' : '';
    const star = f.required ? ' *' : '';
    const label = esc(f.label) + star;
    const ph = esc(f.placeholder || '');
    if (f.type === 'file') {
        return '<div class="form-group"><label>' + esc(f.label) + star + '</label>' +
            '<input type="file" name="' + f.name + '" accept="' + esc(f.accept || '.jar') + '" class="native-input"' + req + '></div>';
    }
    if (f.type === 'textarea') {
        return '<mdui-text-field name="' + f.name + '" label="' + label + '" placeholder="' + ph + '" variant="outlined" rows="3"' + req + '></mdui-text-field>';
    }
    const type = f.type || 'text';
    const toggle = type === 'password' ? ' toggle-password' : '';
    return '<mdui-text-field name="' + f.name + '" label="' + label + '" placeholder="' + ph + '" variant="outlined" type="' + type + '"' + toggle + req + '></mdui-text-field>';
}

function openModal(opts) {
    const title = opts.title || '标题';
    const fields = opts.fields || [];
    const submitText = opts.submitText || '确定';
    const onSubmit = opts.onSubmit;
    const dialog = document.getElementById('modalDialog');
    dialog.headline = title;
    document.getElementById('modalSubmitBtn').textContent = submitText;
    const form = document.getElementById('modalForm');
    form.innerHTML = fields.map(fieldHtml).join('');
    modalSubmitHandler = onSubmit;
    dialog.open = true;
}

function closeModal() {
    document.getElementById('modalDialog').open = false;
    modalSubmitHandler = null;
    document.getElementById('modalForm').innerHTML = '';
}

// 收集表单值（兼容 mdui-text-field 的 .value 与原生 file 的 .files）
function collectModalForm() {
    const form = document.getElementById('modalForm');
    const vals = {};
    form.querySelectorAll('[name]').forEach(el => {
        const name = el.getAttribute('name');
        const type = el.getAttribute('type') || '';
        if (el.files) {
            vals[name] = el.files[0] || null;
        } else if (type === 'number') {
            vals[name] = el.value === '' ? null : Number(el.value);
        } else {
            vals[name] = (el.value || '').trim();
        }
    });
    return vals;
}

document.getElementById('modalSubmitBtn').addEventListener('click', async () => {
    if (!modalSubmitHandler) return;
    const btn = document.getElementById('modalSubmitBtn');
    btn.disabled = true;
    const oldText = btn.textContent;
    btn.textContent = '处理中...';
    try {
        await modalSubmitHandler();
        closeModal();
    } catch (e) {
        toast(e.message, 'error');
    } finally {
        btn.disabled = false;
        btn.textContent = oldText || '确定';
    }
});

document.getElementById('modalCancelBtn').addEventListener('click', closeModal);

/* ===================== 表格空状态 ===================== */
function emptyRow(colspan, msg) {
    msg = msg || '暂无数据';
    return '<tr class="empty-row"><td colspan="' + colspan + '">' + esc(msg) + '</td></tr>';
}

// 状态标签（mdui-chip）
function statusChip(active, okText, badText) {
    return '<mdui-chip variant="assist" class="' + (active ? 'badge-ok' : 'badge-bad') + '">' +
        (active ? okText : badText) + '</mdui-chip>';
}

/* =====================================================================
   1. 仪表盘
===================================================================== */
async function loadDashboard() {
    try {
        const d = await api('/api/plugin/overview');
        const compiler = pick(d, 'java_compiler_available', 'javaCompilerAvailable');
        const el = document.getElementById('stCompiler');
        el.textContent = compiler ? '可用' : '不可用';
        el.style.color = compiler ? 'var(--mdui-color-tertiary)' : 'var(--mdui-color-error)';

        document.getElementById('stJavaVersion').textContent = pick(d, 'java_version', 'javaVersion') || '--';
        document.getElementById('stJarTotal').textContent = (pick(d, 'jar_plugin_total', 'jarPluginTotal') ?? '--');
        document.getElementById('stJarEnabled').textContent = (pick(d, 'jar_plugin_enabled', 'jarPluginEnabled') ?? '--');

        const sys = pick(d, 'java_plugin_system', 'javaPluginSystem');
        const elSys = document.getElementById('stJavaSystem');
        elSys.textContent = (sys === 'enabled' || sys === true) ? '已启用' : '未启用';
        elSys.style.color = (sys === 'enabled' || sys === true) ? 'var(--mdui-color-tertiary)' : 'var(--mdui-color-error)';

        document.getElementById('stJavaTotal').textContent = (pick(d, 'java_plugin_total', 'javaPluginTotal') ?? '--');
    } catch (e) {
        toast('加载总览失败：' + e.message, 'error');
    }
}

/* =====================================================================
   2. 管理员管理
===================================================================== */
async function loadAdmins() {
    const body = document.getElementById('adminsBody');
    body.innerHTML = emptyRow(6, '加载中...');
    try {
        const list = await api('/api/rbac/admins');
        const arr = Array.isArray(list) ? list : (list && list.records ? list.records : []);
        if (!arr.length) { body.innerHTML = emptyRow(6); return; }
        body.innerHTML = arr.map(a => {
            const id = pick(a, 'id');
            const status = pick(a, 'status', 'enabled');
            const active = status === 1 || status === true || status === 'active' || status === 'enabled';
            return '<tr>' +
                '<td>' + esc(id) + '</td>' +
                '<td>' + esc(pick(a, 'username')) + '</td>' +
                '<td>' + esc(pick(a, 'nickname') || '--') + '</td>' +
                '<td>' + statusChip(active, '正常', '禁用') + '</td>' +
                '<td>' + esc(fmtTime(pick(a, 'created_at', 'createdAt', 'createTime'))) + '</td>' +
                '<td><mdui-button variant="tonal" onclick="toggleAdmin(' + esc(id) + ', ' + !active + ')">' + (active ? '禁用' : '启用') + '</mdui-button></td>' +
                '</tr>';
        }).join('');
    } catch (e) {
        body.innerHTML = emptyRow(6, '加载失败：' + esc(e.message));
    }
}

function openAdminModal() {
    openModal({
        title: '新增管理员',
        submitText: '创建',
        fields: [
            { name: 'username', label: '用户名', required: true, placeholder: '登录用户名' },
            { name: 'password', label: '密码', type: 'password', required: true, placeholder: '登录密码' },
            { name: 'nickname', label: '昵称', placeholder: '显示昵称' },
            { name: 'description', label: '描述', type: 'textarea', placeholder: '备注描述' }
        ],
        onSubmit: async () => {
            const vals = collectModalForm();
            if (!vals.username || !vals.password) throw new Error('用户名和密码必填');
            await apiPost('/api/rbac/admins', vals);
            toast('管理员创建成功', 'success');
            loadAdmins();
        }
    });
}

async function toggleAdmin(id, enable) {
    try {
        await apiPost('/api/rbac/admins/' + id + '/' + (enable ? 'enable' : 'disable'), {});
        toast((enable ? '启用' : '禁用') + '成功', 'success');
        loadAdmins();
    } catch (e) { toast(e.message, 'error'); }
}

/* =====================================================================
   3. 角色管理
===================================================================== */
async function loadRoles() {
    const body = document.getElementById('rolesBody');
    body.innerHTML = emptyRow(5, '加载中...');
    try {
        const list = await api('/api/rbac/roles');
        const arr = Array.isArray(list) ? list : (list && list.records ? list.records : []);
        if (!arr.length) { body.innerHTML = emptyRow(5); return; }
        body.innerHTML = arr.map(r => '<tr>' +
            '<td>' + esc(pick(r, 'id')) + '</td>' +
            '<td>' + esc(pick(r, 'name')) + '</td>' +
            '<td>' + esc(pick(r, 'code')) + '</td>' +
            '<td>' + esc(pick(r, 'description') || '--') + '</td>' +
            '<td><mdui-button variant="tonal" onclick="deleteRole(' + pick(r, 'id') + ')">删除</mdui-button></td>' +
            '</tr>').join('');
    } catch (e) {
        body.innerHTML = emptyRow(5, '加载失败：' + esc(e.message));
    }
}

function openRoleModal() {
    openModal({
        title: '新增角色',
        submitText: '创建',
        fields: [
            { name: 'name', label: '名称', required: true, placeholder: '角色名称' },
            { name: 'code', label: '编码', required: true, placeholder: '角色编码（如 admin）' },
            { name: 'description', label: '描述', type: 'textarea', placeholder: '角色描述' }
        ],
        onSubmit: async () => {
            const vals = collectModalForm();
            if (!vals.name || !vals.code) throw new Error('名称和编码必填');
            await apiPost('/api/rbac/roles', vals);
            toast('角色创建成功', 'success');
            loadRoles();
        }
    });
}

async function deleteRole(id) {
    if (!await confirmDialog('确定删除该角色？')) return;
    try {
        await api('/api/rbac/roles/' + id, { method: 'DELETE' });
        toast('删除成功', 'success');
        loadRoles();
    } catch (e) { toast(e.message, 'error'); }
}

/* =====================================================================
   4. 权限管理
===================================================================== */
async function loadPermissions() {
    const body = document.getElementById('permissionsBody');
    body.innerHTML = emptyRow(6, '加载中...');
    try {
        const list = await api('/api/rbac/permissions');
        const arr = Array.isArray(list) ? list : (list && list.records ? list.records : []);
        if (!arr.length) { body.innerHTML = emptyRow(6); return; }
        body.innerHTML = arr.map(p => '<tr>' +
            '<td>' + esc(pick(p, 'id')) + '</td>' +
            '<td>' + esc(pick(p, 'name')) + '</td>' +
            '<td>' + esc(pick(p, 'code')) + '</td>' +
            '<td>' + esc(pick(p, 'parent_id', 'parentId') ?? '--') + '</td>' +
            '<td>' + esc(pick(p, 'route') || '--') + '</td>' +
            '<td>' + esc(pick(p, 'description') || '--') + '</td>' +
            '</tr>').join('');
    } catch (e) {
        body.innerHTML = emptyRow(6, '加载失败：' + esc(e.message));
    }
}

function openPermissionModal() {
    openModal({
        title: '新增权限',
        submitText: '创建',
        fields: [
            { name: 'name', label: '名称', required: true, placeholder: '权限名称' },
            { name: 'code', label: '编码', required: true, placeholder: '权限编码' },
            { name: 'parent_id', label: '父权限ID', type: 'number', placeholder: '顶级权限留空' },
            { name: 'route', label: '路由', placeholder: '如 /api/admin/**' },
            { name: 'description', label: '描述', type: 'textarea', placeholder: '权限描述' }
        ],
        onSubmit: async () => {
            const vals = collectModalForm();
            if (!vals.name || !vals.code) throw new Error('名称和编码必填');
            await apiPost('/api/rbac/permissions', vals);
            toast('权限创建成功', 'success');
            loadPermissions();
        }
    });
}

/* =====================================================================
   5. 用户管理（平台用户）
===================================================================== */
async function loadUsers() {
    const body = document.getElementById('usersBody');
    body.innerHTML = emptyRow(5, '加载中...');
    try {
        const list = await api('/api/user-rbac/users');
        const arr = Array.isArray(list) ? list : (list && list.records ? list.records : []);
        if (!arr.length) { body.innerHTML = emptyRow(5); return; }
        body.innerHTML = arr.map(u => '<tr>' +
            '<td>' + esc(pick(u, 'id')) + '</td>' +
            '<td>' + esc(pick(u, 'name')) + '</td>' +
            '<td>' + esc(pick(u, 'number') || '--') + '</td>' +
            '<td>' + esc(pick(u, 'email') || '--') + '</td>' +
            '<td><mdui-button variant="tonal" onclick="deleteUser(' + pick(u, 'id') + ')">删除</mdui-button></td>' +
            '</tr>').join('');
    } catch (e) {
        body.innerHTML = emptyRow(5, '加载失败：' + esc(e.message));
    }
}

function openUserModal() {
    openModal({
        title: '新增用户',
        submitText: '创建',
        fields: [
            { name: 'name', label: '姓名', required: true, placeholder: '用户姓名' },
            { name: 'number', label: '工号', required: true, placeholder: '员工工号' },
            { name: 'password', label: '密码', type: 'password', required: true, placeholder: '登录密码' },
            { name: 'email', label: '邮箱', type: 'text', placeholder: '电子邮箱' }
        ],
        onSubmit: async () => {
            const vals = collectModalForm();
            if (!vals.name || !vals.number || !vals.password) throw new Error('姓名、工号、密码必填');
            await apiPost('/api/user-rbac/users', vals);
            toast('用户创建成功', 'success');
            loadUsers();
        }
    });
}

async function deleteUser(id) {
    if (!await confirmDialog('确定删除该用户？')) return;
    try {
        await api('/api/user-rbac/users/' + id, { method: 'DELETE' });
        toast('删除成功', 'success');
        loadUsers();
    } catch (e) { toast(e.message, 'error'); }
}

/* =====================================================================
   6. Jar 插件管理
===================================================================== */
async function loadJarPlugins() {
    const body = document.getElementById('jarBody');
    body.innerHTML = emptyRow(6, '加载中...');
    try {
        const list = await api('/api/plugins/jar');
        const arr = Array.isArray(list) ? list : (list && list.records ? list.records : []);
        if (!arr.length) { body.innerHTML = emptyRow(6); return; }
        body.innerHTML = arr.map(p => {
            const pid = pick(p, 'pluginId', 'plugin_id', 'id');
            const enabled = pick(p, 'enabled', 'status', 'active');
            const active = enabled === true || enabled === 1 || enabled === 'enabled' || enabled === 'active';
            const routes = pick(p, 'routeCount', 'route_count', 'routes') ?? (Array.isArray(pick(p, 'routes')) ? pick(p, 'routes').length : 0);
            const beans = pick(p, 'beanCount', 'bean_count', 'beans') ?? (Array.isArray(pick(p, 'beans')) ? pick(p, 'beans').length : 0);
            return '<tr>' +
                '<td>' + esc(pid) + '</td>' +
                '<td>' + esc(pick(p, 'version') || '--') + '</td>' +
                '<td>' + statusChip(active, '启用', '禁用') + '</td>' +
                '<td>' + esc(routes) + '</td>' +
                '<td>' + esc(beans) + '</td>' +
                '<td><mdui-button variant="tonal" onclick="toggleJar(\'' + esc(pid) + '\', ' + !active + ')">' + (active ? '禁用' : '启用') + '</mdui-button></td>' +
                '</tr>';
        }).join('');
    } catch (e) {
        body.innerHTML = emptyRow(6, '加载失败：' + esc(e.message));
    }
}

function openJarUploadModal() {
    openModal({
        title: '上传 Jar 插件',
        submitText: '上传',
        fields: [
            { name: 'file', label: 'Jar 文件', type: 'file', required: true, accept: '.jar' }
        ],
        onSubmit: async () => {
            const vals = collectModalForm();
            const file = vals.file;
            if (!file) throw new Error('请选择 Jar 文件');
            const fd = new FormData();
            fd.append('file', file);
            await api('/api/plugins/jar/upload', { method: 'POST', body: fd });
            toast('Jar 插件上传成功', 'success');
            loadJarPlugins();
        }
    });
}

async function toggleJar(pluginId, enable) {
    try {
        await apiPost('/api/plugins/jar/' + pluginId + '/' + (enable ? 'enable' : 'disable'), {});
        toast((enable ? '启用' : '禁用') + '成功', 'success');
        loadJarPlugins();
    } catch (e) { toast(e.message, 'error'); }
}

/* =====================================================================
   7. Java 插件管理
===================================================================== */
async function loadJavaPlugins() {
    const body = document.getElementById('javaBody');
    body.innerHTML = emptyRow(4, '加载中...');
    try {
        const list = await api('/api/plugins/java');
        const arr = Array.isArray(list) ? list : (list && list.records ? list.records : []);
        if (!arr.length) { body.innerHTML = emptyRow(4); return; }
        body.innerHTML = arr.map(p => {
            const pid = pick(p, 'pluginId', 'plugin_id', 'id');
            const status = pick(p, 'status', 'state');
            const ok = status === 'loaded' || status === 'enabled' || status === 'active' || status === true || status === 1;
            const err = pick(p, 'error', 'lastError', 'last_error', 'errorMessage');
            return '<tr>' +
                '<td>' + esc(pid) + '</td>' +
                '<td>' + statusChip(ok, esc(status || '正常'), esc(status || '异常')) + '</td>' +
                '<td class="code-cell" title="' + esc(err) + '">' + esc(err || '--') + '</td>' +
                '<td><mdui-button variant="tonal" onclick="reloadJava(\'' + esc(pid) + '\')">重载</mdui-button></td>' +
                '</tr>';
        }).join('');
    } catch (e) {
        body.innerHTML = emptyRow(4, '加载失败：' + esc(e.message));
    }
}

function openJavaRegisterModal() {
    openModal({
        title: '注册 Java 插件',
        submitText: '注册',
        fields: [
            { name: 'pluginId', label: 'pluginId', required: true, placeholder: '插件唯一标识' },
            { name: 'sourcePath', label: '源码路径', required: true, placeholder: 'Java 源文件路径' }
        ],
        onSubmit: async () => {
            const vals = collectModalForm();
            if (!vals.pluginId || !vals.sourcePath) throw new Error('pluginId 和源码路径必填');
            await apiPost('/api/plugins/java/register', vals);
            toast('Java 插件注册成功', 'success');
            loadJavaPlugins();
        }
    });
}

async function reloadJava(pluginId) {
    try {
        await apiPost('/api/plugins/java/' + pluginId + '/reload', {});
        toast('重载请求已发送', 'success');
        loadJavaPlugins();
    } catch (e) { toast(e.message, 'error'); }
}

/* =====================================================================
   8. 多租户管理
===================================================================== */
async function loadTenantStatus() {
    const box = document.getElementById('tenantStatusBox');
    box.innerHTML = '加载中...';
    try {
        const d = await api('/api/multi-tenant/status');
        const enabled = pick(d, 'enabled', 'multiTenantEnabled', 'multi_tenant_enabled');
        const items = [
            ['多租户功能', (enabled ? '已启用' : '未启用')],
            ['当前角色', pick(d, 'role', 'currentRole') || '--'],
            ['当前服务器ID', pick(d, 'serverId', 'server_id') || '--'],
            ['租户总数', pick(d, 'tenantCount', 'tenant_count') ?? '--']
        ];
        box.innerHTML = '<mdui-card class="stat-card" variant="filled" style="grid-column:span 2"><div class="inner">' +
            items.map(([k, v]) => '<div class="info-row"><span class="k">' + esc(k) + '</span><span class="v">' + esc(v) + '</span></div>').join('') +
            '</div></mdui-card>';
    } catch (e) {
        box.innerHTML = '<div class="alert-error">加载失败：' + esc(e.message) + '</div>';
    }
}

async function loadTenantPlugins() {
    const tenantId = document.getElementById('tenantIdInput').value.trim();
    const body = document.getElementById('tenantPluginsBody');
    if (!tenantId) { toast('请输入租户ID', 'error'); return; }
    body.innerHTML = emptyRow(3, '加载中...');
    try {
        const list = await api('/api/multi-tenant/tenants/' + tenantId + '/plugins');
        const arr = Array.isArray(list) ? list : (list && list.records ? list.records : []);
        if (!arr.length) { body.innerHTML = emptyRow(3); return; }
        body.innerHTML = arr.map(p => {
            const pid = pick(p, 'pluginId', 'plugin_id', 'id');
            const enabled = pick(p, 'enabled', 'status', 'active');
            const active = enabled === true || enabled === 1 || enabled === 'enabled' || enabled === 'active';
            return '<tr>' +
                '<td>' + esc(pid) + '</td>' +
                '<td>' + statusChip(active, '启用', '禁用') + '</td>' +
                '<td><mdui-button variant="tonal" onclick="toggleTenantPlugin(\'' + esc(tenantId) + '\',\'' + esc(pid) + '\',' + !active + ')">' + (active ? '禁用' : '启用') + '</mdui-button></td>' +
                '</tr>';
        }).join('');
    } catch (e) {
        body.innerHTML = emptyRow(3, '加载失败：' + esc(e.message));
    }
}

function openTenantPluginModal() {
    const tenantId = document.getElementById('tenantIdInput').value.trim();
    if (!tenantId) { toast('请先输入租户ID', 'error'); return; }
    openModal({
        title: '为租户注册插件 (' + tenantId + ')',
        submitText: '注册',
        fields: [
            { name: 'pluginId', label: 'pluginId', required: true, placeholder: '插件唯一标识' }
        ],
        onSubmit: async () => {
            const vals = collectModalForm();
            if (!vals.pluginId) throw new Error('pluginId 必填');
            await apiPost('/api/multi-tenant/tenants/' + tenantId + '/plugins', vals);
            toast('租户插件注册成功', 'success');
            loadTenantPlugins();
        }
    });
}

async function toggleTenantPlugin(tenantId, pluginId, enable) {
    try {
        await apiPost('/api/multi-tenant/tenants/' + tenantId + '/plugins/' + pluginId + '/' + (enable ? 'enable' : 'disable'), {});
        toast((enable ? '启用' : '禁用') + '成功', 'success');
        loadTenantPlugins();
    } catch (e) { toast(e.message, 'error'); }
}

/* =====================================================================
   9. 远程执行
===================================================================== */
async function loadRemoteStatus() {
    const box = document.getElementById('remoteStatusBox');
    box.innerHTML = '加载中...';
    try {
        const d = await api('/api/remote/status');
        const items = [
            ['节点角色', pick(d, 'role', 'nodeRole') || '--'],
            ['服务器ID', pick(d, 'serverId', 'server_id') || '--'],
            ['监听端口', pick(d, 'port') ?? '--'],
            ['已连接子节点', pick(d, 'connectedCount', 'connected_count') ?? '--'],
            ['父节点', pick(d, 'parentServerId', 'parent_server_id') || '无']
        ];
        box.innerHTML = '<mdui-card class="stat-card" variant="filled" style="grid-column:span 2"><div class="inner">' +
            items.map(([k, v]) => '<div class="info-row"><span class="k">' + esc(k) + '</span><span class="v">' + esc(v) + '</span></div>').join('') +
            '</div></mdui-card>';
    } catch (e) {
        box.innerHTML = '<div class="alert-error">加载失败：' + esc(e.message) + '</div>';
    }
}

async function loadSubServers() {
    const body = document.getElementById('subServersBody');
    body.innerHTML = emptyRow(5, '加载中...');
    try {
        const list = await api('/api/remote/sub-servers');
        const arr = Array.isArray(list) ? list : (list && list.records ? list.records : []);
        if (!arr.length) { body.innerHTML = emptyRow(5); return; }
        body.innerHTML = arr.map(s => {
            const sid = pick(s, 'subServerId', 'sub_server_id', 'id');
            const connected = pick(s, 'connected', 'status');
            const on = connected === true || connected === 1 || connected === 'connected' || connected === 'online';
            return '<tr>' +
                '<td>' + esc(sid) + '</td>' +
                '<td>' + esc(pick(s, 'host')) + '</td>' +
                '<td>' + esc(pick(s, 'port')) + '</td>' +
                '<td>' + statusChip(on, '已连接', '未连接') + '</td>' +
                '<td><mdui-button variant="tonal" onclick="toggleSubServer(\'' + esc(sid) + '\',' + !on + ')">' + (on ? '断开' : '连接') + '</mdui-button></td>' +
                '</tr>';
        }).join('');
    } catch (e) {
        body.innerHTML = emptyRow(5, '加载失败：' + esc(e.message));
    }
}

function openSubServerModal() {
    openModal({
        title: '注册子节点',
        submitText: '注册',
        fields: [
            { name: 'subServerId', label: 'subServerId', required: true, placeholder: '子节点唯一标识' },
            { name: 'host', label: 'host', required: true, placeholder: '子节点地址' },
            { name: 'port', label: 'port', type: 'number', required: true, placeholder: '子节点端口' }
        ],
        onSubmit: async () => {
            const vals = collectModalForm();
            if (!vals.subServerId || !vals.host || !vals.port) throw new Error('所有字段必填');
            await apiPost('/api/remote/sub-servers', vals);
            toast('子节点注册成功', 'success');
            loadSubServers();
        }
    });
}

async function toggleSubServer(subServerId, connect) {
    try {
        await apiPost('/api/remote/sub-servers/' + subServerId + '/' + (connect ? 'connect' : 'disconnect'), {});
        toast((connect ? '连接' : '断开') + '请求已发送', 'success');
        loadSubServers();
    } catch (e) { toast(e.message, 'error'); }
}

/* ===================== 初始化 ===================== */
async function init() {
    if (!token) { showLogin(); return; }
    // 用 /api/auth/admin/me 验证 token 并加载管理员信息
    try {
        const me = await api('/api/auth/admin/me');
        if (me) {
            currentAdmin = me;
            localStorage.setItem('admin_info', JSON.stringify(currentAdmin));
        }
        showMain();
    } catch (e) {
        // 401 已由 handleUnauthorized 处理回到登录；其它异常也回到登录
        if (token) {
            token = null;
            localStorage.removeItem('admin_token');
        }
        showLogin();
    }
}
init();
</script>
@endsection
