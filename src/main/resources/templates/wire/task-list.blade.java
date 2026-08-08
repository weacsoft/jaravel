@extends('wire-layout')

@section('title', 'Wire 演示 — CRUD 任务列表')
@section('bodyClass', 'page-tasks')
@section('metaDescription', 'jaravel Wire CRUD：增删改后只刷新列表 section，输入框状态保留')

@section('sidebar')
    <a href="/wire" wire-navigate>📊 仪表盘</a>
    <a href="/wire/records" wire-navigate>📋 记录列表</a>
    <a href="/wire/spa" wire-navigate>🧭 SPA 导航</a>
    <a href="/wire/tasks" wire-navigate class="active">📝 CRUD 列表</a>
    <a href="/wire/components" wire-navigate>🧩 命名组件</a>
    <a href="/wire/anchors" wire-navigate>🔖 锚点改写</a>
@endsection

@section('scripts')
    {{-- 声明本页的 wire update 地址：所有 wire:click / wire:submit 都发往这里 --}}
    <script wire:config data-wire-update="/api/wire/tasks"></script>
@endsection

@section('content')
    <h2 style="margin-bottom:16px;">📝 CRUD 任务列表（SQLite）</h2>
    <p class="hint" style="margin-bottom:20px;">
        基于真实数据库（SQLite）。增删改走 <code>wire:click</code> / <code>wire:submit</code>，
        服务端只回传 <code>summary</code> 与 <code>list</code> 两个 section，<b>页面不整体刷新</b>。
        返回<a href="/wire/spa?action=tasks" wire-navigate>SPA 任务列表</a>对比体验。
    </p>

    <div class="card">
        {{-- wire:submit：拦截表单默认提交，收集表单字段作为 params 发往 update 地址 --}}
        <form wire:submit="add" style="display:flex; gap:8px; margin-bottom:16px;">
            <input type="text" name="name" placeholder="输入任务名称..."
                   style="flex:1; padding:8px 12px; border:1px solid #ddd; border-radius:8px; font-size:14px;">
            <button type="submit" class="btn btn-primary">+ 添加</button>
        </form>
    </div>

    <div class="card">
        <h3 style="margin-bottom:12px;" wire:section="summary">任务列表（共 {{ $total ?? 0 }} 项，已完成 {{ $doneCount ?? 0 }} 项）</h3>
        <table>
            <thead>
                <tr>
                    <th style="width:40px;">完成</th>
                    <th>ID</th>
                    <th>名称</th>
                    <th style="width:120px;">操作</th>
                </tr>
            </thead>
            <tbody wire:section="list">
                @forelse($tasks ?? [] as $task)
                {{-- data-wire-key 标出数据行：行内 wire:model 的实时值会随点击一并提交 --}}
                <tr data-wire-key="task-{{ $task['id'] }}">
                    <td>
                        <button type="button" wire:click="toggle" wire:param-id="{{ $task['id'] }}"
                                style="background:none; border:none; cursor:pointer; font-size:18px;">
                            @if($task['done']) ✅ @else ⬜ @endif
                        </button>
                    </td>
                    <td>{{ $task['id'] }}</td>
                    <td>
                        <div style="display:flex; gap:4px;">
                            <input type="text" wire:model="name" value="{{ $task['name'] }}"
                                   style="flex:1; padding:4px 8px; border:1px solid #ddd; border-radius:4px; font-size:14px;">
                            <button type="button" class="btn btn-outline" style="padding:4px 8px; font-size:12px;"
                                    wire:click="update" wire:param-id="{{ $task['id'] }}">保存</button>
                        </div>
                    </td>
                    <td>
                        <button type="button" class="btn btn-danger" style="padding:4px 8px; font-size:12px;"
                                wire:click="delete" wire:param-id="{{ $task['id'] }}">删除</button>
                    </td>
                </tr>
                @empty
                <tr>
                    <td colspan="4" style="text-align:center; padding:24px; color:#888;">暂无任务</td>
                </tr>
                @endforelse
            </tbody>
        </table>
    </div>
@endsection
