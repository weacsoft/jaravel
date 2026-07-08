package com.weacsoft.jaravel.app.provider;

import com.weacsoft.jaravel.vendor.core.provider.ServiceProvider;
import com.weacsoft.jaravel.vendor.auth.middleware.Authenticate;
import com.weacsoft.jaravel.vendor.middleware.ConvertEmptyStringsToNull;
import com.weacsoft.jaravel.vendor.middleware.TrimStrings;
import com.weacsoft.jaravel.vendor.route.Router;
import com.weacsoft.jaravel.routes.Api;
import com.weacsoft.jaravel.routes.Web;
import com.weacsoft.jaravel.vendor.springboot.GlobalMiddlewareRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 路由服务提供者，对齐 Laravel 的 {@code App\Providers\RouteServiceProvider}。
 * <p>
 * 创建 {@code @Bean Router}，加载 API 路由（{@link Api}）与 Web 路由（{@link Web}），
 * 由 jaravel-springboot 自动装配转换为 Spring RouterFunction。
 * <p>
 * 在 {@link #boot()} 阶段统一注册系统全局中间件（对齐 Laravel Kernel $middleware），
 * 中间件以 Spring {@code @Component} 单例形式管理，无状态、可安全并发复用。
 * <p>
 * 替代原 {@code app/config/RouteConfig.java}，将路由定义拆分到 {@code routes/} 目录，
 * 与 Laravel 的 {@code routes/api.php} + {@code routes/web.php} 结构对齐。
 */
@Configuration
public class RouteServiceProvider extends ServiceProvider {

    @Autowired
    private ApplicationContext context;

    @Autowired
    private GlobalMiddlewareRegistry globalMiddlewareRegistry;

    @Bean
    public Router configureRoutes() {
        Router baseRouter = new Router();
        // 加载 API 路由
        context.getBean(Api.class).register(baseRouter, context);
        // 加载 Web 路由
        context.getBean(Web.class).register(baseRouter, context);
        return baseRouter;
    }

    /**
     * 注册认证中间件为 Spring Bean（vendor 框架的 Authenticate 未标注 @Component）。
     * <p>
     * 这样路由注册时可通过 {@code context.getBean(Authenticate.class)} 获取，
     * 无需每次 {@code new Authenticate()}。
     */
    @Bean
    public Authenticate authenticate() {
        return new Authenticate();
    }

    /**
     * 启动阶段：注册系统全局中间件。
     * <p>
     * 系统自带中间件（TrimStrings、ConvertEmptyStringsToNull）已标注 {@code @Component}，
     * 由 Spring 容器管理为无状态单例。此处通过 {@link GlobalMiddlewareRegistry#addByType(Class)}
     * 从容器中提取并注册到全局中间件栈，对齐 Laravel 在 RouteServiceProvider 统一注册中间件的做法。
     * <p>
     * 对于用户自定义的中间件，可使用 {@code @Component} 注册进 Spring，
     * 在路由中通过 {@code context.getBean(XxxMiddleware.class)} 提取，或在此处统一注册为全局中间件。
     */
    @Override
    public void boot() {
        // 系统全局中间件（对齐 Laravel Kernel $middleware）
        globalMiddlewareRegistry.addByType(TrimStrings.class);
        globalMiddlewareRegistry.addByType(ConvertEmptyStringsToNull.class);
    }
}
