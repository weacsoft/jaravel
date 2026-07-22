package com.weacsoft.jaravel.app.provider;

import com.weacsoft.jaravel.vendor.core.provider.ServiceProvider;
import com.weacsoft.jaravel.vendor.http.middleware.ConvertEmptyStringsToNull;
import com.weacsoft.jaravel.vendor.http.middleware.TrimStrings;
import com.weacsoft.jaravel.vendor.route.Router;
import com.weacsoft.jaravel.routes.Api;
import com.weacsoft.jaravel.routes.Web;
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
 * 系统全局中间件（对齐 Laravel Kernel $middleware）直接在根 {@link Router} 上声明，
 * 所有路由通过 {@code Router.getAllMiddlewares()} 继承，无需单独的全局中间件注册器。
 * <p>
 * 替代原 {@code app/config/RouteConfig.java}，将路由定义拆分到 {@code routes/} 目录，
 * 与 Laravel 的 {@code routes/api.php} + {@code routes/web.php} 结构对齐。
 * <p>
 * 路由级中间件通过 {@code @MiddlewareAlias} 别名引用（如 {@code "auth:admin"}、
 * {@code "permission:api"}），中间件由 SpringBoot 自动扫描注册，无需在此手动注册 Bean。
 */
@Configuration
public class RouteServiceProvider extends ServiceProvider {

    @Autowired
    private ApplicationContext context;

    @Bean
    public Router configureRoutes() {
        Router baseRouter = new Router();
        // 全局中间件（对齐 Laravel Kernel $middleware），直接在根 Router 上声明，
        // 所有路由通过 Router.getAllMiddlewares() 继承
        baseRouter.middleware(
                context.getBean(TrimStrings.class),
                context.getBean(ConvertEmptyStringsToNull.class)
        );
        // 加载 API 路由
        context.getBean(Api.class).register(baseRouter, context);
        // 加载 Web 路由
        context.getBean(Web.class).register(baseRouter, context);
        return baseRouter;
    }
}
