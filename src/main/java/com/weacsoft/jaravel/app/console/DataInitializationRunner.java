package com.weacsoft.jaravel.app.console;

import com.weacsoft.jaravel.app.model.admin.Admin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 数据初始化运行器，应用启动后自动检查并执行种子数据。
 * <p>
 * 当 admin 表为空时，自动调用 {@link DatabaseSeedCommand#handle()} 初始化：
 * <ul>
 *   <li>Admin 权限树（system 根 + 7 个子节点）</li>
 *   <li>User 权限树（platform 根 + java/jar 子节点 + 4 个叶子节点）</li>
 *   <li>超级管理员角色并分配所有 Admin 权限</li>
 *   <li>默认用户角色（普通用户、仅 Java、仅 Jar）</li>
 *   <li>初始管理员账号（admin/admin123）</li>
 * </ul>
 * <p>
 * 这样新用户启动应用后无需手动执行 {@code --artisan db:seed}。
 */
@Component
@Order(100)
public class DataInitializationRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializationRunner.class);

    private final DatabaseSeedCommand seedCommand;

    public DataInitializationRunner(DatabaseSeedCommand seedCommand) {
        this.seedCommand = seedCommand;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            // 不使用 count()（SQLite 下 gaarason 内部 Long 类型转换会失败），
            // 改用 first() 检查是否有任何记录
            boolean isEmpty = Admin.query().first() == null;
            if (isEmpty) {
                log.info("[init] admin 表为空，自动执行种子数据初始化...");
                seedCommand.handle();
            } else {
                log.info("[init] admin 表已有记录，跳过种子数据初始化");
            }
        } catch (Exception e) {
            log.error("[init] 种子数据自动初始化失败: {}", e.getMessage(), e);
        }
    }
}
