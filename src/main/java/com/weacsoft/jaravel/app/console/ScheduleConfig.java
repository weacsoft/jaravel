package com.weacsoft.jaravel.app.console;

import com.weacsoft.jaravel.vendor.schedule.Schedule;
import com.weacsoft.jaravel.vendor.schedule.ScheduledTask;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 定时任务配置，对齐 Laravel 的 {@code app/Console/Kernel.php} schedule 定义。
 * <p>
 * 通过 {@link Schedule} 注册定时任务，{@code ScheduleRunner} 在应用启动时自动调度执行。
 * 当 Redis 可用时，启用分布式锁确保多实例环境下同一任务只有一个实例执行。
 * <p>
 * 注册的任务：
 * <ul>
 *   <li><b>cleanup-expired-cache</b> - 每分钟执行一次，清理过期缓存</li>
 *   <li><b>daily-report</b> - 每天凌晨 0 点执行，生成日报</li>
 * </ul>
 */
@Component
public class ScheduleConfig {

    private static final Logger log = LoggerFactory.getLogger(ScheduleConfig.class);

    @Autowired
    private Schedule schedule;

    @PostConstruct
    public void setup() {
        // 任务一：每分钟执行一次的缓存清理任务
        // 对齐 Laravel $schedule->call(function () { ... })->everyMinute();
        schedule.call("cleanup-expired-cache", this::cleanupExpiredCache)
                .everyMinute()
                .withDistributedLock()  // 启用分布式锁（Redis 可用时生效）
                .description("每分钟清理过期缓存");

        // 任务二：每天凌晨 0 点执行的日报任务
        // 对齐 Laravel $schedule->call(function () { ... })->dailyAt('00:00');
        schedule.call("daily-report", this::generateDailyReport)
                .daily()
                .withDistributedLock()
                .description("每天凌晨生成日报");

        log.info("[Schedule] 已注册 {} 个定时任务", schedule.size());
        for (ScheduledTask task : schedule.all()) {
            log.info("[Schedule]   - {} | cron={} | locked={} | desc={}",
                    task.getName(), task.getCronExpression(),
                    task.isDistributedLock(), task.getDescription());
        }
    }

    /**
     * 清理过期缓存。
     * <p>
     * 演示用：打印清理日志。生产环境可调用 Cache::forget() 清理指定键，
     * 或通过 Redis SCAN 命令批量清理带前缀的过期键。
     */
    private void cleanupExpiredCache() {
        String now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        log.info("[Schedule] cleanup-expired-cache 执行于 {}", now);
        // TODO: 实际清理逻辑，例如遍历缓存键并删除过期项
    }

    /**
     * 生成日报。
     * <p>
     * 演示用：打印日报生成日志。生产环境可查询数据库统计、生成报表文件、发送邮件等。
     */
    private void generateDailyReport() {
        String today = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        log.info("[Schedule] daily-report 执行于 {} 00:00:00", today);
        log.info("[Schedule] 正在生成 {} 的日报...", today);
        // TODO: 实际日报生成逻辑，例如统计用户数、订单数等
        log.info("[Schedule] 日报生成完成");
    }
}
