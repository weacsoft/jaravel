@extends('wire-layout')

@section('title', 'Wire 演示 — SPA 导航')

@section('sidebar')
    <a href="/wire" wire-navigate>📊 仪表盘</a>
    <a href="/wire/records" wire-navigate>📋 记录列表</a>
    <a href="/wire/spa" wire-navigate class="active">🧭 SPA 导航</a>
    <a href="/wire/tasks" wire-navigate>📝 CRUD 列表</a>
    <a href="/wire/components" wire-navigate>🧩 命名组件</a>
@endsection

@section('content')
    <h2 style="margin-bottom:16px;">🧭 SPA 导航演示</h2>
    <p class="hint" style="margin-bottom:20px;">
        点击左侧菜单切换页面——<b>只有 sidebar 高亮和 content 内容变化</b>，
        顶栏始终不变。三个页面由<b>同一个 Controller</b> 的不同方法处理，共享同一布局。
    </p>

    <div class="nav-grid">
        <a href="/wire/spa?action=overview" wire-navigate class="nav-card @if(isset($currentPage) && $currentPage==='overview') active-nav @endif">
            <div class="icon">📈</div>
            <div class="title">概览</div>
            <div class="desc">统计数据 + 快速入口</div>
        </a>
        <a href="/wire/spa?action=tasks" wire-navigate class="nav-card @if(isset($currentPage) && $currentPage==='tasks') active-nav @endif">
            <div class="icon">📝</div>
            <div class="title">任务列表</div>
            <div class="desc">增删改查 + 精准刷新</div>
        </a>
        <a href="/wire/spa?action=about" wire-navigate class="nav-card @if(isset($currentPage) && $currentPage==='about') active-nav @endif">
            <div class="icon">ℹ️</div>
            <div class="title">关于</div>
            <div class="desc">Wire 技术说明</div>
        </a>
    </div>
@endsection
