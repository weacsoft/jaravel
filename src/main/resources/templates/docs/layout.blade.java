<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>{{ $title ?? 'Jaravel Demo 文档' }}</title>
    <style>
        * { margin: 0; padding: 0; box-sizing: border-box; }
        body {
            font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif;
            color: #333; background: #f8f9fa; line-height: 1.7;
        }
        .layout { display: flex; min-height: 100vh; }
        .sidebar {
            width: 260px; background: #2c3e50; color: #ecf0f1;
            padding: 20px 0; position: fixed; top: 0; left: 0;
            height: 100vh; overflow-y: auto;
        }
        .sidebar .logo { padding: 0 24px 20px; font-size: 20px; font-weight: 700; color: #fff; border-bottom: 1px solid #34495e; margin-bottom: 16px; }
        .sidebar .logo small { display: block; font-size: 12px; color: #95a5a6; font-weight: 400; margin-top: 4px; }
        .sidebar .nav-group { margin-bottom: 16px; }
        .sidebar .nav-group-title { padding: 6px 24px; font-size: 12px; text-transform: uppercase; color: #7f8c8d; letter-spacing: 1px; }
        .sidebar .nav-item { display: block; padding: 8px 24px; color: #bdc3c7; text-decoration: none; font-size: 14px; transition: all .2s; }
        .sidebar .nav-item:hover { background: #34495e; color: #fff; }
        .content { flex: 1; margin-left: 260px; padding: 40px 48px; max-width: 960px; }
        .content h1 { font-size: 32px; color: #2c3e50; margin-bottom: 8px; border-bottom: 2px solid #3498db; padding-bottom: 12px; }
        .content h2 { font-size: 24px; color: #2c3e50; margin: 32px 0 12px; }
        .content h3 { font-size: 18px; color: #34495e; margin: 24px 0 8px; }
        .content p { margin-bottom: 12px; }
        .content ul, .content ol { margin: 12px 0 12px 24px; }
        .content li { margin-bottom: 6px; }
        .content code { background: #eef; padding: 2px 6px; border-radius: 3px; font-family: "Fira Code", Consolas, monospace; font-size: 13px; color: #c0392b; }
        .content pre { background: #1e1e1e; color: #d4d4d4; padding: 16px 20px; border-radius: 6px; overflow-x: auto; margin: 12px 0; font-size: 13px; line-height: 1.5; }
        .content pre code { background: none; color: inherit; padding: 0; }
        .content .note { background: #fff3cd; border-left: 4px solid #ffc107; padding: 12px 16px; margin: 16px 0; border-radius: 0 4px 4px 0; }
        .content .note strong { color: #856404; }
        .content .tip { background: #d1ecf1; border-left: 4px solid #17a2b8; padding: 12px 16px; margin: 16px 0; border-radius: 0 4px 4px 0; }
        .content .tip strong { color: #0c5460; }
        .content .warn { background: #f8d7da; border-left: 4px solid #dc3545; padding: 12px 16px; margin: 16px 0; border-radius: 0 4px 4px 0; }
        .content .warn strong { color: #721c24; }
        .content table { width: 100%; border-collapse: collapse; margin: 16px 0; }
        .content th, .content td { border: 1px solid #dee2e6; padding: 8px 12px; text-align: left; }
        .content th { background: #e9ecef; font-weight: 600; }
        .content .feature-list { list-style: none; margin: 16px 0; }
        .content .feature-list li { padding: 8px 0; border-bottom: 1px solid #eee; }
        .content .feature-list li:before { content: ">"; color: #3498db; font-weight: bold; margin-right: 8px; }
        .content .footer { margin-top: 48px; padding-top: 20px; border-top: 1px solid #ddd; color: #999; font-size: 13px; }
    </style>
</head>
<body>
<div class="layout">
    <div class="sidebar">
        <div class="logo">
            Jaravel Demo
            <small>v0.1.0 文档</small>
        </div>
        <div class="nav-group">
            <div class="nav-group-title">入门</div>
            <a class="nav-item" href="/docs">首页</a>
            <a class="nav-item" href="/docs/installation">安装指南</a>
        </div>
        <div class="nav-group">
            <div class="nav-group-title">核心功能</div>
            <a class="nav-item" href="/docs/routing">路由</a>
            <a class="nav-item" href="/docs/eloquent">Eloquent ORM</a>
            <a class="nav-item" href="/docs/auth">认证</a>
            <a class="nav-item" href="/docs/cache">缓存</a>
            <a class="nav-item" href="/docs/events">事件系统</a>
        </div>
        <div class="nav-group">
            <div class="nav-group-title">命令与任务</div>
            <a class="nav-item" href="/docs/artisan">Artisan CLI</a>
            <a class="nav-item" href="/docs/schedule">定时任务</a>
            <a class="nav-item" href="/docs/queue">队列</a>
        </div>
        <div class="nav-group">
            <div class="nav-group-title">扩展</div>
            <a class="nav-item" href="/docs/plugins">插件系统</a>
        </div>
    </div>
    <div class="content">
        @yield('content')
        <div class="footer">
            Jaravel Demo Project v0.1.0 | MIT License
        </div>
    </div>
</div>
</body>
</html>
