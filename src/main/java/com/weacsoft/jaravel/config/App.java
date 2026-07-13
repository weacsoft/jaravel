package com.weacsoft.jaravel.config;

import com.weacsoft.jaravel.config.database.DatabaseConfig;
import com.weacsoft.jaravel.config.view.ViewConfig;
import com.weacsoft.jaravel.config.wire.WireConfig;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * 应用中央配置（类似 Laravel config/app.php）。
 * <p>
 * 所有功能的启用/禁用在此显式控制。要启用某个功能，在 @Import 中添加对应的配置类；
 * 要禁用某个功能，从 @Import 中移除即可。
 * <p>
 * <h3>已启用的功能</h3>
 * <ul>
 *   <li>{@link ViewConfig} — 视图引擎（jblade BladeEngine）</li>
 *   <li>{@link DatabaseConfig} — 数据库（GaarasonDataSource + Druid）</li>
 *   <li>{@link WireConfig} — Wire 部分更新（Laravel Livewire 风格）</li>
 * </ul>
 * <p>
 * <h3>通过 application.yml 控制的功能（SpringBoot 自动装配）</h3>
 * <ul>
 *   <li>jaravel.captcha.enabled — 验证码模块</li>
 *   <li>jaravel.plugin-jar.enabled — JAR 插件系统</li>
 *   <li>jaravel.plugin-jar.multi-tenant.enabled — 多租户插件支持</li>
 *   <li>jaravel.plugin-java.enabled — Java 文件插件系统</li>
 *   <li>jaravel.migration.enabled — 数据库迁移</li>
 *   <li>jaravel.auth.enabled — 认证系统（JWT）</li>
 *   <li>jaravel.event.enabled — 事件系统</li>
 *   <li>jaravel.schedule.enabled — 定时任务</li>
 *   <li>jaravel.artisan.enabled — Artisan 命令行</li>
 * </ul>
 * <p>
 * 如需完全手动控制某个功能，可在 application.yml 中设 enabled=false，
 * 然后在此类中添加对应的 @Configuration 配置。
 */
@Configuration
@Import({
    ViewConfig.class,
    DatabaseConfig.class,
    WireConfig.class
})
public class App {
    // 所有功能通过 @Import 显式启用
    // 要禁用某功能，只需从 @Import 中移除对应的配置类
}
