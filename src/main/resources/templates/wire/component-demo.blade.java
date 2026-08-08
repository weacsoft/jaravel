@extends('wire-layout')
@section('title', '组件演示')

@section('sidebar')
<a href="/wire" wire-navigate class="@if($currentPage === 'dashboard') active @endif">仪表盘</a>
<a href="/wire/spa"   wire-navigate class="@if($currentPage === 'spa') active @endif">SPA 导航</a>
<a href="/wire/tasks" wire-navigate class="@if($currentPage === 'tasks') active @endif">CRUD 列表</a>
<a href="/wire/components" wire-navigate class="@if($currentPage === 'components') active @endif">组件</a>
<a href="/wire/anchors" wire-navigate class="@if($currentPage === 'anchors') active @endif">锚点改写</a>
@endsection

@section('scripts')
<script wire:config data-wire-update="/api/wire/components"></script>
@endsection

@section('content')
<div class="card">
    <h3 style="margin-bottom:16px;font-size:16px;font-weight:700;">Wire 命名组件演示</h3>
    <p class="hint">点击按钮，控制器在 JSON 响应中下发组件，前端 wire-component.js 自动无感挂载。</p>
</div>

<div class="card">
    <h3 style="margin-bottom:12px;font-size:14px;font-weight:600;">Toast 消息</h3>
    <div style="display:flex;gap:8px;flex-wrap:wrap;">
        <button class="btn btn-outline" wire:click="toast_info">信息提示</button>
        <button class="btn btn-primary"  wire:click="toast_success">成功提示</button>
        <button class="btn btn-outline" style="border-color:#f39c12;color:#f39c12;" wire:click="toast_warning">警告提示</button>
        <button class="btn btn-danger"  wire:click="toast_error">错误提示</button>
    </div>
</div>

<div class="card">
    <h3 style="margin-bottom:12px;font-size:14px;font-weight:600;">Confirm 确认框</h3>
    <div style="display:flex;gap:8px;flex-wrap:wrap;">
        <button class="btn btn-danger" wire:click="confirm_show">弹出确认框</button>
        <button class="btn btn-outline" wire:click="confirm_delete">模拟已确认删除</button>
    </div>
</div>

<div class="card">
    <h3 style="margin-bottom:12px;font-size:14px;font-weight:600;">工作原理</h3>
    <table>
        <tr><th>步骤</th><th>说明</th></tr>
        <tr>
            <td>1</td>
            <td>按钮点击 → wire:click 属性触发 sendRequest()，发送 POST 到 /api/wire/components</td>
        </tr>
        <tr>
            <td>2</td>
            <td>控制器调用 <code>WireResponse.of().withComponent("toast", ...).build()</code>，将组件序列化为 JSON 下发</td>
        </tr>
        <tr>
            <td>3</td>
            <td>前端 wire-lib.js 收到 JSON 后，读取 <code>effects.components</code>，调用 <code>WireComponent.mountAll()</code> 挂载</td>
        </tr>
        <tr>
            <td>4</td>
            <td>wire-component.js 解析组件 HTML + 生命周期脚本，插入 <code>[wire:outlet]</code> 容器，执行 onCreate → onStart</td>
        </tr>
    </table>
</div>
@endsection
