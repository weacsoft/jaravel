@extends('docs.layout')

@section('content')
<h1>事件系统</h1>
<p>Jaravel 提供对齐 Laravel 的事件系统，包含事件定义、监听器、异步队列分发和自动重试。实现 <code>ShouldQueue</code> 接口的监听器将被异步分发到命名队列执行。</p>

<h2>事件定义</h2>
<pre><code>public class UserRegisteredEvent implements Event {
    private final Long userId;
    private final String name;

    public UserRegisteredEvent(Long userId, String name) {
        this.userId = userId;
        this.name = name;
    }

    public Long getUserId() { return userId; }
    public String getName() { return name; }
}</code></pre>

<h2>监听器</h2>
<pre><code>// 同步监听器
public class LogRegistrationListener implements Listener&lt;UserRegisteredEvent&gt; {
    @Override
    public void handle(UserRegisteredEvent event) {
        log.info("用户注册: {}", event.getName());
    }
}

// 异步监听器（实现 ShouldQueue 自动异步执行）
public class SendWelcomeEmailListener implements Listener&lt;UserRegisteredEvent&gt;, ShouldQueue {
    @Override
    public void handle(UserRegisteredEvent event) {
        log.info("发送欢迎邮件: {}", event.getName());
    }

    @Override
    public String queue() { return "email"; }  // 使用 email 队列
}</code></pre>

<h2>注册监听器</h2>
<p>方式一：在 EventServiceProvider 中手动注册：</p>
<pre><code>@Component
public class AppEventServiceProvider extends EventServiceProvider {
    @Override
    public void register() {
        listen(UserRegisteredEvent.class, new LogRegistrationListener());
        listen(UserRegisteredEvent.class, new SendWelcomeEmailListener());
    }
}</code></pre>

<p>方式二：使用 @ListensTo 注解自动注册：</p>
<pre><code>@Component
@ListensTo(NotificationEvent.class)
public class SendNotificationJob implements Listener&lt;NotificationEvent&gt;, ShouldQueue {
    @Override
    public void handle(NotificationEvent event) {
        // 异步处理通知
    }

    @Override
    public String queue() { return "notification"; }
}</code></pre>

<h2>分发事件</h2>
<pre><code>Dispatcher dispatcher = SpringContext.bean(Dispatcher.class);
dispatcher.dispatch(new UserRegisteredEvent(user.getId(), user.getName()));</code></pre>

<h2>异步队列</h2>
<p>实现 <code>ShouldQueue</code> 的监听器将被异步分发到命名队列执行，每个队列拥有独立线程池，互不阻塞：</p>
<pre><code>jaravel:
  event:
    queue-enabled: true
    queue:
      default:
        pool-size: 4           # 默认队列线程池大小
      notification:            # notification 队列
        pool-size: 2
      email:                   # email 队列
        pool-size: 2
    retry:
      max-attempts: 3          # 最大重试次数
      delay-ms: 1000           # 重试间隔毫秒</code></pre>

<h2>重试机制</h2>
<p>监听器执行失败时自动重试，可配置最大重试次数和重试间隔：</p>
<ul>
    <li><code>max-attempts</code> - 最大重试次数（不含首次执行），默认 3</li>
    <li><code>delay-ms</code> - 重试间隔毫秒，默认 1000</li>
</ul>

<h2>演示路由</h2>
<pre><code># 分发通知事件（SendNotificationJob 异步执行）
curl http://localhost:8080/api/queue/demo

# 自定义通知参数
curl "http://localhost:8080/api/queue/demo?type=sms&recipient=13800138000&content=验证码123456"</code></pre>

<div class="note">
    <strong>注意：</strong>queue-enabled 为 false 时，所有监听器同步执行（包括实现 ShouldQueue 的）。要启用异步分发，需设置 queue-enabled: true。查看日志输出可确认异步执行效果。
</div>
@endsection
