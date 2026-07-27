package com.weacsoft.jaravel.config;

import com.weacsoft.jaravel.vendor.auth.AuthManager;
import com.weacsoft.jaravel.vendor.auth.contract.SessionStore;
import com.weacsoft.jaravel.vendor.cache.CacheManager;
import com.weacsoft.jaravel.vendor.core.Application;
import com.weacsoft.jaravel.vendor.core.config.ConfigRepository;
import com.weacsoft.jaravel.vendor.event.Dispatcher;
import com.weacsoft.jaravel.vendor.route.Router;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * 应用中央配置（对齐 Laravel 的 {@code config/app.php}）。
 * <p>
 * 继承 {@link Application}，既作为 Spring {@code @Configuration} 控制功能开关，
 * 又作为应用容器提供 typed 服务访问器，替代 Facade 静态代理模式。
 *
 * <h3>功能开关</h3>
 * 所有功能的启用/禁用在 {@code @Import} 中显式控制。要启用某个功能，添加对应的配置类；
 * 要禁用某个功能，移除即可。
 * <ul>
 *   <li>{@link ViewConfig} — 视图引擎（jblade BladeEngine）+ 静态资源</li>
 *   <li>{@link DatabaseConfig} — 数据库（GaarasonDataSource + Druid）</li>
 *   <li>{@link WireConfig} — Wire 部分更新（Laravel Livewire 风格）</li>
 *   <li>{@link AuthConfig} — 认证配置（守卫注册，对齐 config/auth.php）</li>
 *   <li>{@link SessionConfig} — Session 存储配置（对齐 config/session.php）</li>
 * </ul>
 *
 * <h3>typed 服务访问</h3>
 * 通过 {@code App.app()} 获取实例后，可直接链式访问各类服务：
 * <pre>
 * // 在应用代码中（需要强转为 AppConfig，或在应用模块自定义 App 类）
 * AppConfig app = (AppConfig) App.app();
 * app.auth().check();
 * app.cache().get("key");
 * app.config().string("app.name");
 * app.event().dispatch(new UserRegistered(1L));
 * app.session().get("user_id");
 * app.router().getAllRoutes();
 *
 * // 通用方式（任何模块均可使用，无需强转）
 * AuthManager auth = App.app().make(AuthManager.class);
 *
 * // 自定义服务注册
 * App.app().singleton("myService", () -> new MyService());
 * MyService svc = App.app().make("myService");
 * </pre>
 *
 * <h3>自定义扩展</h3>
 * 继承本类可添加更多 typed 访问器，同时保持 {@code @Configuration} 功能：
 * <pre>
 * &#64;Configuration
 * public class MyAppConfig extends AppConfig {
 *     public MyService myService() { return make(MyService.class); }
 * }
 * </pre>
 *
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
 */
@Configuration
@Import({
    ViewConfig.class,
    DatabaseConfig.class,
    WireConfig.class,
    AuthConfig.class,
    SessionConfig.class
})
public class AppConfig extends Application {

    // ==================== 功能开关 ====================
    // 所有功能通过 @Import 显式启用
    // 要禁用某功能，只需从 @Import 中移除对应的配置类

    // ==================== typed 服务访问器 ====================

    /**
     * 获取认证管理器（对齐 Laravel {@code app('auth')}）。
     *
     * @return AuthManager 实例
     */
    public AuthManager auth() {
        return make(AuthManager.class);
    }

    /**
     * 获取缓存管理器（对齐 Laravel {@code app('cache')}）。
     *
     * @return CacheManager 实例
     */
    public CacheManager cache() {
        return make(CacheManager.class);
    }

    /**
     * 获取配置仓库（对齐 Laravel {@code app('config')}）。
     *
     * @return ConfigRepository 实例
     */
    public ConfigRepository config() {
        return make(ConfigRepository.class);
    }

    /**
     * 获取事件分发器（对齐 Laravel {@code app('events')}）。
     *
     * @return Dispatcher 实例
     */
    public Dispatcher event() {
        return make(Dispatcher.class);
    }

    /**
     * 获取 Session 存储器（对齐 Laravel {@code app('session')}）。
     *
     * @return SessionStore 实例
     */
    public SessionStore session() {
        return make(SessionStore.class);
    }

    /**
     * 获取路由器（对齐 Laravel {@code app('router')}）。
     *
     * @return Router 实例
     */
    public Router router() {
        return make(Router.class);
    }
}
