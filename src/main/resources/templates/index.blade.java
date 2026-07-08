@extends('layout')

{{-- 设置浏览器标签页标题 --}}
@section('title', $title)

{{-- 页面专属样式 --}}
@section('head')
<style>
    {{-- Hero 区域 --}}
    .hero {
        text-align: center;
        padding: 72px 24px 56px;
        border-radius: 24px;
        margin-bottom: 32px;
        background: linear-gradient(135deg, var(--mdui-color-primary-container), var(--mdui-color-surface));
    }
    .hero-title {
        font-size: 48px;
        font-weight: 700;
        margin: 0 0 16px;
        color: var(--mdui-color-on-primary-container);
        letter-spacing: 1px;
    }
    .hero-desc {
        font-size: 18px;
        line-height: 1.7;
        color: var(--mdui-color-on-surface-variant);
        margin: 0 auto 32px;
        max-width: 640px;
    }
    .hero-actions {
        display: flex;
        gap: 16px;
        justify-content: center;
        flex-wrap: wrap;
    }
    {{-- 统计卡片 --}}
    .stat-card {
        padding: 16px 18px;
        border-radius: 16px;
        background: var(--mdui-color-surface-variant);
    }
    .stat-label {
        font-size: 13px;
        color: var(--mdui-color-on-surface-variant);
        margin-bottom: 8px;
    }
    .stat-value {
        font-size: 22px;
        font-weight: 600;
        color: var(--mdui-color-on-surface);
    }
    .stat-value.success { color: var(--mdui-color-primary); }
    .stat-value.error { color: var(--mdui-color-error); }
    {{-- 功能卡片 --}}
    .feature-card {
        padding: 24px;
        height: 100%;
        box-sizing: border-box;
    }
    .feature-icon { font-size: 40px; margin-bottom: 12px; }
    .feature-title {
        font-size: 18px;
        font-weight: 600;
        margin-bottom: 8px;
        color: var(--mdui-color-on-surface);
    }
    .feature-desc {
        font-size: 14px;
        line-height: 1.6;
        color: var(--mdui-color-on-surface-variant);
    }
    {{-- 底部说明 --}}
    .bottom-note {
        text-align: center;
        margin-top: 48px;
        padding: 24px;
        color: var(--mdui-color-on-surface-variant);
        font-size: 13px;
    }
</style>
@endsection

{{-- 页面主体内容 --}}
@section('content')
{{-- Hero 区域：应用名称、描述与入口按钮 --}}
<section class="hero">
    <h1 class="hero-title">{{ $appName }}</h1>
    <p class="hero-desc">{{ $description }}</p>
    <div class="hero-actions">
        <mdui-button variant="filled" href="/admin">进入管理后台</mdui-button>
        <mdui-button variant="outlined" href="/user">用户中心</mdui-button>
    </div>
</section>

{{-- 插件系统总览：通过 JS 动态加载 /api/plugin/overview 数据 --}}
<h2 class="section-title">插件系统总览</h2>
<div class="stat-grid" id="pluginOverview">
    <div class="stat-card">
        <div class="stat-label">Java 编译器状态</div>
        <div class="stat-value" id="statJavaCompilerStatus">加载中...</div>
    </div>
    <div class="stat-card">
        <div class="stat-label">Java 版本</div>
        <div class="stat-value" id="statJavaVersion">加载中...</div>
    </div>
    <div class="stat-card">
        <div class="stat-label">Jar 插件数</div>
        <div class="stat-value" id="statJarPluginCount">加载中...</div>
    </div>
    <div class="stat-card">
        <div class="stat-label">Jar 启用数</div>
        <div class="stat-value" id="statJarEnabledCount">加载中...</div>
    </div>
    <div class="stat-card">
        <div class="stat-label">Java 插件系统状态</div>
        <div class="stat-value" id="statJavaPluginSystemStatus">加载中...</div>
    </div>
    <div class="stat-card">
        <div class="stat-label">Java 插件数</div>
        <div class="stat-value" id="statJavaPluginCount">加载中...</div>
    </div>
</div>

