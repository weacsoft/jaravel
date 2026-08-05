{{-- 命名组件：confirm（确认框）。无布局、无 wire:section，纯片段模板。 --}}
{{-- 可用变量：$wireId、$wireName，以及下发参数 $title/$message。 --}}
<div class="wc-confirm-mask" id="{{ $wireId }}">
    <div class="wc-confirm" role="dialog" aria-modal="true">
        <div class="wc-confirm__title">{{ $title }}</div>
        <div class="wc-confirm__body">{{ $message }}</div>
        <div class="wc-confirm__actions">
            <button class="wc-confirm__ok mdui-btn mdui-btn-raised mdui-color-theme" type="button">确定</button>
            <button class="wc-confirm__cancel mdui-btn mdui-btn-raised" type="button">取消</button>
        </div>
    </div>
</div>
<script wire:lifecycle>
    function onStart(el, wire) {
        console.log('[confirm ' + wire.id + '] onStart');
        el.__onResult = function (ok) {
            // 把结果派发到 document，调用方（如演示页）可监听
            document.dispatchEvent(new CustomEvent('wc:confirm', { detail: { id: wire.id, ok: ok } }));
            wire.stop();
        };
        el.querySelector('.wc-confirm__ok').addEventListener('click', function () { el.__onResult(true); });
        el.querySelector('.wc-confirm__cancel').addEventListener('click', function () { el.__onResult(false); });
    }
    function onStop(el, wire) {
        console.log('[confirm ' + wire.id + '] onStop（开始移除）');
        el.style.opacity = '0';
        return 200;
    }
    function onDestroy(el, wire) {
        console.log('[confirm ' + wire.id + '] onDestroy（DOM 已移除）');
        el.__onResult = null;
    }
</script>
