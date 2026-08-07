@extends('wire-layout')

@section('title', 'Wire 演示 — CRUD 任务列表')

@section('sidebar')
    <a href="/wire" wire-navigate>📊 仪表盘</a>
    <a href="/wire/records" wire-navigate>📋 记录列表</a>
    <a href="/wire/spa" wire-navigate>🧭 SPA 导航</a>
    <a href="/wire/tasks" wire-navigate class="active">📝 CRUD 列表</a>
    <a href="/wire/components" wire-navigate>🧩 命名组件</a>
@endsection

@section('content')
    <h2 style="margin-bottom:16px;">📝 CRUD 任务列表（SQLite）</h2>
    <p class="hint" style="margin-bottom:20px;">
        基于真实数据库（SQLite）。增删改后<b>只刷新列表</b>，输入框状态保留。
        返回<a href="/wire/spa?action=tasks" wire-navigate>SPA 任务列表</a>对比体验。
    </p>

    <div class="card">
        <form method="post" action="/api/wire/tasks" style="display:flex; gap:8px; margin-bottom:16px;">
            <input type="text" name="name" placeholder="输入任务名称..."
                   style="flex:1; padding:8px 12px; border:1px solid #ddd; border-radius:8px; font-size:14px;"
                   data-wire-key="task-input">
            <input type="hidden" name="action" value="add">
            <button type="submit" class="btn btn-primary">+ 添加</button>
        </form>
    </div>

    <div class="card">
        <h3 style="margin-bottom:12px;">任务列表（共 {{ $total ?? 0 }} 项，已完成 {{ $doneCount ?? 0 }} 项）</h3>
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
                <tr data-wire-key="task-{{ $task['id'] }}">
                    <td>
                        <form method="post" action="/api/wire/tasks">
                            <input type="hidden" name="action" value="toggle">
                            <input type="hidden" name="id" value="{{ $task['id'] }}">
                            <button type="submit" style="background:none; border:none; cursor:pointer; font-size:18px;">
                                @if($task['done']) ✅ @else ⬜ @endif
                            </button>
                        </form>
                    </td>
                    <td>{{ $task['id'] }}</td>
                    <td>
                        <form method="post" action="/api/wire/tasks" style="display:flex; gap:4px;">
                            <input type="hidden" name="action" value="update">
                            <input type="hidden" name="id" value="{{ $task['id'] }}">
                            <input type="text" name="name" value="{{ $task['name'] }}"
                                   style="flex:1; padding:4px 8px; border:1px solid #ddd; border-radius:4px; font-size:14px;"
                                   data-wire-key="task-name-{{ $task['id'] }}">
                            <button type="submit" class="btn btn-outline" style="padding:4px 8px; font-size:12px;">保存</button>
                        </form>
                    </td>
                    <td>
                        <form method="post" action="/api/wire/tasks">
                            <input type="hidden" name="action" value="delete">
                            <input type="hidden" name="id" value="{{ $task['id'] }}">
                            <button type="submit" class="btn btn-danger" style="padding:4px 8px; font-size:12px;">删除</button>
                        </form>
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
