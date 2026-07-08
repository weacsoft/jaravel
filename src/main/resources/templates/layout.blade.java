<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>@yield('title', 'jaravel')</title>
    {{-- mdui Material Design 3 框架 --}}
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/mdui@2/mdui.css">
    <script src="https://cdn.jsdelivr.net/npm/mdui@2/mdui.global.js"></script>
    {{-- 自定义样式 --}}
    <style>
        body { margin: 0; font-family: var(--mdui-typescale-body-large-font); background: var(--mdui-color-surface); color: var(--mdui-color-on-surface); }
        .app-bar { position: sticky; top: 0; z-index: 100; }
        .container { max-width: 1200px; margin: 0 auto; padding: 24px; }
        .page-content { padding: 24px 0; }
        .card-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(300px, 1fr)); gap: 16px; margin-top: 16px; }
        .stat-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(160px, 1fr)); gap: 12px; margin-top: 16px; }
        .code-editor { width: 100%; min-height: 300px; font-family: 'Cascadia Code', 'Fira Code', monospace; font-size: 14px; }
        .result-box { margin-top: 16px; padding: 16px; border-radius: 12px; background: var(--mdui-color-surface-variant); white-space: pre-wrap; word-break: break-all; font-family: monospace; font-size: 14px; }
        .result-box.success { border-left: 4px solid var(--mdui-color-primary); }
        .result-box.error { border-left: 4px solid var(--mdui-color-error); }
        .hint { font-size: 13px; color: var(--mdui-color-on-surface-variant); margin-top: 8px; line-height: 1.6; }
        .hint code { background: var(--mdui-color-surface-variant); padding: 2px 6px; border-radius: 4px; font-size: 12px; }
        .section-title { font-size: 20px; font-weight: 500; margin: 24px 0 8px; }
        .section-title:first-child { margin-top: 0; }
        table.data-table { width: 100%; border-collapse: collapse; margin-top: 12px; }
        table.data-table th, table.data-table td { text-align: left; padding: 10px 12px; border-bottom: 1px solid var(--mdui-color-outline-variant); font-size: 14px; }
        table.data-table th { font-weight: 500; color: var(--mdui-color-on-surface-variant); }
        .header-controls { display: flex; align-items: center; gap: 8px; }
        .json-viewer { background: var(--mdui-color-surface-variant); border-radius: 8px; padding: 12px; overflow-x: auto; font-family: monospace; font-size: 13px; max-height: 400px; overflow-y: auto; }
        .badge { display: inline-block; padding: 2px 8px; border-radius: 4px; font-size: 12px; }
        .badge-success { background: var(--mdui-color-primary-container); color: var(--mdui-color-on-primary-container); }
        .badge-error { background: var(--mdui-color-error-container); color: var(--mdui-color-on-error-container); }
        .hidden { display: none; }
        footer { text-align: center; padding: 32px 24px; color: var(--mdui-color-on-surface-variant); font-size: 13px; }
    </style>
    @yield('head')
</head>
<body>
    {{-- 顶部导航栏 --}}
    <mdui-top-app-bar class="app-bar">
        <mdui-top-app-bar-title>{{ $appName ?? 'jaravel' }}</mdui-top-app-bar-title>
        <div style="display:flex;gap:8px;">
            <mdui-button variant="text" href="/">首页</mdui-button>
            <mdui-button variant="text" href="/admin">管理后台</mdui-button>
            <mdui-button variant="text" href="/user">用户中心</mdui-button>
        </div>
    </mdui-top-app-bar>

    {{-- 页面主体 --}}
    <div class="container">
        @yield('content')
    </div>

    <footer>
        jaravel v0.1.0 &mdash; Powered by jblade 模板引擎 + mdui Material Design 3
    </footer>

    {{-- 页面专属脚本 --}}
    @yield('scripts')
</body>
</html>
