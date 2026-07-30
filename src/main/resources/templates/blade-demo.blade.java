@extends('demo.two-col')

@section('title', 'jblade 功能演示 - ' . $appName)

@section('nav')
    @parent
    | <span>三层继承导航（base -&gt; two-col -&gt; blade-demo）</span>
@endsection

@section('sidebar')
    <ul>
    @foreach($items as $item)
        <li>{{ $loop->iteration }}. {{ $item }}@if($loop->last)（最后一项）@endif</li>
    @endforeach
    </ul>
@endsection

@section('main')
    <h1>成绩等级：@if($score >= 90)优秀@elseif($score >= 60)及格@else()不及格@endif（{{ $score }} 分）</h1>
    <p>HTML 转义输出：{{ $rawHtml }}</p>
    <p>原样输出：{!! $rawHtml !!}</p>
    <p>路由别名 wire.demo 解析为：<code>@route('wire.demo')</code></p>
@endsection
