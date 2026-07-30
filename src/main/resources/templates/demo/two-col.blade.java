@extends('demo.base')

@section('nav')
    @parent
    | <a href="@route('captcha.demo')">验证码演示</a>
@endsection

@section('content')
    <div class="layout">
        <aside>@yield('sidebar')</aside>
        <article>@yield('main')</article>
    </div>
@endsection
