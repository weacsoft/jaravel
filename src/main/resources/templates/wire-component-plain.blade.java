@extends('layout')

@section('title', 'Wire 命名组件 - 自定义加载位置')

@section('content')
<div class="mdui-container">
    <div class="page-content">
        <h2 class="section-title">自定义加载位置（outlet）</h2>
        <p class="hint">
            默认情况下 <code>WireOutlet</code> 中间件会把加载位置容器自动插到 <code>&lt;/body&gt;</code> 之前；
            你也可以用 <code>@{!! wire_outlet() !!}</code> 在任意位置手动指定，中间件检测到页面已有
            outlet 标记就不会重复注入。
        </p>

        <div class="custom-outlet-area" style="border:1px dashed #bdbdbd; padding:16px; border-radius:6px; margin-top:16px;">
            <p class="hint" style="margin-top:0;">下面这个虚线框里就是本页用 <code>@{!! wire_outlet() !!}</code> 放置的加载点：</p>
            {!! wire_outlet() !!}
        </div>

        <p class="hint" style="margin-top:16px;">
            首屏已通过 <code>WireComponents.push("toast", ...)</code> 下发一条 toast，
            它会被挂载到上面的自定义位置（而非默认 body 末尾）。
        </p>
        <p><a href="/wire-component-demo" class="mdui-btn mdui-btn-raised mdui-ripple">← 返回主演示</a></p>
    </div>
</div>
@endsection
