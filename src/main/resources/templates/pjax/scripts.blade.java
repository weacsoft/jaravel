{{--
    共享脚本片段。两个页面输出一致 -> scripts 区域指纹相同 -> 切换时不会被替换，
    因此下面的 setInterval / 事件监听只会注册一次，不会随切换重复叠加。
--}}
<script>
(function () {
    if (window.__pjaxDemoBooted) { return; }
    window.__pjaxDemoBooted = true;

    var seconds = 0;
    setInterval(function () {
        seconds++;
        var el = document.getElementById('probe-uptime');
        if (el) { el.textContent = seconds; }
    }, 1000);

    var visits = 0;
    document.addEventListener('pjax:loaded', function (e) {
        visits++;
        var v = document.getElementById('probe-visits');
        if (v) { v.textContent = visits; }
        var log = document.getElementById('probe-log');
        if (log) {
            var d = e.detail || {};
            log.textContent = (d.url || '') +
                '　已替换: [' + (d.changed || []).join(', ') + ']' +
                '　未变化: [' + (d.unchanged || []).join(', ') + ']';
        }
    });
})();
</script>
