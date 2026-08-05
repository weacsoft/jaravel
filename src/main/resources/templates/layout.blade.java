<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1, maximum-scale=1, shrink-to-fit=no">
    <meta name="renderer" content="webkit">
    <title>@yield('title', 'jaravel')</title>
    {{-- mdui 1.x Material Design 1（本地资源） --}}
    <link rel="stylesheet" href="@asset('css/mdui.min.css')">
    <style>
        .mdui-container { max-width: 1200px; padding: 24px; }
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
        .json-viewer { background: #f5f5f5; border-radius: 4px; padding: 12px; overflow-x: auto; font-family: monospace; font-size: 13px; max-height: 400px; overflow-y: auto; }
        .badge { display: inline-block; padding: 2px 8px; border-radius: 2px; font-size: 12px; }
        .badge-success { background: #c8e6c9; color: #1b5e20; }
        .badge-error { background: #ffcdd2; color: #b71c1c; }
        .hidden { display: none; }
        {{-- Wire 命名组件（toast / confirm）样式（全局，确保 outlet 内挂载的组件始终有样式） --}}
        .wc-toast{position:fixed; top:16px; right:16px; z-index:9999; display:flex; align-items:center; gap:10px;
            max-width:360px; padding:12px 14px; border-radius:8px; background:#323232; color:#fff;
            box-shadow:0 4px 16px rgba(0,0,0,.24); font-size:14px; line-height:1.4;}
        .wc-toast--success{background:#2e7d32;} .wc-toast--info{background:#1976d2;}
        .wc-toast--warning{background:#f57c00;} .wc-toast--error{background:#c62828;}
        .wc-toast__icon{font-size:18px;} .wc-toast__msg{flex:1;}
        .wc-toast__close{border:0; background:transparent; color:#fff; font-size:18px; cursor:pointer; line-height:1; opacity:.8;}
        .wc-toast__close:hover{opacity:1;}
        .wc-confirm-mask{position:fixed; inset:0; z-index:9998; display:flex; align-items:center; justify-content:center;
            background:rgba(0,0,0,.45); transition:opacity .2s ease;}
        .wc-confirm{width:320px; max-width:90vw; background:#fff; border-radius:10px; padding:20px; box-shadow:0 8px 30px rgba(0,0,0,.3);}
        .wc-confirm__title{font-size:16px; font-weight:600; margin-bottom:8px;}
        .wc-confirm__body{font-size:14px; color:#555; margin-bottom:16px;}
        .wc-confirm__actions{display:flex; justify-content:flex-end; gap:10px;}
        footer { text-align: center; padding: 32px 24px; color: #9e9e9e; font-size: 13px; }
        {{-- 抽屉导航栏内部样式 --}}
        .drawer-header { padding: 16px 16px 8px; }
        .drawer-header .drawer-title { font-size: 18px; font-weight: 600; color: #3f51b5; }
        .drawer-header .drawer-subtitle { font-size: 12px; color: #757575; margin-top: 4px; }
    </style>
    @yield('head')
</head>
<body class="mdui-appbar-with-toolbar">
    {{-- 顶部应用栏：汉堡菜单 + 标题 --}}
    <div class="mdui-appbar mdui-appbar-fixed">
        <div class="mdui-toolbar mdui-color-theme">
            <button class="mdui-btn mdui-btn-icon" id="drawerToggle">
                <i class="mdui-icon material-icons">menu</i>
            </button>
            <a href="/" class="mdui-typo-title">{{ $appName ?? 'jaravel' }}</a>
            <div class="mdui-toolbar-spacer"></div>
            <a href="https://github.com/lijialong1313/jaravel" class="mdui-btn mdui-btn-icon" target="_blank">
                <i class="mdui-icon material-icons">code</i>
            </a>
        </div>
    </div>

    {{-- 抽屉导航栏（由子模板通过 @section('drawer') 提供） --}}
    @yield('drawer')

    {{-- 页面主体 --}}
    <div class="mdui-container">
        @yield('content')
    </div>

    <footer>
        jaravel v0.1.2 &mdash; Powered by jblade + mdui Material Design 1
    </footer>

    {{-- mdui 1.x JavaScript（本地资源） --}}
    <script src="@asset('js/mdui.min.js')"></script>
    {{-- CodeMirror 5.x 代码编辑器（本地资源） --}}
    <link rel="stylesheet" href="@asset('codemirror/codemirror.min.css')">
    <link rel="stylesheet" href="@asset('codemirror/theme-material.min.css')">
    <script src="@asset('codemirror/codemirror.min.js')"></script>
    <script src="@asset('codemirror/mode-clike.min.js')"></script>
    <script>
        // 全局抽屉控制工具（用于 showLogin/showMain 程序化控制 + 汉堡菜单点击切换）
        var jaravelDrawer = {
            _inst: null,
            _getInst: function() {
                if (!this._inst) {
                    var el = document.getElementById('mainDrawer');
                    if (!el) return null;
                    this._inst = new mdui.Drawer(el);
                }
                return this._inst;
            },
            open: function() {
                var inst = this._getInst();
                if (inst) inst.open();
            },
            close: function() {
                var inst = this._getInst();
                if (inst) inst.close();
            },
            toggle: function() {
                var inst = this._getInst();
                if (inst) inst.toggle();
            }
        };
        // 页面加载完成后绑定事件
        document.addEventListener('DOMContentLoaded', function() {
            // 汉堡菜单点击切换抽屉
            var toggleBtn = document.getElementById('drawerToggle');
            if (toggleBtn) {
                toggleBtn.addEventListener('click', function() {
                    jaravelDrawer.toggle();
                });
            }
            // 监听抽屉开关事件，动态调整 body padding
            var drawerEl = document.getElementById('mainDrawer');
            if (drawerEl) {
                drawerEl.addEventListener('opened.mdui.drawer', function() {
                    document.body.classList.add('mdui-drawer-body-left');
                });
                drawerEl.addEventListener('closed.mdui.drawer', function() {
                    document.body.classList.remove('mdui-drawer-body-left');
                });
            }
        });
    </script>

    @yield('scripts')
</body>
</html>
