<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>@yield('title', 'Wire Demo')</title>
    <link rel="stylesheet" href="@asset('css/mdui.min.css')">
    <style>
        * { margin:0; padding:0; box-sizing:border-box; }
        body { font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif; background:#f0f2f5; }
        /* 顶栏 */
        .topbar { background:#4a6cf7; color:#fff; padding:0 24px; height:56px; display:flex; align-items:center; font-size:18px; font-weight:700; }
        .topbar .spacer { flex:1; }
        .topbar a { color:#fff; text-decoration:none; margin-left:20px; font-size:14px; opacity:.85; }
        .topbar a:hover { opacity:1; }
        .topbar a.active { opacity:1; border-bottom:2px solid #fff; padding-bottom:2px; }
        /* 布局 */
        .layout-body { display:flex; min-height:calc(100vh - 56px); }
        .sidebar { width:200px; background:#fff; border-right:1px solid #e8ecf1; padding:16px 0; flex-shrink:0; }
        .sidebar a { display:block; padding:10px 20px; color:#333; text-decoration:none; font-size:14px; }
        .sidebar a:hover { background:#eef1ff; color:#4a6cf7; }
        .sidebar a.active { background:#4a6cf7; color:#fff; font-weight:600; }
        .main { flex:1; padding:24px; }
        /* 通用组件 */
        .card { background:#fff; border-radius:12px; padding:20px; margin-bottom:16px; box-shadow:0 1px 3px rgba(0,0,0,.06); }
        .stat-grid { display:grid; grid-template-columns:repeat(auto-fill,minmax(160px,1fr)); gap:12px; }
        .stat-card { background:#fff; border:1px solid #e8ecf1; border-radius:10px; padding:16px; }
        .stat-card .label { font-size:12px; color:#888; margin-bottom:4px; }
        .stat-card .value { font-size:28px; font-weight:700; color:#4a6cf7; }
        .btn { display:inline-flex; align-items:center; gap:4px; padding:8px 16px; border-radius:8px; border:none; cursor:pointer; font-size:13px; font-weight:500; text-decoration:none; }
        .btn-primary { background:#4a6cf7; color:#fff; }
        .btn-danger  { background:#e74c3c; color:#fff; }
        .btn-outline { background:#fff; border:1px solid #ddd; color:#333; }
        table { width:100%; border-collapse:collapse; }
        th { text-align:left; padding:10px; background:#f8f9fb; font-size:12px; color:#888; border-bottom:2px solid #e8ecf1; }
        td { padding:10px; border-bottom:1px solid #e8ecf1; font-size:14px; }
        .badge { display:inline-block; padding:2px 8px; border-radius:10px; font-size:11px; font-weight:600; }
        .badge-success { background:#e8f5e9; color:#27ae60; }
        .badge-warning { background:#fff3e0; color:#f39c12; }
        .hint { font-size:13px; color:#757575; margin:8px 0; line-height:1.6; }
        .hint code { background:#f5f5f5; padding:1px 5px; border-radius:3px; font-size:12px; }
        /* Toast 命名组件 — 固定定位，右下角弹出 */
        .wc-toast {
            position: fixed; right:24px; z-index:9999;
            display:flex; align-items:center; gap:10px;
            padding:14px 20px; border-radius:10px;
            font-size:14px; font-weight:500; line-height:1.4;
            box-shadow:0 4px 16px rgba(0,0,0,.15);
            pointer-events:auto; cursor:default;
            max-width:380px; min-width:240px;
        }
        .wc-toast--info    { background:#e8f4fd; color:#1976d2; border-left:4px solid #1976d2; }
        .wc-toast--success { background:#e8f5e9; color:#2e7d32; border-left:4px solid #2e7d32; }
        .wc-toast--warning { background:#fff8e1; color:#f57f17; border-left:4px solid #f57f17; }
        .wc-toast--error   { background:#fce4ec; color:#c62828; border-left:4px solid #c62828; }
        .wc-toast__icon { font-size:18px; flex-shrink:0; }
        .wc-toast__msg { flex:1; }
        .wc-toast__close {
            flex-shrink:0; background:none; border:none;
            font-size:18px; cursor:pointer; opacity:.5; padding:0 2px;
            color:inherit; line-height:1;
        }
        .wc-toast__close:hover { opacity:1; }
        /* Confirm 确认框 — 遮罩层 + 居中弹窗 */
        .wc-confirm-mask {
            position: fixed; top:0; left:0; right:0; bottom:0;
            background:rgba(0,0,0,.45); z-index:10000;
            display:flex; align-items:center; justify-content:center;
        }
        .wc-confirm {
            background:#fff; border-radius:12px; padding:28px 24px 20px;
            max-width:420px; width:90%;
            box-shadow:0 8px 32px rgba(0,0,0,.2);
            animation:wc-confirm-in .2s ease;
        }
        @keyframes wc-confirm-in {
            from { opacity:0; transform:scale(.92) translateY(8px); }
            to   { opacity:1; transform:scale(1) translateY(0); }
        }
        .wc-confirm__title { font-size:16px; font-weight:700; margin-bottom:10px; color:#333; }
        .wc-confirm__body  { font-size:14px; color:#666; margin-bottom:22px; line-height:1.6; }
        .wc-confirm__actions { display:flex; gap:8px; justify-content:flex-end; }
        .wc-confirm__ok,
        .wc-confirm__cancel { min-width:72px; }
        /* 导航卡片 */
        .nav-grid { display:grid; grid-template-columns:repeat(auto-fill,minmax(220px,1fr)); gap:12px; margin-top:16px; }
        .nav-card { background:#fff; border:1px solid #e8ecf1; border-radius:12px; padding:20px; text-decoration:none; color:#333; display:block; transition:box-shadow .15s, transform .15s; }
        .nav-card:hover { box-shadow:0 4px 12px rgba(74,108,247,.15); transform:translateY(-2px); }
        .nav-card .icon { font-size:28px; margin-bottom:8px; }
        .nav-card .title { font-size:15px; font-weight:600; margin-bottom:4px; }
        .nav-card .desc { font-size:12px; color:#888; line-height:1.5; }
        footer { text-align:center; padding:24px; color:#9e9e9e; font-size:12px; }
    </style>
    @yield('head')
</head>
<body>
    {{-- 顶栏：展示当前页面归属（多 Controller 切换的视觉反馈） --}}
    <div class="topbar">
        jaravel Wire 演示
        <div class="spacer"></div>
        <a href="/wire" wire-navigate class="@if(isset($currentPage) && $currentPage==='dashboard') active @endif">仪表盘</a>
        <a href="/wire/spa"   wire-navigate class="@if(isset($currentPage) && $currentPage==='spa') active @endif">SPA 导航</a>
        <a href="/wire/tasks" wire-navigate class="@if(isset($currentPage) && $currentPage==='tasks') active @endif">CRUD 列表</a>
        <a href="/wire/components" wire-navigate class="@if(isset($currentPage) && $currentPage==='components') active @endif">组件</a>
    </div>

    <div class="layout-body">
        {{-- 侧栏：由各页面 section 填充 --}}
        <div class="sidebar">
            @yield('sidebar')
        </div>

        {{-- 主内容区：由各页面 section 填充 --}}
        <div class="main">
            @yield('content')
        </div>
    </div>

    <footer>jaravel v0.1.2 &mdash; Wire 透明导航演示</footer>

    {{-- Wire 命名组件（toast/confirm）挂载容器 --}}
    {{-- 注意：中间件 WireOutlet 未注册时不会自动注入，此处手动添加 --}}
    <div id="wire-outlet" wire:outlet data-wire-outlet="wire-outlet"></div>

    {{-- Wire 运行时 --}}
    <script src="@asset('js/wire-navigate.js')"></script>
    <script src="@asset('js/wire-lib.js')"></script>
    <script src="@asset('js/wire-component.js')"></script>
    @yield('scripts')
</body>
</html>
