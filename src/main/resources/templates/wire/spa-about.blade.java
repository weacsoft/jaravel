@extends('wire-layout')

@section('title', 'Wire 演示 — 关于')

@section('sidebar')
    <a href="/wire" wire-navigate>📊 仪表盘</a>
    <a href="/wire/records" wire-navigate>📋 记录列表</a>
    <a href="/wire/spa" wire-navigate class="active">🧭 SPA 导航</a>
    <a href="/wire/tasks" wire-navigate>📝 CRUD 列表</a>
    <a href="/wire/components" wire-navigate>🧩 命名组件</a>
@endsection

@section('content')
    <h2 style="margin-bottom:16px;">ℹ️ 关于 Wire 透明导航</h2>

    <div class="card">
        <h3 style="margin-bottom:12px;">🔧 核心技术</h3>
        <table>
            <tr><th>技术</th><th>说明</th></tr>
            <tr><td><code>X-Wire-Navigate</code></td><td>请求头标识这是一个导航请求</tr>
            <tr><td><code>X-Wire-Hashes</code></td><td>前端发送当前各 section 的 hash，后端 diff 计算</tr>
            <tr><td><code>&lt;!--wire:section-start:NAME--&gt;</code></td><td>响应中标记 section 边界</tr>
            <tr><td><code>@section / @yield</code></td><td>Blade 原生 section 机制，用于 diff 对比</tr>
        </table>
    </div>

    <div class="card">
        <h3 style="margin-bottom:12px;">📐 本演示展示的架构</h3>
        <pre style="background:#f8f9fb; padding:16px; border-radius:8px; font-size:13px; line-height:1.8; overflow-x:auto;">
controllers/
  wire/
    WireShowcaseController  ← 仪表盘 + 记录列表（两个方法，同一 layout）
    WireSpaController       ← SPA 导航（三个页面，共享 layout）
    WireListController      ← CRUD 列表（真实数据库）
    WireComponentController ← 命名组件（toast/confirm）

templates/wire/
  wire-layout.blade.java    ← 所有页面的共享布局
  index.blade.java          ← 仪表盘页
  records.blade.java        ← 记录列表页
  spa-overview.blade.java   ← SPA 概览页
  spa-task-list.blade.java  ← SPA 任务列表页
  spa-about.blade.java      ← SPA 关于页
  task-list.blade.java      ← CRUD 任务列表页
  component-demo.blade.java ← 组件演示页
        </pre>
    </div>

    <div class="card">
        <h3 style="margin-bottom:12px;">🎯 关键特性</h3>
        <ul style="line-height:2; padding-left:20px;">
            <li><b>透明导航</b>：Controller 无需任何 Wire 代码，正常写路由即可</li>
            <li><b>多 Controller 协作</b>：同一布局下，不同 Controller 处理方法之间的切换</li>
            <li><b>多 Blade 模板继承</b>：共享 wire-layout，各页面只定义自己的 section</li>
            <li><b>精准 diff</b>：只有变化的 section 被传输，未变化的部分（顶栏、CSS）不重复传输</li>
            <li><b>状态保留</b>：通过 data-wire-key 标记 DOM，刷新后输入框状态不丢失</li>
        </ul>
    </div>
@endsection
