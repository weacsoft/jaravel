@extends('docs.layout')

@section('content')
<h1>定时任务</h1>
<p>Jaravel 提供对齐 Laravel 的任务调度框架，支持 Cron 表达式、固定间隔任务和一次性延迟任务。当 Redis 可用时，通过分布式锁确保多实例环境下同一任务只有一个实例执行。</p>

<h2>任务注册</h2>
<p>在 <code>ScheduleConfig</code> 中通过 <code>Schedule</code> 注册任务：</p>
<pre><code>@Component
public class ScheduleConfig {

    @Autowired
    private Schedule schedule;

    @PostConstruct
    public void setup() {
        // 每分钟执行一次的缓存清理任务
        schedule.call("cleanup-expired-cache", this::cleanupExpiredCache)
                .everyMinute()
                .withDistributedLock()
                .description("每分钟清理过期缓存");

        // 每天凌晨 0 点执行的日报任务
        schedule.call("daily-report", this::generateDailyReport)
                .daily()
                .withDistributedLock()
                .description("每天凌晨生成日报");
    }

    private void cleanupExpiredCache() {
        log.info("清理过期缓存...");
    }

    private void generateDailyReport() {
        log.info("生成日报...");
    }
}</code></pre>

<h2>Cron 表达式</h2>
<p>支持 6 字段 Cron 表达式（秒 分 时 日 月 周）：</p>
<pre><code>// 自定义 Cron 表达式
schedule.call("task", this::runTask)
        .cron("0 0 2 * * ?")       // 每天凌晨 2 点

// 便捷方法
schedule.call("task", this::runTask).everyMinute();       // 每分钟
schedule.call("task", this::runTask).hourly();            // 每小时
schedule.call("task", this::runTask).dailyAt("18:30");   // 每天 18:30
schedule.call("task", this::runTask).daily();            // 每天午夜
schedule.call("task", this::runTask).weekly();            // 每周
schedule.call("task", this::runTask).monthly();           // 每月</code></pre>

<h2>Redis 分布式锁</h2>
<p>启用分布式锁后，多实例环境下同一任务只有一个实例执行：</p>
<pre><code>// 启用分布式锁（默认 TTL 300 秒）
schedule.call("task", this::runTask)
        .daily()
        .withDistributedLock();

// 指定锁 TTL
schedule.call("task", this::runTask)
        .daily()
        .withDistributedLock(600);  // 10 分钟 TTL</code></pre>

<p>分布式锁基于 Redis <code>SET key value NX EX seconds</code> 原子命令实现，获取锁失败则跳过本次执行。</p>

<h2>Artisan 命令任务</h2>
<p>也可注册定时执行的 Artisan 命令：</p>
<pre><code>// 每天凌晨执行 artisan 命令
schedule.command("daily:cleanup")
        .daily()
        .withDistributedLock();</code></pre>

<h2>配置</h2>
<pre><code>jaravel:
  schedule:
    enabled: true              # 启用调度（默认启用）
    # pool-size: 4              # 调度线程池大小（默认 4）
    # distributed-lock: true    # 启用分布式锁（默认 true）
    # lock-connection: cache    # 锁 Redis 连接名</code></pre>

<h2>查看任务状态</h2>
<pre><code>curl http://localhost:8080/api/schedule/status</code></pre>

<p>返回所有已注册任务的信息，包括任务名、Cron 表达式、是否启用分布式锁等。</p>

<div class="note">
    <strong>注意：</strong>ScheduleRunner 在应用启动时自动调度所有已注册任务。Redis 不可用时，分布式锁功能自动降级（所有实例都会执行任务）。生产环境建议配置 Redis 以确保任务不重复执行。
</div>
@endsection
