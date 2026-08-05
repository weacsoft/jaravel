{{--
    /home 页面模板。独立文件、独立控制器、独立路由。
    与 /list 共用 pjax.layout，但不共享任何 PJAX 相关代码或标记。
--}}
@extends('pjax.layout')

@section('title', '概览 · jaravel PJAX')

@section('sidebar')
@include('pjax.nav', ['active' => 'home'])
@endsection

@section('content')
<div class="panel">
    <h2 style="margin-top:0;">概览</h2>
    <p class="muted">当前控制器：<code>PjaxHomeController</code>　模板：<code>templates/pjax/home.blade.java</code>　路由：<code>GET /home</code></p>
    <p>{{ $intro }}</p>
</div>

<div class="panel">
    <div class="muted" style="margin-bottom:8px;">任务统计</div>
    <table class="grid">
        <tr><th style="width:40%;">总任务数</th><td>{{ $total }}</td></tr>
        <tr><th>已完成</th><td>{{ $done }}</td></tr>
        <tr><th>进行中</th><td>{{ $pending }}</td></tr>
        <tr><th>服务端渲染时刻</th><td class="kv">{{ $renderedAt }}</td></tr>
    </table>
    <p style="margin-top:16px;"><a href="/list" class="mdui-btn mdui-color-indigo mdui-ripple">前往任务列表 →</a></p>
</div>
@endsection

@section('scratch')
@include('pjax.scratch')
@endsection

@section('scripts')
@include('pjax.scripts')
@endsection
