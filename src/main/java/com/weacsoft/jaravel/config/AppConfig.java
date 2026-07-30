package com.weacsoft.jaravel.config;

import com.weacsoft.jaravel.vendor.auth.AuthManager;
import com.weacsoft.jaravel.vendor.auth.contract.SessionStore;
import com.weacsoft.jaravel.vendor.cache.CacheManager;
import com.weacsoft.jaravel.vendor.core.Application;
import com.weacsoft.jaravel.vendor.core.SpringContext;
import com.weacsoft.jaravel.vendor.core.config.ConfigRepository;
import com.weacsoft.jaravel.vendor.event.Dispatcher;
import com.weacsoft.jaravel.vendor.route.RouteHelper;
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
 *
 * <h3>免强转访问</h3>
 * 通过 {@link #app()} 静态方法直接返回 {@code AppConfig} 类型，无需强转：
 * <pre>
 * AppConfig.app().auth().check();
 * AppConfig.app().cache().get("key");
 * AppConfig.app().config().string("app.name");
 * AppConfig.app().event().dispatch(new UserRegistered(1L));
 * AppConfig.app().session().get("user_id");
 * AppConfig.app().router().getAllRoutes();
 * </pre>
 *
 * <h3>自动注册（对齐 Laravel aliases 数组）</h3>
 * static 块中集中注册常用服务别名，{@code make("auth")} 即可解析：
 * <pre>
 * AppConfig.app().make("auth");     // -> AuthManager
 * AppConfig.app().make("cache");    // -> CacheManager
 * AppConfig.app().make("config");   // -> ConfigRepository
 * </pre>
 *
 * <h3>自定义服务注册</h3>
 * <pre>
 * AppConfig.app().singleton("myService", () -> new MyService());
 * MyService svc = AppConfig.app().make("myService");
 * </pre>
 *
 * <h3>自定义扩展</h3>
 * <pre>
 * &#64;Configuration
 * public class MyAppConfig extends AppConfig {
 *     public MyService myService() { return make(MyService.class); }
 * }
 * </pre>
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

    // ==================== 自动注册（对齐 Laravel aliases 数组） ====================

    static {
        // 常用服务自动注册，make("auth") 即可解析
        registerDefaultBinding("auth", AuthManager.class);
        registerDefaultBinding("cache", CacheManager.class);
        registerDefaultBinding("config", ConfigRepository.class);
        registerDefaultBinding("event", Dispatcher.class);
        registerDefaultBinding("session", SessionStore.class);
        registerDefaultBinding("router", Router.class);
    }

    // ==================== 免强转静态入口 ====================

    /**
     * 获取应用容器实例（返回具体类型，免强转）。
     * <p>
     * 对齐 Laravel {@code app()}，但返回 {@code AppConfig} 而非基类，
     * 因此可以直接链式调用 typed 访问器：
     * <pre>
     * AppConfig.app().auth().check();
     * AppConfig.app().cache().get("key");
     * </pre>
     *
     * @return AppConfig 实例
     */
    public static AppConfig app() {
        return SpringContext.bean(AppConfig.class);
    }

    // ==================== typed 服务访问器 ====================

    /**
     * 获取认证管理器（对齐 Laravel {@code app('auth')}）。
     */
    public AuthManager auth() {
        return make(AuthManager.class);
    }

    /**
     * 获取缓存管理器（对齐 Laravel {@code app('cache')}）。
     */
    public CacheManager cache() {
        return make(CacheManager.class);
    }

    /**
     * 获取配置仓库（对齐 Laravel {@code app('config')}）。
     */
    public ConfigRepository config() {
        return make(ConfigRepository.class);
    }

    /**
     * 获取事件分发器（对齐 Laravel {@code app('events')}）。
     */
    public Dispatcher event() {
        return make(Dispatcher.class);
    }

    /**
     * 获取 Session 存储器（对齐 Laravel {@code app('session')}）。
     */
    public SessionStore session() {
        return make(SessionStore.class);
    }

    /**
     * 获取路由器（对齐 Laravel {@code app('router')}）。
     */
    public Router router() {
        return make(Router.class);
    }

    /**
     * 路由辅助门面（对齐 Laravel 全局辅助函数 {@code route()} / {@code url()}）。
     * <p>
     * 返回 {@link RouteHelper} 共享实例，提供两套语义，与 Laravel 高度一致：
     * <pre>
     * // route(别名) —— 按路由别名解析 URL（对齐 Laravel route('admin.login')）
     * String url = AppConfig.app().route().route("admin.login");
     *
     * // url(路径) —— 单纯生成 URL，不校验是否存在（对齐 Laravel url('/admin/login')）
     * String url2 = AppConfig.app().route().url("admin/login");
     * </pre>
     * 也可静态调用 {@code RouteHelper.route("admin.login")} / {@code RouteHelper.url("/admin/login")}。
     */
    public RouteHelper route() {
        return RouteHelper.instance();
    }
}