{{-- 核心功能卡片网格 --}}
<h2 class="section-title">核心功能</h2>
<div class="card-grid">
    {{-- Java 在线编译 --}}
    <mdui-card class="feature-card" clickable>
        <div class="feature-icon">☕</div>
        <div class="feature-title">Java 在线编译</div>
        <div class="feature-desc">在浏览器中直接编写、编译并运行 Java 代码，即时查看执行结果，无需配置本地环境。</div>
    </mdui-card>
    {{-- Jar 插件热加载 --}}
    <mdui-card class="feature-card" clickable>
        <div class="feature-icon">📦</div>
        <div class="feature-title">Jar 插件热加载</div>
        <div class="feature-desc">动态加载与卸载 Jar 插件，无需重启服务即可完成热部署，插件管理灵活高效。</div>
    </mdui-card>
    {{-- 多租户隔离 --}}
    <mdui-card class="feature-card" clickable>
        <div class="feature-icon">🛡️</div>
        <div class="feature-title">多租户隔离</div>
        <div class="feature-desc">多租户资源与数据隔离，保障各租户服务独立性与数据安全，互不干扰。</div>
    </mdui-card>
    {{-- P2SP 远程执行 --}}
    <mdui-card class="feature-card" clickable>
        <div class="feature-icon">🌐</div>
        <div class="feature-title">P2SP 远程执行</div>
        <div class="feature-desc">点对服务器点（P2SP）远程执行能力，跨节点分发与调度任务，扩展执行边界。</div>
    </mdui-card>
    {{-- 双 Guard 认证 --}}
    <mdui-card class="feature-card" clickable>
        <div class="feature-icon">🔐</div>
        <div class="feature-title">双 Guard 认证</div>
        <div class="feature-desc">Admin 与 User 双重认证守卫，精细化区分权限边界，保障访问安全可控。</div>
    </mdui-card>
    {{-- 热重载 --}}
    <mdui-card class="feature-card" clickable>
        <div class="feature-icon">🔄</div>
        <div class="feature-title">热重载</div>
        <div class="feature-desc">模板与代码热重载机制，修改即时生效，开发与调试效率倍增。</div>
    </mdui-card>
</div>

{{-- 底部说明 --}}
<div class="bottom-note">
    Powered by jblade + mdui
</div>
@endsection

{{-- 页面专属脚本：加载插件系统概览数据 --}}
@section('scripts')
<script>
    // 加载插件系统概览数据并渲染到统计卡片
    (function () {
        // 设置指定统计项的文本
        function setText(id, text) {
            var el = document.getElementById(id);
            if (el) el.textContent = text;
        }
        // 渲染布尔状态：已启用 / 未启用
        function renderStatus(id, enabled) {
            var el = document.getElementById(id);
            if (!el) return;
            el.textContent = enabled ? '已启用' : '未启用';
            el.className = 'stat-value ' + (enabled ? 'success' : 'error');
        }

        fetch('/api/plugin/overview')
            .then(function (res) { return res.json(); })
            .then(function (data) {
                // Java 编译器状态
                renderStatus('statJavaCompilerStatus', data.javaCompilerEnabled || data.compilerAvailable);
                // Java 版本
                setText('statJavaVersion', data.javaVersion || data.javaRuntimeVersion || '未知');
                // Jar 插件数
                setText('statJarPluginCount',
                    data.jarPluginCount != null ? data.jarPluginCount
                        : (data.jarTotal != null ? data.jarTotal : '-'));
                // Jar 启用数
                setText('statJarEnabledCount',
                    data.jarEnabledCount != null ? data.jarEnabledCount
                        : (data.jarEnabled != null ? data.jarEnabled : '-'));
                // Java 插件系统状态
                renderStatus('statJavaPluginSystemStatus', data.javaPluginSystemEnabled || data.javaPluginEnabled);
                // Java 插件数
                setText('statJavaPluginCount',
                    data.javaPluginCount != null ? data.javaPluginCount
                        : (data.javaPlugins != null ? data.javaPlugins : '-'));
            })
            .catch(function (err) {
                // 加载失败时的兜底提示
                ['statJavaCompilerStatus', 'statJavaVersion', 'statJarPluginCount',
                 'statJarEnabledCount', 'statJavaPluginSystemStatus', 'statJavaPluginCount'
                ].forEach(function (id) { setText(id, '加载失败'); });
                console.error('加载插件概览失败:', err);
            });
    })();
</script>
@endsection
