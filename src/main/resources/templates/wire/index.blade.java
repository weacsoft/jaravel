@extends('wire-layout')

@section('title', 'Wire 演示 — 仪表盘')

@section('sidebar')
    <a href="/wire" wire-navigate class="active">📊 仪表盘</a>
    <a href="/wire/records" wire-navigate>📋 记录列表</a>
    <a href="/wire/spa" wire-navigate>🧭 SPA 导航</a>
    <a href="/wire/tasks" wire-navigate>📝 CRUD 列表</a>
    <a href="/wire/components" wire-navigate>🧩 命名组件</a>
@endsection

@section('content')
    <h2 style="margin-bottom:16px;">📊 Wire 透明导航演示</h2>
    <p class="hint" style="margin-bottom:20px;">
        本演示展示 Wire 的核心能力：<b>同一个 Blade 继承链下，多个 Controller 和多个 Blade 模板之间的无感切换</b>。
        点击顶栏或侧栏链接，页面<b>无整页刷新</b>，只有变化的部分被传输和替换。
    </p>

    <div class="stat-grid">
        <div class="stat-card">
            <div class="label">总记录数</div>
            <div class="value">{{ $recordCount ?? 0 }}</div>
        </div>
        <div class="stat-card">
            <div class="label">本月新增</div>
            <div class="value" style="color:#27ae60;">{{ $monthCount ?? 0 }}</div>
        </div>
        <div class="stat-card">
            <div class="label">待处理</div>
            <div class="value" style="color:#f39c12;">{{ $pendingCount ?? 0 }}</div>
        </div>
        <div class="stat-card">
            <div class="label">已完成</div>
            <div class="value" style="color:#888;">{{ $doneCount ?? 0 }}</div>
        </div>
    </div>

    <div class="card" style="margin-top:20px;">
        <h3 style="margin-bottom:12px;">🚀 快速导航</h3>
        <p class="hint">点击卡片跳转到其他演示页面，体验 Wire 透明导航：</p>
        <div class="nav-grid">
            <a href="/wire/records" wire-navigate class="nav-card">
                <div class="icon">📋</div>
                <div class="title">记录列表</div>
                <div class="desc">与仪表盘共享同一 layout，切换时只有 content 区域更新</div>
            </a>
            <a href="/wire/spa" wire-navigate class="nav-card">
                <div class="icon">🧭</div>
                <div class="title">SPA 导航</div>
                <div class="desc">左侧菜单切换，展示 sidebar diff 机制</div>
            </a>
            <a href="/wire/tasks" wire-navigate class="nav-card">
                <div class="icon">📝</div>
                <div class="title">CRUD 列表</div>
                <div class="desc">增删改后精准刷新列表，输入框状态保留</div>
            </a>
            <a href="/wire/components" wire-navigate class="nav-card">
                <div class="icon">🧩</div>
                <div class="title">命名组件</div>
                <div class="desc">Toast 消息和 Confirm 弹窗组件演示</div>
            </a>
        </div>
    </div>

    <div class="card">
        <h3 style="margin-bottom:12px;">💡 Wire 原理</h3>
        <ol style="line-height:2; padding-left:20px;">
            <li>链接使用 <code>wire-navigate</code> 属性标记</li>
            <li>点击时前端发送 <code>X-Wire-Navigate: true</code> + <code>X-Wire-Hashes</code> 请求头</li>
            <li>后端 Controller 照常使用 <code>ResponseBuilder.view()</code>，无需感知导航</li>
            <li>框架对比 section hash，<b>只返回变化的部分（diff）</b></li>
            <li>前端按 <code>&lt;!--wire:section-start:NAME--&gt;</code> 标记精确替换 DOM</li>
            <li>直接访问（无 Wire 头）→ 正常全页面渲染</li>
        </ol>
    </div>
@endsection
