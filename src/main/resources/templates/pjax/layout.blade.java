{{--
    PJAX 演示共享布局。

    零侵入说明：本文件<b>没有任何 pjax 专属标记</b>。
    jblade 在编译期自动分析 @extends / @section / @yield 的继承关系，
    渲染时会在每个 @yield 区域外自动包裹注释锚点
    <!--pjax:start:NAME--> ... <!--pjax:end:NAME-->，
    服务端据此逐区域算指纹、前端据此定位替换。模板作者只需正常写继承即可。

    本布局定义 5 个区域：title / head / sidebar / content / scratch / scripts
    - title、content 每页不同     -> 切换时会被替换
    - sidebar 仅高亮项不同        -> 切换时会被替换
    - head、scratch、scripts 相同 -> 指纹一致，服务端判定未变化，前端完全不碰这段 DOM
--}}
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>@yield('title', 'jaravel PJAX')</title>
    <link rel="stylesheet" href="@asset('css/mdui.min.css')">
    <style>
        body { background: #fafafa; }
        .pjax-wrap { max-width: 1100px; margin: 0 auto; padding: 16px; }
        .pjax-shell { display: flex; gap: 16px; align-items: flex-start; }
        .pjax-side { width: 200px; flex: 0 0 200px; }
        .pjax-main { flex: 1; min-width: 0; }
        .panel { background: #fff; border-radius: 4px; box-shadow: 0 1px 3px rgba(0,0,0,.12); padding: 16px; }
        .panel + .panel { margin-top: 16px; }
        .nav-item { display: block; padding: 10px 14px; border-radius: 4px; color: #424242; text-decoration: none; }
        .nav-item:hover { background: #f0f0f0; }
        .nav-item.active { background: #e8eaf6; color: #3f51b5; font-weight: 600; }
        table.grid { width: 100%; border-collapse: collapse; }
        table.grid th, table.grid td { text-align: left; padding: 8px 10px; border-bottom: 1px solid #eee; font-size: 14px; }
        table.grid th { color: #757575; font-weight: 500; }
        .muted { color: #9e9e9e; font-size: 13px; }
        .tag { display: inline-block; padding: 1px 8px; border-radius: 10px; font-size: 12px; background: #eee; color: #616161; }
        .tag.done { background: #c8e6c9; color: #1b5e20; }
        .pager a, .pager span { display: inline-block; padding: 4px 10px; margin-right: 4px; border-radius: 3px; text-decoration: none; }
        .pager a { color: #3f51b5; background: #f0f0f0; }
        .pager .cur { background: #3f51b5; color: #fff; }
        #probe-input { width: 100%; padding: 8px; border: 1px solid #ccc; border-radius: 4px; font-size: 14px; }
        .kv { font-family: monospace; font-size: 13px; }
    </style>
    @yield('head')
</head>
<body>
    {{-- 静态头部：不在任何 @yield 内，因此不是区域，永远不会被替换 --}}
    <div class="mdui-appbar">
        <div class="mdui-toolbar mdui-color-indigo">
            <span class="mdui-typo-title">jaravel PJAX 无感切换演示</span>
            <div class="mdui-toolbar-spacer"></div>
            <span class="muted" style="color:#c5cae9;">零侵入 · 区域指纹 diff</span>
        </div>
    </div>

    <div class="pjax-wrap">
        <div class="pjax-shell">
            <aside class="pjax-side panel">@yield('sidebar')</aside>
            <main class="pjax-main">@yield('content')</main>
        </div>

        {{-- 状态保持探针：两个页面输出完全一致，因此指纹相同 -> 服务端不下发、前端不替换 --}}
        <div class="panel" style="margin-top:16px;">@yield('scratch')</div>
    </div>

    <script src="@asset('js/mdui.min.js')"></script>
    @yield('scripts')
</body>
</html>
