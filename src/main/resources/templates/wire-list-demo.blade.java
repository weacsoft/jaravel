@extends('layout')

@section('title', 'Wire 列表演示 - CRUD + 分页 + 精准刷新')

@section('content')
<div class="mdui-container">
    <div class="page-content">
        <h2 class="section-title">{{ $title }}</h2>
        <p class="hint">
            本页演示：静态数组模拟数据库（无真实 DB）。<br>
            • 增删改走 <code>update</code> 统一通道，操作后重新加载权威列表；<br>
            • 每行的 <code>data-wire-key</code> 让前端做最小化 diff，勾选/输入框状态在刷新后保留；<br>
            • 分页器使用原生 Laravel 分页（<code>$paginator->links('pageinator')</code>），外层包 <code>wire:pagination wire:target="list"</code> 后点击不整页跳；<br>
            • 列表首屏直接由后端渲染真实数据（无懒加载闪烁）。
        </p>

        {{-- 新增表单（演示第5点：INSERT） --}}
        <div class="mdui-card" style="margin-top: 16px;">
            <div class="mdui-card-header">
                <div class="mdui-card-header-title">新增任务（服务端 INSERT）</div>
            </div>
            <div class="mdui-card-content">
                <div class="mdui-textfield" style="display:inline-block; width: 280px;">
                    <label class="mdui-textfield-label">任务名称</label>
                    <input class="mdui-textfield-input" type="text" wire:model="name" value="{{ $name }}" placeholder="输入任务名..." />
                </div>
                <button wire:click="addItem" wire:target="list" class="mdui-btn mdui-btn-raised mdui-color-theme mdui-ripple">
                    <i class="mdui-icon material-icons" style="margin-right: 4px;">add</i>添加
                </button>
                <span wire:loading wire:target="addItem" class="mdui-spinner" style="display:none;"></span>
            </div>
        </div>

        {{-- 列表区：直接用 wire:section="list" 属性标记（不引入第二个 @section，保持 jblade @section 语义不变）。
             前端 Wire.refresh(['list']) 只拉取这块；后端按属性从 content 中截取返回。 --}}
        {{-- 第4点：wire:lazy 声明式懒加载。首次 GET 由后端 once 空壳渲染 spinner 占位，
             前端页面 load 后自动 Wire.refresh(['list']) 拉真实数据，无需手写 if/else。 --}}
        <div wire:section="list" class="mdui-card" style="margin-top: 16px;">
            <div class="mdui-card-header">
                <div class="mdui-card-header-title">任务列表（共 {{ $total }} 项，每页 {{ $perPage }}）</div>
            </div>
            <div class="mdui-card-content">
                @if($items->isEmpty())
                    <div class="mdui-spinner" style="margin: 16px;"></div>
                    <span class="hint">加载中…（懒加载：等前端拉取）</span>
                @else
                <table class="data-table" style="width:100%; border-collapse: collapse;">
                    <thead>
                        <tr>
                            <th style="text-align:left; padding:6px;">完成</th>
                            <th style="text-align:left; padding:6px;">ID</th>
                            <th style="text-align:left; padding:6px;">名称</th>
                            <th style="text-align:left; padding:6px;">操作</th>
                        </tr>
                    </thead>
                    <tbody>
                        @foreach($items as $item)
                        {{-- data-wire-key 让前端在更新时复用此行 DOM，保留勾选/输入状态（第2点） --}}
                        <tr data-wire-key="{{ $item['id'] }}">
                            <td style="padding:6px;">
                                <label class="mdui-checkbox">
                                    <input type="checkbox" wire:model="done" wire:target="list" {{ $item['done'] ? 'checked' : '' }} wire:click="updateItem" wire:param-id="{{ $item['id'] }}" />
                                    <i class="mdui-icon"></i>
                                </label>
                            </td>
                            <td style="padding:6px;">{{ $item['id'] }}</td>
                            <td style="padding:6px;">
                                <input class="mdui-textfield-input" type="text"
                                       wire:model="name" value="{{ $item['name'] }}" style="min-width:160px;" />
                                {{-- 保存改名（演示 UPDATE 单条）。name 取自同行的 wire:model="name" 输入框，
                                     由 wire.js 在点击时按所在 [data-wire-key] 行自动收集，避免用服务端旧值覆盖用户输入 --}}
                                <button wire:click="updateItem" wire:target="list"
                                        wire:param-id="{{ $item['id'] }}"
                                        class="mdui-btn mdui-btn-dense mdui-ripple">保存改名</button>
                            </td>
                            <td style="padding:6px;">
                                <button wire:click="deleteItem" wire:target="list" wire:param-id="{{ $item['id'] }}"
                                        class="mdui-btn mdui-btn-dense mdui-color-red mdui-ripple">删除</button>
                            </td>
                        </tr>
                        @endforeach
                    </tbody>
                </table>

                {{-- 第6点：原生 Laravel 分页器（pageinator.jblade 完全不动）。
                     外层 wire:pagination + wire:target="list" 让前端拦截 a[href=?page=N]，
                     发起 $paginate 并只刷新 list section，不整页跳转。
                     {!! !!} 原样输出 HTML，避免分页器引号被转义破坏 a[href]。 --}}
                <div wire:pagination wire:target="list" style="margin-top: 12px;">
                    {!! $paginatorHtml !!}
                </div>
                @endif
            </div>
        </div>

        {{-- 第1点：手动精准刷新演示 —— 模拟「别人改了后端，我要主动拉最新」 --}}
        <div class="mdui-card" style="margin-top: 16px;">
            <div class="mdui-card-header">
                <div class="mdui-card-header-title">精准刷新（Wire.refresh）</div>
            </div>
            <div class="mdui-card-content">
                <p class="hint">点击下面按钮 = 调用 <code>Wire.refresh(['list'])</code>，只拉取 list section 的最新数据，不触碰别的组件、也不整页刷新。</p>
                <button onclick="Wire.refresh(['list'])" class="mdui-btn mdui-btn-raised mdui-color-theme mdui-ripple">
                    <i class="mdui-icon material-icons" style="margin-right: 4px;">refresh</i>只刷新列表
                </button>
            </div>
        </div>
    </div>
</div>

{{-- 第4点：懒加载已由 wire.js 的 initLazy() 自动处理（页面 load 后对 wire:lazy 的 section 自动发 $refresh），无需手写脚本 --}}
@endsection
