{{-- 侧边导航（共享片段）。$active 决定高亮项，因此两个页面的该区域指纹不同 -> 会被替换。 --}}
<div class="muted" style="padding:0 14px 8px;">导航</div>
<a class="nav-item @if($active === 'home') active @endif" href="/home">概览</a>
<a class="nav-item @if($active === 'list') active @endif" href="/list">任务列表</a>
<div class="muted" style="padding:12px 14px 0; line-height:1.6;">
    点击链接不会整页刷新：<br>
    仅变化区域被替换。
</div>
