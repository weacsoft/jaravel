@extends('layout')

@section('title', 'Wire Demo - 部分更新演示')

@section('content')
<div class="mdui-container">
    <div class="page-content">
        <h2 class="section-title">Wire Demo - Laravel Livewire 风格部分更新</h2>
        <p class="hint">
            此页面使用 <code>Response.wire()</code> 渲染，update URL 为 <code>/api/wire/demo</code>。<br>
            所有交互通过 wire: 指令触发，仅更新 content section，不刷新整页。
        </p>

        <div class="mdui-row-md-2">
            {{-- 计数器演示 --}}
            <div class="mdui-col">
                <div class="mdui-card" style="margin-top: 16px;">
                    <div class="mdui-card-header">
                        <div class="mdui-card-header-title">计数器（wire:click）</div>
                    </div>
                    <div class="mdui-card-content">
                        <div style="display: flex; align-items: center; gap: 16px;">
                            <button wire:click="decrement" class="mdui-btn mdui-btn-icon mdui-color-red mdui-ripple">
                                <i class="mdui-icon material-icons">remove</i>
                            </button>
                            <span style="font-size: 32px; font-weight: bold; min-width: 60px; text-align: center;">{{ $count }}</span>
                            <button wire:click="increment" class="mdui-btn mdui-btn-icon mdui-color-theme mdui-ripple">
                                <i class="mdui-icon material-icons">add</i>
                            </button>
                            <span wire:loading wire:target="increment" class="mdui-spinner" style="display: none;"></span>
                            <span wire:loading wire:target="decrement" class="mdui-spinner" style="display: none;"></span>
                        </div>
                        <button wire:click="reset" class="mdui-btn mdui-btn-dense" style="margin-top: 12px;">
                            重置计数器
                        </button>
                    </div>
                </div>
            </div>

            {{-- 双向绑定演示 --}}
            <div class="mdui-col">
                <div class="mdui-card" style="margin-top: 16px;">
                    <div class="mdui-card-header">
                        <div class="mdui-card-header-title">实时同步（wire:model）</div>
                    </div>
                    <div class="mdui-card-content">
                        <div class="mdui-textfield">
                            <label class="mdui-textfield-label">输入消息（防抖 150ms 自动同步）</label>
                            <input class="mdui-textfield-input" type="text" wire:model="message" value="{{ $message }}" placeholder="输入文字试试..." />
                        </div>
                        <div class="mdui-textfield">
                            <label class="mdui-textfield-label">即时同步（wire:model.live）</label>
                            <input class="mdui-textfield-input" type="text" wire:model.live="message" value="{{ $message }}" placeholder="即时同步..." />
                        </div>
                        <p style="margin-top: 12px;">服务端回显: <strong>{{ $message }}</strong></p>
                    </div>
                </div>
            </div>
        </div>

        {{-- 列表演示 --}}
        <div class="mdui-card" style="margin-top: 16px;">
            <div class="mdui-card-header">
                <div class="mdui-card-header-title">列表管理（wire:click + @foreach）</div>
            </div>
            <div class="mdui-card-content">
                <div style="margin-bottom: 12px;">
                    <button wire:click="addItem" class="mdui-btn mdui-btn-raised mdui-color-theme mdui-ripple">
                        <i class="mdui-icon material-icons" style="margin-right: 4px;">add</i>添加项目
                    </button>
                    <button wire:click="removeItem" class="mdui-btn mdui-btn-raised mdui-color-red mdui-ripple">
                        <i class="mdui-icon material-icons" style="margin-right: 4px;">remove</i>删除最后
                    </button>
                </div>
                <table class="data-table">
                    <thead>
                        <tr><th>#</th><th>名称</th></tr>
                    </thead>
                    <tbody>
                        @foreach($items as $item)
                        <tr>
                            <td>{{ $item }}</td>
                            <td>{{ $item }}</td>
                        </tr>
                        @endforeach
                    </tbody>
                </table>
                <p class="hint">列表共 {{ $items->size() }} 项，每次点击按钮后服务端重新渲染此 section。</p>
            </div>
        </div>

        {{-- 自定义 URL 演示 --}}
        <div class="mdui-card" style="margin-top: 16px;">
            <div class="mdui-card-header">
                <div class="mdui-card-header-title">自定义 Update URL（wire:update）</div>
            </div>
            <div class="mdui-card-content">
                <p class="hint">
                    下方按钮使用 <code>wire:update="/api/wire/demo"</code> 显式指定 update URL。<br>
                    点击后计数器 +1，与上方计数器共享同一状态。
                </p>
                <button wire:click="increment" wire:update="/api/wire/demo" class="mdui-btn mdui-btn-dense mdui-color-theme">
                    通过 wire:update 指定 URL 增加 +1（当前计数: {{ $count }}）
                </button>
            </div>
        </div>
    </div>
</div>
@endsection
