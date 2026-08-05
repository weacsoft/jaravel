{{--
    状态保持探针（共享片段）。

    两个页面渲染出的 HTML 完全一致 -> 服务端算出的区域指纹相同
    -> 判定为 unchanged -> 不下发内容，前端也不触碰这段 DOM。

    因此：输入框里的文字、光标位置、JS 计时器绑定的 DOM 节点全部原样保留，
    这正是「无感切换」与 location.href 整页跳转的本质区别。
--}}
<div class="muted" style="margin-bottom:8px;">状态保持探针（该区域两页一致，切换时不会被替换）</div>
<div style="display:flex; gap:16px; flex-wrap:wrap; align-items:center;">
    <div style="flex:1; min-width:240px;">
        <input id="probe-input" type="text" placeholder="在这里输入任意文字，然后切换页面——内容不会丢失">
    </div>
    <div class="kv">存活计时：<b id="probe-uptime">0</b> s</div>
    <div class="kv">PJAX 切换次数：<b id="probe-visits">0</b></div>
</div>
<div class="kv muted" style="margin-top:10px;">最近一次切换：<span id="probe-log">（尚未切换）</span></div>
