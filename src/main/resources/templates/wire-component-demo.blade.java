@extends('layout')

@section('title', 'Wire 命名组件演示')

@section('content')
<div class="mdui-container">
    <div class="page-content">
        <h2 class="section-title">Wire 命名组件（toast / confirm）</h2>
        <p class="hint">
            后端通过 <code>WireService.responseComponent(name, params)</code> 把组件随 Wire 更新响应下发，
            或首屏通过 <code>WireComponents.push(name, params)</code> 下发；前端 <code>wire-component.js</code>
            无感挂载。每个实例拥有独立生命周期 <code>onCreate / onStart / onStop / onDestroy</code>，
            模板内调用 <code>wire.stop()</code> 即表示“展示完成，移除我”。
        </p>
        <div style="display:flex; gap:12px; flex-wrap:wrap; margin-top:16px;">
            <button wire:click="toast" class="mdui-btn mdui-btn-raised mdui-color-theme mdui-ripple">弹出 toast</button>
            <button wire:click="confirm" class="mdui-btn mdui-btn-raised mdui-color-theme mdui-ripple">弹出 confirm</button>
            <button wire:click="multi" class="mdui-btn mdui-btn-raised mdui-color-theme mdui-ripple">连续弹出 3 条（验证隔离）</button>
            <a href="/wire-component-plain" class="mdui-btn mdui-btn-raised mdui-ripple">自定义加载位置演示 →</a>
        </div>
        <p class="hint" style="margin-top:16px;">
            打开浏览器控制台可见每条组件的 <code>onCreate → onStart →（stop）→ onStop → onDestroy</code> 生命周期日志；
            “连续弹出 3 条”会同时挂载三个互不干扰的 toast 实例。
        </p>
    </div>
</div>
@endsection
