{{--
    /list 页面模板。独立文件、独立控制器、独立路由。
    分页链接是普通 <a href="/list?page=N">，控制器里也是普通的 request.query() 读取，
    没有为 PJAX 做任何特殊改造 —— 切换与分页均由框架自动接管。
--}}
@extends('pjax.layout')

@section('title', '任务列表 · 第 ' . $pageNum . ' 页 · jaravel PJAX')

@section('sidebar')
@include('pjax.nav', ['active' => 'list'])
@endsection

@section('content')
<div class="panel">
    <h2 style="margin-top:0;">任务列表</h2>
    <p class="muted">当前控制器：<code>PjaxListController</code>　模板：<code>templates/pjax/list.blade.java</code>　路由：<code>GET /list</code></p>
    <p class="muted">共 {{ $total }} 条，每页 {{ $perPage }} 条，当前第 {{ $pageNum }} / {{ $lastPage }} 页。</p>

    <table class="grid">
        <thead>
            <tr><th style="width:70px;">ID</th><th>名称</th><th style="width:110px;">状态</th></tr>
        </thead>
        <tbody>
        @foreach($items as $item)
            <tr>
                <td>{{ $item['id'] }}</td>
                <td>{{ $item['name'] }}</td>
                <td>
                    @if($item['done'])
                        <span class="tag done">已完成</span>
                    @else
                        <span class="tag">进行中</span>
                    @endif
                </td>
            </tr>
        @endforeach
        </tbody>
    </table>

    <div class="pager" style="margin-top:16px;">
        @for($p = 1; $p <= $lastPage; $p++)
            @if($p == $pageNum)
                <span class="cur">{{ $p }}</span>
            @else
                <a href="/list?page={{ $p }}">{{ $p }}</a>
            @endif
        @endfor
    </div>
    <p class="muted" style="margin-top:12px;">服务端渲染时刻：<span class="kv">{{ $renderedAt }}</span></p>
</div>

@include('pjax.rt-probe', ['page' => 'list'])
@endsection

@section('scratch')
@include('pjax.scratch')
@endsection

@section('scripts')
@include('pjax.scripts')
@endsection
