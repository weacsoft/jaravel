package com.weacsoft.jaravel.config;

import com.weacsoft.jaravel.vendor.queue.database.QueueDriver;
import com.weacsoft.jaravel.vendor.queue.database.RegisterQueueDriver;
import org.springframework.context.annotation.Configuration;

/**
 * 队列配置，对齐 Laravel config/queue.php。
 * <p>
 * 由 {@code artisan vendor:publish --tag=queue} 发布生成，可自由修改。
 *
 * <h3>驱动选择与回退</h3>
 * <ol>
 *   <li>{@code @RegisterQueueDriver} 注解注册的驱动（<b>全局只允许一个</b>）</li>
 *   <li>自动装配：{@code jaravel.queue.driver=redis} 且引入 jaravel-redis</li>
 *   <li>自动装配：{@code jaravel.queue.driver=database} 且存在 DataSource</li>
 *   <li>都没有 → <b>sync 同步模式</b>，任务在当前线程立即执行</li>
 * </ol>
 *
 * <h3>数据库表要求</h3>
 * 使用 database 驱动时需要 {@code jobs} 与 {@code failed_jobs} 两张表，
 * 执行 {@code artisan queue:table} 生成迁移后再 {@code artisan migrate}。
 */
@Configuration
public class QueueConfig {

    // 自定义队列驱动（全局唯一）。不声明则使用框架自动装配的驱动。
    // 如需覆盖框架已装配的驱动，请使用 @RegisterQueueDriver(override = true)
    //
    // @RegisterQueueDriver
    // public QueueDriver myQueueDriver() {
    //     return new MyQueueDriver();
    // }
}
