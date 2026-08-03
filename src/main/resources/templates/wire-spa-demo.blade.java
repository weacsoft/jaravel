@extends('layout')

@section('content')
<div class="wire-spa" wire:section="content" style="display:flex; min-height: 70vh; gap: 16px;">
    {{-- 左侧菜单：wire:nav-menu 标记分组，菜单项用 wire:nav="pageKey" 触发 SPA 导航（第3点） --}}
    <nav wire:nav-menu class="mdui-card" style="width: 200px; flex: 0 0 200px; padding: 8px;">
        <div class="mdui-list">
            <a wire:nav="home"  class="mdui-list-item mdui-ripple @if($page === 'home') wire-nav-active @endif" style="display:block; padding:12px; cursor:pointer;">概览</a>
            <a wire:nav="list"  class="mdui-list-item mdui-ripple @if($page === 'list') wire-nav-active @endif" style="display:block; padding:12px; cursor:pointer;">任务列表</a>
            <a wire:nav="about" class="mdui-list-item mdui-ripple @if($page === 'about') wire-nav-active @endif" style="display:block; padding:12px; cursor:pointer;">关于</a>
        </div>
    </nav>

    {{-- 右侧内容区：wire:nav-content 标记承载区，点击左侧菜单只刷新整个 content section，
         不整页跳转（第3点 SPA）。注意 wire:section="content" 已上移到最外层 .wire-spa，
         以保证 SPA 刷新范围与 @section('content') 范围一致（否则整页两栏会被塞进 main 导致错乱）。 --}}
    <main wire:nav-content class="mdui-card" style="flex:1; padding: 16px;">

        @if($page === 'home')
            <h2>概览</h2>
            <p>这是 SPA 演示：左侧菜单切换、列表懒加载 + 分页 + 精准刷新（CRUD）组合，全部基于 Wire 统一通道。</p>
            <p>当前任务总数：<b>{{ $total ?: 12 }}</b></p>
            <p><a wire:nav="list" style="cursor:pointer; color:#2196f3;">前往任务列表 →</a></p>

        @elseif($page === 'list')
            {{-- 第7点组合：懒加载 + 分页 + 精准刷新/CRUD，仅这一个 section 参与更新 --}}
            <div wire:section="list" wire:lazy class="mdui-card" style="margin-top: 8px;">
                <div class="mdui-card-header">
                    <div class="mdui-card-header-title">任务列表（共 {{ $total }} 项，每页 {{ $perPage }}）</div>
                </div>
                <div class="mdui-card-content">
                    @if(empty($items))
                        <div class="mdui-spinner" style="margin: 16px;"></div>
                        <span class="hint">加载中…（wire:lazy 自动拉取）</span>
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
                            <tr data-wire-key="{{ $item['id'] }}">
                                <td style="padding:6px;">
                                    <label class="mdui-checkbox">
                                        <input type="checkbox" wire:click="updateItem" wire:param-id="{{ $item['id'] }}" {{ $item['done'] ? 'checked' : '' }} />
                                        <i class="mdui-icon"></i>
                                    </label>
                                </td>
                                <td style="padding:6px;">{{ $item['id'] }}</td>
                                <td style="padding:6px;">
                                    <input class="mdui-textfield-input" type="text" wire:model="name" value="{{ $item['name'] }}" style="min-width:160px;" />
                                    <button wire:click="updateItem" wire:param-id="{{ $item['id'] }}" class="mdui-btn mdui-btn-dense mdui-ripple">改名</button>
                                </td>
                                <td style="padding:6px;">
                                    <button wire:click="deleteItem" wire:param-id="{{ $item['id'] }}" class="mdui-btn mdui-btn-dense mdui-color-red mdui-ripple">删除</button>
                                </td>
                            </tr>
                            @endforeach
                        </tbody>
                    </table>

                    {{-- 原生 Laravel 分页器（pageinator.jblade 语义不变），wire:pagination 拦截分页点击 --}}
                    {{-- {!! !!} 原样输出 HTML，避免分页器引号被转义破坏 a[href] --}}
                    <div wire:pagination wire:target="list" style="margin-top: 12px;">
                        {!! $paginatorHtml !!}
                    </div>
                    @endif
                </div>
            </div>
            {{-- 第1点：手动精准刷新按钮（只刷新 list） --}}
            <button wire:click="$refresh" wire:target="list" class="mdui-btn mdui-btn-dense mdui-ripple" style="margin-top:8px;">刷新列表</button>

        @else
            <h2>关于</h2>
            <p>本页演示 Jaravel Wire 模块：</p>
            <ul>
                <li>第1点：组件级精准刷新（Wire.refresh(['list'])）</li>
                <li>第2点：列表交互 key 稳定（data-wire-key）</li>
                <li>第3点：SPA 导航（wire:nav，只刷右侧 content）</li>
                <li>第4点：懒加载（wire:lazy 声明式）</li>
                <li>第5点：CRUD 绑定 DB</li>
                <li>第6点：原生 Laravel 分页器（pageinator.jblade 不动）</li>
                <li>第7点：懒加载 + 分页 + 精准刷新组合</li>
            </ul>
        @endif
    </main>
</div>

<style>
    .wire-nav-active { background: #e3f2fd; font-weight: 600; border-left: 3px solid #2196f3; }
</style>
@endsection
