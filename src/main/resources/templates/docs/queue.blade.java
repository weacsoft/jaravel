@extends('docs.layout')

@section('content')
<h1>队列</h1>
<p>Jaravel 提供两种队列机制：基于事件系统的异步监听器（内存队列）和基于数据库的持久化队列。实现 <code>ShouldQueue</code> 接口的监听器将被异步分发到命名队列执行。</p>

<h2>异步监听器</h2>
<p>监听器实现 <code>ShouldQueue</code> 接口后，将被异步分发到指定队列执行：</p>
<pre><code>@Component
@ListensTo(NotificationEvent.class)
public class SendNotificationJob implements Listener&lt;NotificationEvent&gt;, ShouldQueue {

    @Override
    public void handle(NotificationEvent event) {
        // 异步处理通知
        log.info("发送通知: {}", event.getContent());
    }

    @Override
    public String queue() {
        return "notification";  // 使用 notification 队列
    }

    @Override
    public long delay() {
        return 0;  // 立即执行（可配置延迟毫秒数）
    }
}</code></pre>

<h2>事件定义与分发</h2>
<pre><code>// 定义事件
public class NotificationEvent implements Event {
    private final String type;
    private final String recipient;
    private final String content;
    // constructor, getters...
}

// 分发事件（监听器自动异步执行）
Dispatcher dispatcher = SpringContext.bean(Dispatcher.class);
dispatcher.dispatch(new NotificationEvent("email", "alice@test.com", "欢迎注册"));</code></pre>

<h2>数据库队列</h2>
<p>queue-database 模块提供基于数据库的持久化队列，任务以 JSON 序列化存储到 <code>jaravel_jobs</code> 表：</p>
<pre><code>jaravel:
  queue:
    database:
      enabled: true
      table: jaravel_jobs
      default-queue: default
      max-attempts: 5
      sleep-interval: 500</code></pre>

<p>DatabaseQueueWorker 在后台线程中轮询数据库取出任务，通过反射调用 Job 处理类：</p>
<pre><code>// 推入即时任务
String jobId = driver.push("default", "SendEmailJob", emailPayload, 0);

// 推入延迟任务（60 秒后执行）
String delayedId = driver.push("default", "SendReminderJob", reminderData, 60);

// 取出并处理
QueuedJob job = driver.pop("default");
if (job != null) {
    try {
        processJob(job);
        driver.delete("default", job.getId());
    } catch (Exception e) {
        driver.release("default", job, 30);  // 30 秒后重试
    }
}</code></pre>

<h2>重试机制</h2>
<p>监听器执行失败时自动重试，可配置最大重试次数和重试间隔：</p>
<pre><code>jaravel:
  event:
    queue-enabled: true
    retry:
      max-attempts: 3    # 最大重试次数（不含首次执行）
      delay-ms: 1000     # 重试间隔毫秒</code></pre>

<h2>多队列隔离</h2>
<p>每个命名队列拥有独立线程池，不同队列互不阻塞：</p>
<pre><code>jaravel:
  event:
    queue-enabled: true
    queue:
      default:
        pool-size: 4           # 默认队列
      notification:
        pool-size: 2           # 通知队列
      email:
        pool-size: 2           # 邮件队列</code></pre>

<h2>演示路由</h2>
<pre><code># 分发通知事件（SendNotificationJob 异步执行）
curl http://localhost:8080/api/queue/demo

# 自定义通知参数
curl "http://localhost:8080/api/queue/demo?type=email&recipient=alice@test.com&content=Hello"</code></pre>

<div class="note">
    <strong>注意：</strong>queue-enabled 为 false 时，所有监听器同步执行。要启用异步分发，需设置 queue-enabled: true。异步执行结果可通过查看应用日志确认。
</div>

<div class="tip">
    <strong>提示：</strong>数据库队列适用于不需要引入 Redis/RabbitMQ 的轻量级异步任务场景。如需更高性能和可靠性，建议使用 Redis 队列或专业消息中间件。
</div>
@endsection
