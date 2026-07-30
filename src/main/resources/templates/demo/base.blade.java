<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="utf-8">
    <title>@yield('title', 'jaravel')</title>
    <style>
        body { font-family: sans-serif; margin: 2rem; }
        nav { padding: .5rem 0; border-bottom: 1px solid #ddd; }
        .layout { display: flex; gap: 2rem; margin-top: 1rem; }
        aside { min-width: 200px; background: #f6f6f6; padding: 1rem; }
    </style>
</head>
<body>
<nav>
@section('nav')
    <a href="@route('blade.demo')">Blade 演示</a>
@show
</nav>
<main>@yield('content')</main>
</body>
</html>
