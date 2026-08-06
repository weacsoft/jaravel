{{--
    区域脚本运行时探针（Region Script Runtime Probe）。

    本片段被放进 content 区域——该区域每次切换都会被替换，因此这里的
    「进入即触发」脚本每次切换都应当重新执行一次，且：
      · 不能因为片段插入 + 重放而执行两次；
      · 上一份脚本注册在 document/window 上的监听器、setInterval 必须被回收；
      · document-ready 类回调要在每次切换后各回放一次；
      · <script src> 只加载一次；data-pjax-run="once" 整个会话只跑一次；
      · 顶层 function / var 仍要能被 onclick="fn()" 这类内联属性看到。

    参数：$page —— 页面标识（home / list），用于分页面计数。
--}}
<div class="panel" id="rt-probe" data-page="{{ $page }}">
    <div class="muted" style="margin-bottom:8px;">区域脚本运行时探针 · 当前页 <code>{{ $page }}</code></div>
    <table class="grid">
        <tr><th style="width:46%;">本页内联脚本执行次数</th><td class="kv" id="rt-exec">-</td></tr>
        <tr><th>document-ready 回放次数（累计）</th><td class="kv" id="rt-ready">-</td></tr>
        <tr><th>once 脚本执行次数（累计）</th><td class="kv" id="rt-once">-</td></tr>
        <tr><th>外部脚本执行次数（累计）</th><td class="kv" id="rt-ext">-</td></tr>
        <tr><th>document click 监听命中数</th><td class="kv" id="rt-clicks">-</td></tr>
        <tr><th>本页 setInterval 心跳</th><td class="kv" id="rt-tick">-</td></tr>
    </table>
    <p style="margin-top:12px;">
        <button type="button" id="rt-hello-btn" class="mdui-btn mdui-color-indigo mdui-ripple" onclick="rtHello()">调用顶层函数 rtHello()</button>
        <span class="kv" id="rt-hello">未调用</span>
    </p>
</div>

{{-- 外部脚本：两个页面引用同一个文件，用于验证跨切换不重复加载 --}}
<script src="@asset('js/pjax-probe-ext.js')"></script>

{{-- once 脚本：首屏原生执行一次后，之后任何切换都不应再执行 --}}
<script data-pjax-run="once" id="rt-once-script">
window.__rt = window.__rt || {};
window.__rt.once = (window.__rt.once || 0) + 1;
</script>

{{-- 顶层声明：被包进函数作用域后需回填到 window，否则 onclick="rtHello()" 会报 ReferenceError --}}
<script>
var rtVarTag = 'top-level-var';
function rtHello() {
    var rt = window.__rt || {};
    rt.helloCalls = (rt.helloCalls || 0) + 1;
    var el = document.getElementById('rt-hello');
    if (el) { el.textContent = 'rtHello() 第 ' + rt.helloCalls + ' 次调用 · ' + rtVarTag; }
}
</script>

{{-- 主探针：进入即触发，每次区域替换都应重新执行恰好一次 --}}
<script>
(function () {
    var page = '{{ $page }}';
    var rt = window.__rt = window.__rt || {};
    rt.exec = rt.exec || {};
    rt.tick = rt.tick || {};
    rt.ready = rt.ready || 0;
    rt.docClicks = rt.docClicks || 0;
    rt.readyLog = rt.readyLog || [];

    rt.exec[page] = (rt.exec[page] || 0) + 1;
    rt.tick[page] = rt.tick[page] || 0;

    function render() {
        function put(id, val) {
            var el = document.getElementById(id);
            if (el) { el.textContent = String(val); }
        }
        put('rt-exec', rt.exec[page]);
        put('rt-ready', rt.ready);
        put('rt-once', rt.once || 0);
        put('rt-ext', rt.ext || 0);
        put('rt-clicks', rt.docClicks);
        put('rt-tick', rt.tick[page]);
    }

    // 进入即触发的「文档就绪」回调：首屏由原生 DOMContentLoaded 触发，
    // 切换后由 pjax 运行时在本次替换结束时回放一次。
    document.addEventListener('DOMContentLoaded', function () {
        rt.ready++;
        rt.readyLog.push(page);
        render();
    });

    // 注册在 document 上的委托监听：若旧区域的监听器未被回收，
    // 一次点击会被计数多次（每来回切换一次就多一份）。
    document.addEventListener('click', function () {
        rt.docClicks++;
        render();
    });

    // 心跳定时器：区域被替换后，旧脚本的 interval 必须停止。
    setInterval(function () {
        rt.tick[page] = (rt.tick[page] || 0) + 1;
        render();
    }, 100);

    render();
})();
</script>
