@extends('layout')

@section('title', $title)
@section('page', $page)
@section('appName', $appName)

{{-- ============================================================
     管理后台页面（jblade 模板）
     - 继承 layout 布局（已引入 mdui 1.x 的 css / js）
     - 使用 mdui 1.x Material Design 1（CSS 类，非 Web Components）
     - 全部接口调用使用 fetch，token 存于 localStorage
============================================================ --}}

@section('head')
<script src="/js/jaravel-captcha.js"></script>
<style>
    /* ===== 管理后台专属样式（基于 mdui 1.x） ===== */
    /* 页面头部栏（标题 + 用户信息） */
    .page-header {
        display: flex;
        align-items: center;
        justify-content: space-between;
        margin-bottom: 20px;
        flex-wrap: wrap;
        gap: 12px;
    }
    .page-header h2 {
        margin: 0;
        font-size: 22px;
        color: #212121;
    }
    .header-user { display: flex; align-items: center; gap: 12px; }
    .header-user .meta { text-align: right; line-height: 1.2; }
    .header-user .meta .name { font-weight: 600; font-size: 14px; color: #212121; }
    .header-user .meta .role { font-size: 12px; color: #757575; }
    .admin-avatar {
        width: 40px;
        height: 40px;
        border-radius: 50%;
        display: flex;
        align-items: center;
        justify-content: center;
        color: #ffffff;
        font-weight: 600;
        font-size: 18px;
        flex-shrink: 0;
    }
    /* 抽屉导航内部样式 */
    .drawer-nav .mdui-list-item { margin-bottom: 4px; }
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
    .panel-header h3 { margin: 0; font-size: 16px; color: #212121; }
    .panel-header .actions { display: flex; gap: 8px; flex-wrap: wrap; }
    /* 仪表盘统计卡片 */
    .stats-grid {
        display: grid;
        grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
        gap: 16px;
    }
    .stat-card { display: block; width: 100%; box-sizing: border-box; }
    .stat-card .inner { padding: 16px; }
    .stat-card .label { font-size: 13px; color: #757575; }
    .stat-card .value {
        font-size: 26px;
        font-weight: 700;
        color: #212121;
        margin-top: 6px;
    }
    /* 表格 */
    .table-wrap { overflow-x: auto; border-radius: 12px; }
    table { width: 100%; border-collapse: collapse; font-size: 14px; }
    thead th {
        text-align: left;
        padding: 12px;
        background: #f5f5f5;
        color: #757575;
        font-weight: 600;
        border-bottom: 1px solid #e0e0e0;
        white-space: nowrap;
    }
    tbody td {
        padding: 12px;
        border-bottom: 1px solid #e0e0e0;
        color: #212121;
        vertical-align: middle;
    }
    tbody tr:hover { background: #f5f5f5; }
    .empty-row td { text-align: center; color: #757575; padding: 32px; }
    .code-cell { max-width: 320px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
    /* 登录页 */
    .login-wrap {
        min-height: 100vh;
        display: flex;
        align-items: center;
        justify-content: center;
        background: #fafafa;
        padding: 20px;
    }
    .login-card { width: 100%; max-width: 400px; display: block; }
    .login-card .login-inner { padding: 32px; }
    .login-card h2 { margin: 0 0 8px; font-size: 24px; color: #212121; }
    .login-card .subtitle { margin: 0 0 24px; color: #757575; font-size: 14px; }
    #loginForm .mdui-textfield { display: block; width: 100%; margin-bottom: 16px; }
    #loginForm .login-btn { width: 100%; margin-top: 8px; }
    .alert-box { margin: 8px 0 0; }
    .alert-error {
        padding: 10px 12px;
        border-radius: 10px;
        background: #ffebee;
        color: #b71c1c;
        font-size: 13px;
    }
    /* 状态标签 */
    .badge { display: inline-block; padding: 2px 10px; border-radius: 12px; font-size: 12px; }
    .badge-success { background: #c8e6c9; color: #1b5e20; }
    .badge-error { background: #ffcdd2; color: #b71c1c; }
    /* 工具栏与行内输入 */
    .section-toolbar { display: flex; gap: 12px; align-items: center; flex-wrap: wrap; margin-bottom: 16px; }
    .inline-input {
        padding: 8px 12px;
        border: 1px solid #e0e0e0;
        border-radius: 8px;
        font-size: 14px;
        background: #f5f5f5;
        color: #212121;
        min-width: 220px;
    }
    .inline-input:focus { outline: none; border-color: #3f51b5; }
    /* 模态对话框表单 */
    #modalForm { display: flex; flex-direction: column; gap: 16px; }
    #modalForm .mdui-textfield { display: block; width: 100%; }
    #modalForm .form-group label {
        display: block;
        font-size: 13px;
        color: #757575;
        margin-bottom: 6px;
    }
    .native-input {
        padding: 10px;
        border: 1px solid #e0e0e0;
        border-radius: 8px;
        background: #f5f5f5;
        color: #212121;
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
        border-bottom: 1px solid #e0e0e0;
        font-size: 14px;
    }
    .info-row:last-child { border-bottom: none; }
    .info-row .k { color: #757575; }
    .info-row .v { font-weight: 500; color: #212121; }
    /* Toast 回退样式（mdui.snackbar 不可用时使用） */
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
    .toast.success { background: #4caf50; color: #ffffff; }
    .toast.error { background: #f44336; color: #ffffff; }
    .toast.info { background: #3f51b5; color: #ffffff; }
    /* 表格内操作按钮收紧 */
    .btn-action { min-width: 0; padding: 0 12px; }
</style>
@endsection

{{-- ===================== 抽屉导航栏 ===================== --}}
@section('drawer')
<div class="mdui-drawer mdui-drawer-close" id="mainDrawer">
    <div class="drawer-header">
        <div class="drawer-title">{{ $appName }}</div>
        <div class="drawer-subtitle">管理后台</div>
    </div>
    <ul class="mdui-list drawer-nav">
        <li class="mdui-list-item mdui-ripple" data-section="sec-dashboard">
            <i class="mdui-list-item-icon mdui-icon material-icons">dashboard</i>
            <div class="mdui-list-item-content">仪表盘</div>
        </li>
        <li class="mdui-list-item mdui-ripple" data-section="sec-admins">
            <i class="mdui-list-item-icon mdui-icon material-icons">account_circle</i>
            <div class="mdui-list-item-content">管理员管理</div>
        </li>
        <li class="mdui-list-item mdui-ripple" data-section="sec-roles">
            <i class="mdui-list-item-icon mdui-icon material-icons">security</i>
            <div class="mdui-list-item-content">角色管理</div>
        </li>
        <li class="mdui-list-item mdui-ripple" data-section="sec-permissions">
            <i class="mdui-list-item-icon mdui-icon material-icons">vpn_key</i>
            <div class="mdui-list-item-content">权限管理</div>
        </li>
        <li class="mdui-list-item mdui-ripple" data-section="sec-users">
            <i class="mdui-list-item-icon mdui-icon material-icons">group</i>
            <div class="mdui-list-item-content">用户管理</div>
        </li>
        <li class="mdui-list-item mdui-ripple" data-section="sec-jar">
            <i class="mdui-list-item-icon mdui-icon material-icons">archive</i>
            <div class="mdui-list-item-content">Jar 插件管理</div>
        </li>
        <li class="mdui-list-item mdui-ripple" data-section="sec-java">
            <i class="mdui-list-item-icon mdui-icon material-icons">local_cafe</i>
            <div class="mdui-list-item-content">Java 插件管理</div>
        </li>
        <li class="mdui-list-item mdui-ripple" data-section="sec-tenant">
            <i class="mdui-list-item-icon mdui-icon material-icons">domain</i>
            <div class="mdui-list-item-content">多租户管理</div>
        </li>
        <li class="mdui-list-item mdui-ripple" data-section="sec-remote">
            <i class="mdui-list-item-icon mdui-icon material-icons">language</i>
            <div class="mdui-list-item-content">远程执行</div>
        </li>
    </ul>
</div>
@endsection

@section('content')
{{-- ===================== 登录界面 ===================== --}}
<div id="loginView" class="login-wrap">
    <div class="mdui-card login-card">
        <div class="login-inner">
            <h2>管理员登录</h2>
            <p class="subtitle">{{ $appName }} 后台管理系统</p>
            <form id="loginForm">
                <div class="mdui-textfield">
                    <label class="mdui-textfield-label">用户名</label>
                    <input class="mdui-textfield-input" id="loginUsername" name="username" type="text"
                           placeholder="请输入管理员用户名" required autocomplete="username">
                </div>
                <div class="mdui-textfield">
                    <label class="mdui-textfield-label">密码</label>
                    <input class="mdui-textfield-input" id="loginPassword" name="password" type="password"
                           placeholder="请输入密码" required autocomplete="current-password">
                </div>
                <div id="loginAlert" class="alert-box"></div>
                {{-- 验证码区域（点击登录时才显示） --}}
<div id="captchaArea" style="display:none; margin-bottom:16px;">
    <div id="captchaContainer"></div>
    <div id="captchaStatus" style="text-align:center;margin-top:4px;font-size:13px;color:#757575;">请完成验证后自动登录</div>
</div>
                <button type="button" class="mdui-btn mdui-btn-raised mdui-ripple mdui-color-theme-accent login-btn" id="loginBtn">登 录</button>
            </form>
        </div>
    </div>
</div>

{{-- ===================== 主界面 ===================== --}}
<div id="mainView" style="display:none">
    {{-- 页面头部 --}}
    <div class="page-header">
        <h2 id="pageTitle">仪表盘</h2>
        <div class="header-user">
            <div class="meta">
                <div class="name" id="adminName">管理员</div>
                <div class="role">已登录</div>
            </div>
            <div id="adminAvatar" class="admin-avatar mdui-color-theme-accent">A</div>
            <button class="mdui-btn mdui-ripple mdui-color-theme" id="logoutBtn">退出登录</button>
        </div>
    </div>

    {{-- ===== 仪表盘 ===== --}}
        <section id="sec-dashboard" class="section active">
            <div class="mdui-card panel">
                <div class="panel-inner">
                    <div class="panel-header">
                        <h3>插件系统总览</h3>
                        <div class="actions">
                            <button class="mdui-btn mdui-ripple mdui-color-theme" onclick="loadDashboard()">刷新</button>
                        </div>
                    </div>
                    <div class="stats-grid" id="dashboardStats">
                        <div class="mdui-card stat-card"><div class="inner"><div class="label">Java 编译器</div><div class="value" id="stCompiler" style="font-size:18px">--</div></div></div>
                        <div class="mdui-card stat-card"><div class="inner"><div class="label">Java 版本</div><div class="value" id="stJavaVersion" style="font-size:18px">--</div></div></div>
                        <div class="mdui-card stat-card"><div class="inner"><div class="label">Jar 插件总数</div><div class="value" id="stJarTotal">--</div></div></div>
                        <div class="mdui-card stat-card"><div class="inner"><div class="label">Jar 启用数</div><div class="value" id="stJarEnabled">--</div></div></div>
                        <div class="mdui-card stat-card"><div class="inner"><div class="label">Java 插件系统</div><div class="value" id="stJavaSystem" style="font-size:18px">--</div></div></div>
                        <div class="mdui-card stat-card"><div class="inner"><div class="label">Java 插件数</div><div class="value" id="stJavaTotal">--</div></div></div>
                    </div>
                </div>
            </div>
        </section>

        {{-- ===== 管理员管理 ===== --}}
        <section id="sec-admins" class="section">
            <div class="mdui-card panel">
                <div class="panel-inner">
                    <div class="panel-header">
                        <h3>管理员列表</h3>
                        <div class="actions">
                            <button class="mdui-btn mdui-ripple mdui-color-theme" onclick="loadAdmins()">刷新</button>
                            <button class="mdui-btn mdui-btn-raised mdui-ripple mdui-color-theme-accent" onclick="openAdminModal()">+ 新增管理员</button>
                        </div>
                    </div>
                    <div class="table-wrap">
                        <table>
                            <thead><tr><th>ID</th><th>用户名</th><th>昵称</th><th>状态</th><th>创建时间</th><th>操作</th></tr></thead>
                            <tbody id="adminsBody"><tr class="empty-row"><td colspan="6">加载中...</td></tr></tbody>
                        </table>
                    </div>
                </div>
            </div>
        </section>

        {{-- ===== 角色管理 ===== --}}
        <section id="sec-roles" class="section">
            <div class="mdui-card panel">
                <div class="panel-inner">
                    <div class="panel-header">
                        <h3>角色列表</h3>
                        <div class="actions">
                            <button class="mdui-btn mdui-ripple mdui-color-theme" onclick="loadRoles()">刷新</button>
                            <button class="mdui-btn mdui-btn-raised mdui-ripple mdui-color-theme-accent" onclick="openRoleModal()">+ 新增角色</button>
                        </div>
                    </div>
                    <div class="table-wrap">
                        <table>
                            <thead><tr><th>ID</th><th>名称</th><th>编码</th><th>描述</th><th>操作</th></tr></thead>
                            <tbody id="rolesBody"><tr class="empty-row"><td colspan="5">加载中...</td></tr></tbody>
                        </table>
                    </div>
                </div>
            </div>
        </section>

        {{-- ===== 权限管理 ===== --}}
        <section id="sec-permissions" class="section">
            <div class="mdui-card panel">
                <div class="panel-inner">
                    <div class="panel-header">
                        <h3>权限列表</h3>
                        <div class="actions">
                            <button class="mdui-btn mdui-ripple mdui-color-theme" onclick="loadPermissions()">刷新</button>
                            <button class="mdui-btn mdui-btn-raised mdui-ripple mdui-color-theme-accent" onclick="openPermissionModal()">+ 新增权限</button>
                        </div>
                    </div>
                    <div class="table-wrap">
                        <table>
                            <thead><tr><th>ID</th><th>名称</th><th>编码</th><th>父ID</th><th>路由</th><th>描述</th></tr></thead>
                            <tbody id="permissionsBody"><tr class="empty-row"><td colspan="6">加载中...</td></tr></tbody>
                        </table>
                    </div>
                </div>
            </div>
        </section>

        {{-- ===== 用户管理 ===== --}}
        <section id="sec-users" class="section">
            <div class="mdui-card panel">
                <div class="panel-inner">
                    <div class="panel-header">
                        <h3>平台用户列表</h3>
                        <div class="actions">
                            <button class="mdui-btn mdui-ripple mdui-color-theme" onclick="loadUsers()">刷新</button>
                            <button class="mdui-btn mdui-btn-raised mdui-ripple mdui-color-theme-accent" onclick="openUserModal()">+ 新增用户</button>
                        </div>
                    </div>
                    <div class="table-wrap">
                        <table>
                            <thead><tr><th>ID</th><th>姓名</th><th>工号</th><th>邮箱</th><th>操作</th></tr></thead>
                            <tbody id="usersBody"><tr class="empty-row"><td colspan="5">加载中...</td></tr></tbody>
                        </table>
                    </div>
                </div>
            </div>
        </section>

        {{-- ===== Jar 插件管理 ===== --}}
        <section id="sec-jar" class="section">
            <div class="mdui-card panel">
                <div class="panel-inner">
                    <div class="panel-header">
                        <h3>Jar 插件列表</h3>
                        <div class="actions">
                            <button class="mdui-btn mdui-ripple mdui-color-theme" onclick="loadJarPlugins()">刷新</button>
                            <button class="mdui-btn mdui-btn-raised mdui-ripple mdui-color-theme-accent" onclick="openJarUploadModal()">上传 Jar</button>
                        </div>
                    </div>
                    <div class="table-wrap">
                        <table>
                            <thead><tr><th>pluginId</th><th>版本</th><th>状态</th><th>路由数</th><th>Bean数</th><th>操作</th></tr></thead>
                            <tbody id="jarBody"><tr class="empty-row"><td colspan="6">加载中...</td></tr></tbody>
                        </table>
                    </div>
                </div>
            </div>
        </section>

        {{-- ===== Java 插件管理 ===== --}}
        <section id="sec-java" class="section">
            {{-- 插件列表 --}}
            <div class="mdui-card panel">
                <div class="panel-inner">
                    <div class="panel-header">
                        <h3>Java 插件列表</h3>
                        <div class="actions">
                            <button class="mdui-btn mdui-ripple mdui-color-theme" onclick="loadJavaPlugins()">刷新</button>
                        </div>
                    </div>
                    <div class="table-wrap">
                        <table>
                            <thead><tr><th>pluginId</th><th>状态</th><th>错误信息</th><th>操作</th></tr></thead>
                            <tbody id="javaBody"><tr class="empty-row"><td colspan="4">加载中...</td></tr></tbody>
                        </table>
                    </div>
                </div>
            </div>

            {{-- 添加 Java 插件：在线编辑器 + 文件上传 --}}
            <div class="mdui-card panel">
                <div class="panel-inner">
                    <div class="panel-header">
                        <h3>添加 Java 插件</h3>
                    </div>

                    {{-- pluginId 输入 --}}
                    <div class="mdui-textfield" style="max-width:400px;margin-bottom:16px;">
                        <label class="mdui-textfield-label">pluginId（插件唯一标识）</label>
                        <input class="mdui-textfield-input" type="text" id="javaPluginId" placeholder="例如：my-plugin">
                    </div>

                    {{-- 切换标签 --}}
                    <div class="mdui-tab" mdui-tab>
                        <a href="#tab-editor" class="mdui-ripple">在线编辑源码</a>
                        <a href="#tab-upload" class="mdui-ripple">上传 Java 文件</a>
                    </div>

                    {{-- Tab 1: 在线代码编辑器 --}}
                    <div id="tab-editor" style="padding-top:16px;">
                        <div style="border:1px solid #e0e0e0;border-radius:4px;overflow:hidden;">
                            <textarea id="javaCodeEditor"></textarea>
                        </div>
                        <p class="hint" style="margin-top:8px;">
                            在编辑器中编写 Java 源码（支持含 <code>run()</code> 或 <code>main()</code> 方法的类），
                            点击下方按钮直接编译注册为插件。
                        </p>
                        <button class="mdui-btn mdui-btn-raised mdui-ripple mdui-color-theme-accent" onclick="registerJavaFromEditor()">
                            <i class="mdui-icon material-icons">cloud_upload</i> 编译并注册
                        </button>
                    </div>

                    {{-- Tab 2: 文件上传 --}}
                    <div id="tab-upload" style="padding-top:16px;">
                        <div class="mdui-textfield" style="margin-bottom:16px;">
                            <input class="mdui-textfield-input" type="file" id="javaFileInput" accept=".java">
                            <label class="mdui-textfield-label">选择 .java 文件</label>
                        </div>
                        <p class="hint" style="margin-top:8px;">
                            上传一个 <code>.java</code> 源文件，系统将编译并注册为插件。
                        </p>
                        <button class="mdui-btn mdui-btn-raised mdui-ripple mdui-color-theme-accent" onclick="registerJavaFromFile()">
                            <i class="mdui-icon material-icons">file_upload</i> 上传并注册
                        </button>
                    </div>
                </div>
            </div>
        </section>

        {{-- ===== 多租户管理 ===== --}}
        <section id="sec-tenant" class="section">
            <div class="mdui-card panel">
                <div class="panel-inner">
                    <div class="panel-header">
                        <h3>多租户状态</h3>
                        <button class="mdui-btn mdui-ripple mdui-color-theme" onclick="loadTenantStatus()">刷新</button>
                    </div>
                    <div id="tenantStatusBox" class="status-grid">加载中...</div>
                </div>
            </div>
            <div class="mdui-card panel">
                <div class="panel-inner">
                    <div class="panel-header">
                        <h3>租户插件列表</h3>
                        <div class="section-toolbar">
                            <div class="mdui-textfield" style="min-width:220px;flex:0 0 auto">
                                <input class="mdui-textfield-input" id="tenantIdInput" type="text" placeholder="请输入租户ID (tenantId)">
                            </div>
                            <button class="mdui-btn mdui-ripple mdui-color-theme" onclick="loadTenantPlugins()">查询</button>
                        </div>
                    </div>
                    <div class="table-wrap">
                        <table>
                            <thead><tr><th>pluginId</th><th>状态</th><th>操作</th></tr></thead>
                            <tbody id="tenantPluginsBody"><tr class="empty-row"><td colspan="3">请输入租户ID后查询</td></tr></tbody>
                        </table>
                    </div>
                </div>
            </div>

            {{-- 通用接口上传：上传 JAR 文件为租户注册插件 --}}
            <div class="mdui-card panel">
                <div class="panel-inner">
                    <div class="panel-header">
                        <h3>通用接口上传</h3>
                    </div>
                    <p class="hint" style="margin-bottom:16px;">
                        上传 JAR 插件文件，系统会自动为指定租户注册插件，Bean 名称和路由路径自动按租户前缀化。
                        其他应用可通过 <code>/{tenantId}/...</code> 路径跨应用调用。
                        请求：<code>POST /api/multi-tenant/tenants/{tenantId}/upload</code>
                    </p>
                    <div class="mdui-textfield" style="max-width:300px;margin-bottom:16px;">
                        <label class="mdui-textfield-label">租户ID（与上方查询共用）</label>
                        <input class="mdui-textfield-input" type="text" id="uploadTenantId" placeholder="例如：acme">
                    </div>
                    <div class="mdui-textfield" style="max-width:300px;margin-bottom:16px;">
                        <label class="mdui-textfield-label">pluginId（可选，留空则用文件名）</label>
                        <input class="mdui-textfield-input" type="text" id="uploadPluginId" placeholder="例如：blog-service">
                    </div>
                    <div class="mdui-textfield" style="margin-bottom:16px;">
                        <input class="mdui-textfield-input" type="file" id="tenantJarFile" accept=".jar">
                        <label class="mdui-textfield-label">选择 .jar 文件</label>
                    </div>
                    <button class="mdui-btn mdui-btn-raised mdui-ripple mdui-color-theme-accent" onclick="uploadTenantJar()">
                        <i class="mdui-icon material-icons">cloud_upload</i> 上传并注册
                    </button>
                </div>
            </div>
        </section>

        {{-- ===== 远程执行 ===== --}}
        <section id="sec-remote" class="section">
            <div class="mdui-card panel">
                <div class="panel-inner">
                    <div class="panel-header">
                        <h3>远程执行状态</h3>
                        <button class="mdui-btn mdui-ripple mdui-color-theme" onclick="loadRemoteStatus()">刷新</button>
                    </div>
                    <div id="remoteStatusBox" class="status-grid">加载中...</div>
                </div>
            </div>
            <div class="mdui-card panel">
                <div class="panel-inner">
                    <div class="panel-header">
                        <h3>子节点列表</h3>
                        <div class="actions">
                            <button class="mdui-btn mdui-ripple mdui-color-theme" onclick="loadSubServers()">刷新</button>
                            <button class="mdui-btn mdui-btn-raised mdui-ripple mdui-color-theme-accent" onclick="openSubServerModal()">注册子节点</button>
                        </div>
                    </div>
                    <div class="table-wrap">
                        <table>
                            <thead><tr><th>subServerId</th><th>host</th><th>port</th><th>连接状态</th><th>操作</th></tr></thead>
                            <tbody id="subServersBody"><tr class="empty-row"><td colspan="5">加载中...</td></tr></tbody>
                        </table>
                    </div>
                </div>
            </div>
        </section>
    </div>
</div>

{{-- ===================== 通用对话框（mdui 1.x） ===================== --}}
<div class="mdui-dialog" id="modalDialog">
    <div class="mdui-dialog-title">标题</div>
    <div class="mdui-dialog-content">
        <form id="modalForm"></form>
    </div>
    <div class="mdui-dialog-actions">
        <button class="mdui-btn mdui-ripple" id="modalCancelBtn">取消</button>
        <button class="mdui-btn mdui-ripple mdui-color-theme-accent" id="modalSubmitBtn">确定</button>
    </div>
</div>

{{-- ===================== Toast 回退容器 ===================== --}}
<div id="toast" class="toast"></div>
@endsection

@section('scripts')
<script>
/* ===================== 全局状态 ===================== */
let token = localStorage.getItem('admin_token');
let currentAdmin = JSON.parse(localStorage.getItem('admin_info') || 'null');
let modalSubmitHandler = null;
let modalDialogInst = null;
/* 自带 captcha 模块 */
let captchaKey = null;
let captchaInput = null;
let captchaInstance = null;

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

// Toast 提示（优先使用 mdui.snackbar，回退到自定义 toast）
let toastTimer = null;
function toast(msg, type) {
    type = type || 'info';
    if (typeof mdui !== 'undefined' && typeof mdui.snackbar === 'function') {
        try {
            mdui.snackbar({ message: msg, position: 'top' });
            return;
        } catch (e) { /* 回退到自定义 toast */ }
    }
    const el = document.getElementById('toast');
    if (!el) return;
    el.textContent = msg;
    el.className = 'toast ' + type + ' show';
    clearTimeout(toastTimer);
    toastTimer = setTimeout(() => el.classList.remove('show'), 3000);
}

// 确认对话框（mdui 1.x 使用回调式 confirm，包装为 Promise）
async function confirmDialog(msg) {
    if (typeof mdui !== 'undefined' && typeof mdui.confirm === 'function') {
        return await new Promise(function (resolve) {
            mdui.confirm(msg, '请确认', function () { resolve(true); }, function () { resolve(false); });
        });
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

/* ===================== 自带 captcha 模块（OOP API） ===================== */
// 流程：点击登录 → 弹出验证码 → 完成后自动提交登录
let captchaKey = null;
let captchaInput = null;
let captchaInstance = null;
let loginPending = false;  // 标记是否正在等待验证码完成以执行登录

function showCaptchaForLogin() {
    // 显示验证码区域
    document.getElementById('captchaArea').style.display = 'block';
    document.getElementById('captchaStatus').innerHTML = '请完成验证后自动登录';

    // 每次都重新创建实例（避免旧 captchaKey 已过期）
    if (captchaInstance) {
        captchaInstance.destroy();
        captchaInstance = null;
    }
    captchaKey = null;
    captchaInput = null;

    captchaInstance = Captcha.init('captchaContainer', {
        type: 'rotate',
        autoVerify: false,
        onComplete: function(key, input) {
            captchaKey = key;
            captchaInput = input;
            document.getElementById('captchaStatus').innerHTML =
                '<i class="mdui-icon material-icons" style="color:#4caf50">check_circle</i> 验证完成，正在登录...';
            // 验证码完成 → 自动提交登录
            if (loginPending) {
                submitLogin();
            }
        }
    });
    // 构造函数已自动加载验证码，不需要再调 refresh()
}

/* ===================== 登录 / 退出 ===================== */
function showLogin() {
    document.getElementById('loginView').style.display = 'flex';
    document.getElementById('mainView').style.display = 'none';
    if (typeof jaravelDrawer !== 'undefined') jaravelDrawer.close();
    // 重置验证码状态
    captchaKey = null;
    captchaInput = null;
    loginPending = false;
    document.getElementById('captchaArea').style.display = 'none';
    document.getElementById('captchaStatus').innerHTML = '请完成验证后自动登录';
    if (captchaInstance) { captchaInstance.destroy(); captchaInstance = null; }
}

function showMain() {
    document.getElementById('loginView').style.display = 'none';
    document.getElementById('mainView').style.display = 'block';
    updateAdminUI();
    showSection('sec-dashboard');
    if (typeof jaravelDrawer !== 'undefined') jaravelDrawer.open();
}

// 更新顶部栏管理员信息
function updateAdminUI() {
    const name = pick(currentAdmin, 'nickname', 'username', 'name') || '管理员';
    document.getElementById('adminName').textContent = name;
    const initial = (String(name).charAt(0) || 'A').toUpperCase();
    document.getElementById('adminAvatar').textContent = initial;
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

// 登录表单提交（由登录按钮触发）
document.getElementById('loginForm').addEventListener('submit', async (e) => {
    e.preventDefault();
    doLogin();
});

// 登录按钮点击
document.getElementById('loginBtn').addEventListener('click', (e) => {
    e.preventDefault();
    doLogin();
});

/**
 * 登录流程：
 * 1. 校验用户名密码已填
 * 2. 如果验证码未完成 → 弹出验证码，等 onComplete 回调自动提交
 * 3. 如果验证码已完成 → 直接提交登录
 */
function doLogin() {
    const username = document.getElementById('loginUsername').value.trim();
    const password = document.getElementById('loginPassword').value;
    const alertBox = document.getElementById('loginAlert');
    alertBox.innerHTML = '';

    if (!username || !password) {
        alertBox.innerHTML = '<div class="alert-error">请输入用户名和密码</div>';
        return;
    }

    // 如果验证码还没完成 → 弹出验证码
    if (!captchaKey || !captchaInput) {
        loginPending = true;
        showCaptchaForLogin();
        return;
    }

    // 验证码已完成 → 提交登录
    submitLogin();
}

/**
 * 提交登录请求（携带验证码数据）
 */
async function submitLogin() {
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
            body: JSON.stringify({
                username: username,
                password: password,
                captchaType: 'rotate',
                captchaKey: captchaKey,
                captchaInput: captchaInput
            })
        });
        token = pick(data, 'token', 'accessToken', 'access_token');
        if (!token) throw new Error('登录返回数据异常：未获取到 token');
        localStorage.setItem('admin_token', token);
        currentAdmin = pick(data, 'admin', 'adminInfo', 'user') || data;
        localStorage.setItem('admin_info', JSON.stringify(currentAdmin));
        await loadAdminMe();
        toast('登录成功', 'success');
        loginPending = false;
        showMain();
    } catch (err) {
        alertBox.innerHTML = '<div class="alert-error">' + esc(err.message) + '</div>';
        // 登录失败：重置验证码，用户可重新点登录
        captchaKey = null;
        captchaInput = null;
        loginPending = false;
        document.getElementById('captchaArea').style.display = 'none';
        if (captchaInstance) { captchaInstance.destroy(); captchaInstance = null; }
        document.getElementById('captchaStatus').innerHTML = '请完成验证后自动登录';
    } finally {
        btn.disabled = false;
        btn.textContent = oldText;
    }
}

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
    document.querySelectorAll('.drawer-nav .mdui-list-item').forEach(a => {
        if (a.dataset.section === id) {
            a.classList.add('mdui-list-item-active');
        } else {
            a.classList.remove('mdui-list-item-active');
        }
    });
    document.getElementById('pageTitle').textContent = sectionTitles[id] || '';
    if (sectionLoaders[id]) {
        try { sectionLoaders[id](); } catch (e) { console.error(e); }
    }
    // 初始化 Java 代码编辑器
    if (id === 'sec-java') {
        setTimeout(initJavaCodeEditor, 100);
    }
}

document.querySelectorAll('.drawer-nav .mdui-list-item').forEach(item => {
    item.addEventListener('click', () => showSection(item.dataset.section));
});

/* ===================== 通用模态对话框（mdui 1.x Dialog） ===================== */
// 构建单个表单字段 HTML（mdui-textfield；文件类型用原生 input）
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
        return '<div class="mdui-textfield"><label class="mdui-textfield-label">' + label + '</label>' +
            '<textarea class="mdui-textfield-input" name="' + f.name + '" rows="3" placeholder="' + ph + '"' + req + '></textarea></div>';
    }
    const type = f.type || 'text';
    return '<div class="mdui-textfield"><label class="mdui-textfield-label">' + label + '</label>' +
        '<input class="mdui-textfield-input" name="' + f.name + '" type="' + type + '" placeholder="' + ph + '"' + req + '></div>';
}

function getModalDialog() {
    if (!modalDialogInst) {
        modalDialogInst = new mdui.Dialog('#modalDialog');
    }
    return modalDialogInst;
}

function openModal(opts) {
    const title = opts.title || '标题';
    const fields = opts.fields || [];
    const submitText = opts.submitText || '确定';
    const onSubmit = opts.onSubmit;
    document.querySelector('#modalDialog .mdui-dialog-title').textContent = title;
    document.getElementById('modalSubmitBtn').textContent = submitText;
    const form = document.getElementById('modalForm');
    form.innerHTML = fields.map(fieldHtml).join('');
    // 动态插入 textfield / 控件后，通知 mdui 重新初始化
    if (typeof mdui !== 'undefined') {
        mdui.updateTextFields();
        mdui.mutation();
    }
    modalSubmitHandler = onSubmit;
    const inst = getModalDialog();
    inst.open();
    try { inst.handleUpdate(); } catch (e) {}
}

function closeModal() {
    try { getModalDialog().close(); } catch (e) {}
    modalSubmitHandler = null;
    document.getElementById('modalForm').innerHTML = '';
}

// 对话框关闭后清理提交句柄（兼容遮罩 / ESC 关闭）
document.getElementById('modalDialog').addEventListener('closed.mdui.dialog', function () {
    modalSubmitHandler = null;
});

// 收集表单值（原生 input/textarea 的 .value 与原生 file 的 .files）
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

/* ===================== 表格空状态 / 状态标签 / 操作按钮 ===================== */
function emptyRow(colspan, msg) {
    msg = msg || '暂无数据';
    return '<tr class="empty-row"><td colspan="' + colspan + '">' + esc(msg) + '</td></tr>';
}

// 状态标签（自定义 badge span）
function statusChip(active, okText, badText) {
    return '<span class="badge ' + (active ? 'badge-success' : 'badge-error') + '">' +
        (active ? okText : badText) + '</span>';
}

// 表格内紧凑操作按钮
function actBtn(label, onclickAttr) {
    return '<button class="mdui-btn mdui-btn-dense mdui-ripple mdui-color-theme btn-action" onclick="' + onclickAttr + '">' + label + '</button>';
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
        el.style.color = compiler ? '#4caf50' : '#f44336';

        document.getElementById('stJavaVersion').textContent = pick(d, 'java_version', 'javaVersion') || '--';
        document.getElementById('stJarTotal').textContent = (pick(d, 'jar_plugin_total', 'jarPluginTotal') ?? '--');
        document.getElementById('stJarEnabled').textContent = (pick(d, 'jar_plugin_enabled', 'jarPluginEnabled') ?? '--');

        const sys = pick(d, 'java_plugin_system', 'javaPluginSystem');
        const elSys = document.getElementById('stJavaSystem');
        elSys.textContent = (sys === 'enabled' || sys === true) ? '已启用' : '未启用';
        elSys.style.color = (sys === 'enabled' || sys === true) ? '#4caf50' : '#f44336';

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
                '<td>' + actBtn(active ? '禁用' : '启用', 'toggleAdmin(' + esc(id) + ', ' + !active + ')') + '</td>' +
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
            '<td>' + actBtn('删除', 'deleteRole(' + pick(r, 'id') + ')') + '</td>' +
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
            '<td>' + actBtn('删除', 'deleteUser(' + pick(u, 'id') + ')') + '</td>' +
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
                '<td>' + actBtn(active ? '禁用' : '启用', 'toggleJar(\'' + esc(pid) + '\', ' + !active + ')') + '</td>' +
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
                '<td>' + actBtn('重载', 'reloadJava(\'' + esc(pid) + '\')') + '</td>' +
                '</tr>';
        }).join('');
    } catch (e) {
        body.innerHTML = emptyRow(4, '加载失败：' + esc(e.message));
    }
}

/* ---- Java 插件：CodeMirror 编辑器 + 文件上传 ---- */
var javaCodeMirror = null;

// 初始化 CodeMirror 编辑器（在 showSection 切换到 java 时调用）
function initJavaCodeEditor() {
    if (javaCodeMirror) return;
    var ta = document.getElementById('javaCodeEditor');
    if (!ta || typeof CodeMirror === 'undefined') return;
    javaCodeMirror = CodeMirror.fromTextArea(ta, {
        mode: 'text/x-java',
        theme: 'material',
        lineNumbers: true,
        indentUnit: 4,
        tabSize: 4,
        height: '400px'
    });
    javaCodeMirror.setSize('100%', '400px');
    javaCodeMirror.setValue('public class Hello {\n    public String run() {\n        return "Hello from jaravel!";\n    }\n}');
}

// 从编辑器编译并注册
async function registerJavaFromEditor() {
    var pluginId = document.getElementById('javaPluginId').value.trim();
    if (!pluginId) { toast('请输入 pluginId', 'error'); return; }
    if (!javaCodeMirror) { toast('编辑器未初始化', 'error'); return; }
    var code = javaCodeMirror.getValue();
    if (!code.trim()) { toast('源码不能为空', 'error'); return; }
    try {
        var res = await apiPost('/api/plugin/java/run', { code: code, in_memory: true });
        // 编译成功后注册为插件
        await apiPost('/api/plugins/java/register', { pluginId: pluginId, sourcePath: pluginId });
        toast('Java 插件注册成功', 'success');
        loadJavaPlugins();
    } catch (e) { toast(e.message, 'error'); }
}

// 从文件上传并注册
async function registerJavaFromFile() {
    var pluginId = document.getElementById('javaPluginId').value.trim();
    if (!pluginId) { toast('请输入 pluginId', 'error'); return; }
    var fileInput = document.getElementById('javaFileInput');
    if (!fileInput || !fileInput.files.length) { toast('请选择 .java 文件', 'error'); return; }
    var file = fileInput.files[0];
    var reader = new FileReader();
    reader.onload = async function(e) {
        var code = e.target.result;
        try {
            var res = await apiPost('/api/plugin/java/run', { code: code, in_memory: true });
            await apiPost('/api/plugins/java/register', { pluginId: pluginId, sourcePath: pluginId });
            toast('Java 插件注册成功', 'success');
            loadJavaPlugins();
        } catch (err) { toast(err.message, 'error'); }
    };
    reader.readAsText(file);
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
        box.innerHTML = '<div class="mdui-card stat-card" style="grid-column:span 2"><div class="inner">' +
            items.map(([k, v]) => '<div class="info-row"><span class="k">' + esc(k) + '</span><span class="v">' + esc(v) + '</span></div>').join('') +
            '</div></div>';
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
                '<td>' + actBtn(active ? '禁用' : '启用', 'toggleTenantPlugin(\'' + esc(tenantId) + '\',\'' + esc(pid) + '\',' + !active + ')') + '</td>' +
                '</tr>';
        }).join('');
    } catch (e) {
        body.innerHTML = emptyRow(3, '加载失败：' + esc(e.message));
    }
}

async function uploadTenantJar() {
    var tenantId = document.getElementById('uploadTenantId').value.trim();
    if (!tenantId) {
        tenantId = document.getElementById('tenantIdInput').value.trim();
    }
    if (!tenantId) { toast('请输入租户ID', 'error'); return; }
    var pluginId = document.getElementById('uploadPluginId').value.trim();
    var fileInput = document.getElementById('tenantJarFile');
    if (!fileInput || !fileInput.files.length) { toast('请选择 .jar 文件', 'error'); return; }
    var file = fileInput.files[0];

    var formData = new FormData();
    formData.append('file', file);
    if (pluginId) formData.append('pluginId', pluginId);

    try {
        var token = localStorage.getItem('adminToken');
        var resp = await fetch('/api/multi-tenant/tenants/' + tenantId + '/upload', {
            method: 'POST',
            headers: { 'Authorization': 'Bearer ' + token },
            body: formData
        });
        var data = await resp.json();
        if (data.code !== 0 && data.code !== 200) throw new Error(data.message || '上传失败');
        toast('插件已上传并注册，路由前缀：/' + tenantId + '/...', 'success');
        document.getElementById('tenantIdInput').value = tenantId;
        loadTenantPlugins();
    } catch (e) { toast(e.message, 'error'); }
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
        box.innerHTML = '<div class="mdui-card stat-card" style="grid-column:span 2"><div class="inner">' +
            items.map(([k, v]) => '<div class="info-row"><span class="k">' + esc(k) + '</span><span class="v">' + esc(v) + '</span></div>').join('') +
            '</div></div>';
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
                '<td>' + actBtn(on ? '断开' : '连接', 'toggleSubServer(\'' + esc(sid) + '\',' + !on + ')') + '</td>' +
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