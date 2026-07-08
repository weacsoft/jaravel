package com.weacsoft.jaravel.app.console;

import com.weacsoft.jaravel.vendor.schedule.Schedule;
import com.weacsoft.jaravel.vendor.schedule.ScheduledTask;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 定时任务配置，对齐 Laravel 的 {@code app/Console/Kernel.php} schedule 定义。
 * <p>
 * 注册插件平台相关的定时清理任务：
 * <ul>
 *   <li><b>cleanup-temp-classfiles</b> - 每小时执行，清理在线编译产生的临时 .class 文件</li>
 *   <li><b>cleanup-expired-cache</b> - 每天执行，清理过期缓存</li>
 * </ul>
 */
@Component
public class ScheduleConfig {

    private static final Logger log = LoggerFactory.getLogger(ScheduleConfig.class);

    @Autowired
    private Schedule schedule;

    @PostConstruct
    public void setup() {
        // 任务一：每小时清理临时编译文件
        schedule.call("cleanup-temp-classfiles", this::cleanupTempClassFiles)
                .hourly()
                .withDistributedLock()
                .description("每小时清理在线编译产生的临时 .class 文件");

        // 任务二：每天清理过期缓存
        schedule.call("cleanup-expired-cache", this::cleanupExpiredCache)
                .daily()
                .withDistributedLock()
                .description("每天清理过期缓存");

        log.info("[Schedule] 已注册 {} 个定时任务", schedule.size());
        for (ScheduledTask task : schedule.all()) {
            log.info("[Schedule]   - {} | cron={} | locked={} | desc={}",
                    task.getName(), task.getCronExpression(),
                    task.isDistributedLock(), task.getDescription());
        }
    }

    /**
     * 清理在线编译产生的临时 .class 文件。
     * <p>
     * 扫描系统临时目录中以 "java-run-" 为前缀的目录并删除。
     */
    private void cleanupTempClassFiles() {
        String now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        log.info("[Schedule] cleanup-temp-classfiles 执行于 {}", now);

        File tempDir = new File(System.getProperty("java.io.tmpdir"));
        File[] javaRunDirs = tempDir.listFiles((dir, name) -> name.startsWith("java-run-"));
        if (javaRunDirs != null) {
            int cleaned = 0;
            for (File dir : javaRunDirs) {
                if (deleteDirectory(dir)) {
                    cleaned++;
                }
            }
            log.info("[Schedule] 清理了 {} 个临时编译目录", cleaned);
        }
    }

    /**
     * 清理过期缓存。
     */
    private void cleanupExpiredCache() {
        String now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        log.info("[Schedule] cleanup-expired-cache 执行于 {}", now);
    }

    /** 递归删除目录 */
    private boolean deleteDirectory(File dir) {
        if (dir.isDirectory()) {
            File[] children = dir.listFiles();
            if (children != null) {
                for (File child : children) {
                    deleteDirectory(child);
                }
            }
        }
        return dir.delete();
    }
}
