@extends('wire-layout')

@section('title', 'Wire 演示 — 记录列表')

@section('sidebar')
    <a href="/wire" wire-navigate>📊 仪表盘</a>
    <a href="/wire/records" wire-navigate class="active">📋 记录列表</a>
    <a href="/wire/spa" wire-navigate>🧭 SPA 导航</a>
    <a href="/wire/tasks" wire-navigate>📝 CRUD 列表</a>
    <a href="/wire/components" wire-navigate>🧩 命名组件</a>
@endsection

@section('content')
    <h2 style="margin-bottom:8px;">📋 记录列表</h2>
    <p class="hint" style="margin-bottom:20px;">
        返回<a href="/wire" wire-navigate>仪表盘</a>，体验同一 layout 下多 Controller 切换：
        只有 <b>content</b> 和 <b>sidebar</b> 区域的 diff 被传输，顶栏保持不变。
    </p>

    <div class="card">
        <table>
            <thead>
                <tr>
                    <th>ID</th>
                    <th>标题</th>
                    <th>类型</th>
                    <th>金额</th>
                    <th>日期</th>
                    <th>状态</th>
                </tr>
            </thead>
            <tbody>
                @forelse($records ?? [] as $r)
                <tr>
                    <td>{{ $r['id'] ?? '—' }}</td>
                    <td>{{ $r['title'] ?? '—' }}</td>
                    <td>
                        <span class="badge @if(($r['type'] ?? '') === 'income') badge-success @else badge-warning @endif">
                            {{ ($r['type'] ?? '') === 'income' ? '收入' : '支出' }}
                        </span>
                    </td>
                    <td>¥{{ number_format($r['amount'] ?? 0, 2) }}</td>
                    <td>{{ $r['date'] ?? '—' }}</td>
                    <td>{{ ($r['status'] ?? '') === 'done' ? '已完成' : '待处理' }}</td>
                </tr>
                @empty
                <tr>
                    <td colspan="6" style="text-align:center; padding:24px; color:#888;">暂无记录</td>
                </tr>
                @endforelse
            </tbody>
        </table>
    </div>

    <div class="card">
        <h3 style="margin-bottom:8px;">💡 演示要点</h3>
        <ul style="line-height:2; padding-left:20px;">
            <li>本页面和仪表盘<b>共享同一个 wire-layout 布局</b></li>
            <li>布局中的 sidebar 内容不同（高亮项不同），但<b>结构相同</b></li>
            <li>导航时只传输 content 和 sidebar 的 diff，顶栏和 head 保持不变</li>
            <li>两个页面分别由 <b>WireShowcaseController</b> 的两个方法处理——多 Controller 方法，同一继承链</li>
        </ul>
    </div>
@endsection
