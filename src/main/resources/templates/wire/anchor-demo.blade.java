@extends('wire-layout')

{{-- 以下三个 section 分别落在「HTML 注释非法/失效」的三种位置：
     title 文本节点、meta 的 content 属性、body 的 class 属性。
     若锚点改写未生效，这些位置会出现 <!--wire:section-start:xxx--> 明文。 --}}
@section('title', '锚点改写演示 — Wire')
@section('bodyClass', 'page-anchors theme-light')
@section('metaDescription', '验证 title / class / meta 等注释非法位置的 wire section 替换')

@section('sidebar')
<a href="/wire" wire-navigate class="@if($currentPage === 'dashboard') active @endif">仪表盘</a>
<a href="/wire/spa"   wire-navigate class="@if($currentPage === 'spa') active @endif">SPA 导航</a>
<a href="/wire/tasks" wire-navigate class="@if($currentPage === 'tasks') active @endif">CRUD 列表</a>
<a href="/wire/components" wire-navigate class="@if($currentPage === 'components') active @endif">组件</a>
<a href="/wire/anchors" wire-navigate class="@if($currentPage === 'anchors') active @endif">锚点改写</a>
@endsection

@section('scripts')
<script wire:config data-wire-update="/api/wire/anchors"></script>
@endsection

@section('content')
<div class="card">
    <h3 style="margin-bottom:12px;font-size:16px;font-weight:700;">⚓ 锚点改写演示</h3>
    <p class="hint">
        wire 的局部刷新靠 <code>&lt;!--wire:section-start:NAME--&gt;</code> 注释锚点定位片段。
        但 <code>&lt;title&gt;</code>、<code>&lt;textarea&gt;</code>、<code>&lt;script&gt;</code>、<code>&lt;style&gt;</code>
        内部的注释不会被浏览器解析，标签属性值里的注释更会直接变成脏字符串。
        框架在渲染出口把这些位置的锚点改写为 <code>wire:section-attr</code> /
        <code>wire:section-text</code> 标记属性，透明导航时由 <code>anchors</code> 字段精确回填。
    </p>
</div>

<div class="card">
    <h3 style="margin-bottom:12px;font-size:14px;font-weight:600;">本页三处受控位置</h3>
    <table>
        <tr><th>位置</th><th>section</th><th>当前值</th></tr>
        <tr><td>&lt;title&gt; 文本</td><td>title</td><td><code>锚点改写演示 — Wire</code></td></tr>
        <tr><td>&lt;body class&gt; 属性</td><td>bodyClass</td><td><code>page-anchors theme-light</code></td></tr>
        <tr><td>&lt;meta description&gt; 属性</td><td>metaDescription</td><td><code>验证注释非法位置的替换</code></td></tr>
    </table>
    <p class="hint" style="margin-top:12px;">
        点顶栏「仪表盘」用透明导航离开本页，标签页标题、body class、meta description 会同步变成仪表盘的值——
        且全程不出现任何 <code>wire:section</code> 明文。
    </p>
</div>

<div class="card">
    <h3 style="margin-bottom:12px;font-size:14px;font-weight:600;">实时自检</h3>
    <div style="display:flex;gap:8px;flex-wrap:wrap;margin-bottom:12px;">
        <button class="btn btn-primary" wire:click="ping">发一次 wire 请求</button>
        <button class="btn btn-outline" id="anchor-selfcheck">检查当前页面是否被污染</button>
    </div>
    <pre id="anchor-report" style="background:#f8f9fb;border:1px solid #e8ecf1;border-radius:8px;padding:12px;font-size:12px;line-height:1.7;margin:0;white-space:pre-wrap;">点上面的按钮开始检查</pre>
</div>

<script>
(function () {
    var btn = document.getElementById('anchor-selfcheck');
    if (!btn) return;
    btn.addEventListener('click', function () {
        var lines = [];
        var title = document.title;
        var cls = document.body.getAttribute('class') || '';
        var metaEl = document.querySelector('meta[name="description"]');
        var desc = metaEl ? metaEl.getAttribute('content') : '(缺失)';
        var dirty = /wire:section/;
        lines.push((dirty.test(title) ? '✗' : '✓') + ' title       = ' + JSON.stringify(title));
        lines.push((dirty.test(cls) ? '✗' : '✓') + ' body.class  = ' + JSON.stringify(cls));
        lines.push((dirty.test(desc) ? '✗' : '✓') + ' meta.desc   = ' + JSON.stringify(desc));
        var ok = !dirty.test(title) && !dirty.test(cls) && !dirty.test(desc);
        lines.push('');
        lines.push(ok ? '结论：三处注释非法位置均干净，锚点改写生效。' : '结论：仍存在锚点污染，改写未生效。');
        document.getElementById('anchor-report').textContent = lines.join('\n');
    });
})();
</script>
@endsection
