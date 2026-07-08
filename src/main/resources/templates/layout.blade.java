<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1, maximum-scale=1, shrink-to-fit=no">
    <meta name="renderer" content="webkit">
    <title>@yield('title', 'jaravel')</title>
    {{-- mdui 1.x Material Design 1 框架 --}}
    <link rel="stylesheet" href="https://unpkg.com/mdui@1.0.2/dist/css/mdui.min.css">
    <style>
        .container { max-width: 1200px; margin: 0 auto; padding: 24px; }
        .page-content { padding: 24px 0; }
        .card-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(300px, 1fr)); gap: 16px; margin-top: 16px; }
        .stat-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(160px, 1fr)); gap: 12px; margin-top: 16px; }
        .code-editor { width: 100%; min-height: 300px; font-family: 'Cascadia Code', 'Fira Code', monospace; font-size: 14px; }
        .result-box { margin-top: 16px; padding: 16px; border-radius: 4px; background: #f5f5f5; white-space: pre-wrap; word-break: break-all; font-family: monospace; font-size: 14px; }
        .result-box.success { border-left: 4px solid #4caf50; }
        .result-box.error { border-left: 4px solid #f44336; }
        .hint { font-size: 13px; color: #757575; margin-top: 8px; line-height: 1.6; }
        .hint code { background: #f5f5f5; padding: 2px 6px; border-radius: 2px; font-size: 12px; }
        .section-title { font-size: 20px; font-weight: 500; margin: 24px 0 8px; }
        .section-title:first-child { margin-top: 0; }
        table.data-table { width: 100%; border-collapse: collapse; margin-top: 12px; }
        table.data-table th, table.data-table td { text-align: left; padding: 10px 12px; border-bottom: 1px solid #e0e0e0; font-size: 14px; }
        table.data-table th { font-weight: 500; color: #757575; }
        .header-controls { display: flex; align-items: center; gap: 8px; }
        .json-viewer { background: #f5f5f5; border-radius: 4px; padding: 12px; overflow-x: auto; font-family: monospace; font-size: 13px; max-height: 400px; overflow-y: auto; }
        .badge { display: inline-block; padding: 2px 8px; border-radius: 2px; font-size: 12px; }
        .badge-success { background: #c8e6c9; color: #1b5e20; }
        .badge-error { background: #ffcdd2; color: #b71c1c; }
        .hidden { display: none; }
        footer { text-align: center; padding: 32px 24px; color: #9e9e9e; font-size: 13px; }
        .nav-link { color: inherit; text-decoration: none; }
    </style>
    @yield('head')
</head>
<body class="mdui-appbar-with-toolbar">
    {{-- 顶部应用栏 --}}
    <div class="mdui-appbar mdui-appbar-fixed">
        <div class="mdui-toolbar mdui-color-theme">
            <a href="/" class="mdui-btn mdui-btn-icon">
                <i class="mdui-icon material-icons">code</i>
            </a>
            <a href="/" class="mdui-typo-title">{{ $appName ?? 'jaravel' }}</a>
            <div class="mdui-toolbar-spacer"></div>
            <a href="/" class="mdui-btn mdui-ripple">
                <i class="mdui-icon mdui-icon-left material-icons">home</i> 首页
            </a>
            <a href="/admin" class="mdui-btn mdui-ripple">
                <i class="mdui-icon mdui-icon-left material-icons">admin_panel_settings</i> 管理后台
            </a>
            <a href="/user" class="mdui-btn mdui-ripple">
                <i class="mdui-icon mdui-icon-left material-icons">person</i> 用户中心
            </a>
        </div>
    </div>

    {{-- 页面主体 --}}
    <div class="container">
        @yield('content')
    </div>

    <footer>
        jaravel v0.1.2 &mdash; Powered by jblade 模板引擎 + mdui Material Design 1
    </footer>

    {{-- mdui 1.x JavaScript --}}
    <script src="https://unpkg.com/mdui@1.0.2/dist/js/mdui.min.js"></script>

    {{-- 页面专属脚本 --}}
    @yield('scripts')
</body>
</html>
