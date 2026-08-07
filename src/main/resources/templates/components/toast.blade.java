{{-- 命名组件：toast（消息提示）。无布局、无 wire:section，纯片段模板。 --}}
{{-- 可用变量：$wireId（实例唯一 id）、$wireName（组件名）、以及下发参数 $type/$message/$ttl。 --}}
{{-- $type 取值：info(默认) / success / warning / error，空时自动 fallback 为 info --}}
@php
    $_t = $type ?? 'info';
    $_icon = $_t === 'info' ? 'ℹ' : ($_t === 'success' ? '✓' : ($_t === 'warning' ? '⚠' : ($_t === 'error' ? '✕' : 'ℹ')));
@endphp
<div class="wc-toast wc-toast--{{ $_t }}" id="{{ $wireId }}" role="alert">
    <span class="wc-toast__icon">{{ $_icon }}</span>
    <span class="wc-toast__msg">{{ $message }}</span>
    <button class="wc-toast__close" type="button" aria-label="关闭"
            onclick="var el=this.closest('.wc-toast'); if(el && el.__wcStop){el.__wcStop();}">×</button>
</div>
<script wire:lifecycle>
    function onCreate(el, wire) {
        console.log('[toast ' + wire.id + '] onCreate');
        // 多实例堆叠：按当前已有 toast 数量向下偏移，避免互相覆盖
        var siblings = document.querySelectorAll('.wc-toast').length;
        el.style.top = (16 + siblings * 64) + 'px';
        el.style.opacity = '0';
        el.style.transform = 'translateY(-8px)';
    }
    function onStart(el, wire) {
        console.log('[toast ' + wire.id + '] onStart');
        requestAnimationFrame(function () {
            el.style.transition = 'opacity .25s ease, transform .25s ease';
            el.style.opacity = '1';
            el.style.transform = 'translateY(0)';
        });
        var ttl = (wire.params && wire.params.ttl) || 3000;
        wire._timer = setTimeout(function () { wire.stop(); }, Number(ttl));
        // 暴露停止函数给关闭按钮
        el.__wcStop = function () { wire.stop(); };
    }
    function onStop(el, wire) {
        console.log('[toast ' + wire.id + '] onStop（开始移除）');
        el.style.opacity = '0';
        el.style.transform = 'translateY(-8px)';
        return 280; // 等待退场动画后再真正移除 DOM
    }
    function onDestroy(el, wire) {
        console.log('[toast ' + wire.id + '] onDestroy（DOM 已移除）');
        if (wire._timer) { clearTimeout(wire._timer); }
        el.__wcStop = null;
    }
</script>
