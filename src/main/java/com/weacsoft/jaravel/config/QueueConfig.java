package com.weacsoft.jaravel.config;

import org.springframework.context.annotation.Configuration;

/**
 * 队列配置（对齐 Laravel 的 {@code config/queue.php}）。
 * <p>
 * 由 {@code artisan vendor:publish --tag=queue} 发布生成，可自由修改。
 *
 * <h2>一、三层优先级：声明 → 配置 → 默认</h2>
 * <ol>
 *   <li><b>声明</b>：本类中的 {@code @RegisterQueueDriver} 方法（<b>全局只允许一个</b>）。
 *       要覆盖框架已装配的驱动，用 {@code @RegisterQueueDriver(override = true)}</li>
 *   <li><b>配置</b>：{@code application.yml} 的 {@code jaravel.queue.driver}（本项目采用）</li>
 *   <li><b>默认（兜底）</b>：没写 driver → {@code sync}，任务在当前线程立即执行，
 *       不需要任何外部依赖，保证功能基本可用</li>
 * </ol>
 *
 * <h2>二、"安装 ≠ 启用"：驱动按需装配</h2>
 * <ul>
 *   <li>{@code RedisQueueDriver} — 条件 {@code OnRedisQueueDriverCondition}：
 *       <b>严格按需</b>，必须 {@code jaravel.queue.driver=redis} 才装配。
 *       即使引入 jaravel-redis，不写也不会连接 Redis</li>
 *   <li>{@code DatabaseQueueDriver} — 条件 {@code OnDatabaseQueueDriverCondition}：
 *       <b>严格按需</b>，必须 {@code jaravel.queue.driver=database} 才装配，
 *       此时才需要 {@code DataSource} 与 {@code jobs} / {@code failed_jobs} 表</li>
 *   <li><b>没有 redis → database 的互相回退</b>。写谁装配谁；都没写就走 sync 内存兜底</li>
 * </ul>
 *
 * <h2>三、驱动一览</h2>
 * <table border="1">
 *   <caption>可用的 queue 驱动</caption>
 *   <tr><th>driver</th><th>依赖</th><th>说明</th></tr>
 *   <tr><td>{@code sync}</td><td>queue（内置）</td><td><b>兜底默认</b>，当前线程同步执行</td></tr>
 *   <tr><td>{@code database}</td><td>queue-database + DataSource</td><td>需 {@code artisan queue:table} 建表</td></tr>
 *   <tr><td>{@code redis}</td><td>queue-database + jaravel-redis</td><td>高吞吐，多实例消费</td></tr>
 * </table>
 */
@Configuration
public class QueueConfig {

    // =====================================================================
    // 方式一（本项目采用）：配置式 —— application.yml
    // =====================================================================
    // jaravel:
    //   queue:
    //     driver: sync                  # sync（兜底默认）| database | redis
    //     failed-job-retention-days: 7
    //     database:                     # driver=database 时生效
    //       table: jobs
    //       retry-after: 1800
    //       max-attempts: 3
    //       retry-delay-ms: 5000
    //       poll-interval-ms: 1000
    //       worker-threads: 1
    //       auto-start: false
    //       queues: [default, emails, payments, invoices]
    //     # redis:                      # driver=redis 时生效
    //     #   connection: default
    //     #   queue: default
    //     #   block-for-seconds: 5

    // =====================================================================
    // 方式二：注解声明式 —— 自定义队列驱动（全局唯一）
    // =====================================================================
    // 不声明则使用框架按 driver 自动装配的驱动。
    // 如需覆盖框架已装配的驱动，加 override = true。
    //
    // @RegisterQueueDriver
    // public QueueDriver myQueueDriver() {
    //     return new MyQueueDriver();
    // }
    //
    // @RegisterQueueDriver(override = true)
    // public QueueDriver rabbitQueueDriver(RabbitTemplate rabbit) {
    //     return new RabbitQueueDriver(rabbit);
    // }

    // =====================================================================
    // 方式三：兜底默认 —— 什么都不写
    // =====================================================================
    // 删除本类、且不配 jaravel.queue.driver 时，走 sync 同步模式：
    // 任务在派发线程内立即执行，不落库、不连 Redis，开发期最省事。

    // =====================================================================
    // 附：database 驱动的表要求
    // =====================================================================
    // 需要 jobs 与 failed_jobs 两张表：
    //   artisan queue:table    # 生成迁移
    //   artisan migrate        # 执行迁移
}
