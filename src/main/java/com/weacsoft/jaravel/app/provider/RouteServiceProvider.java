package com.weacsoft.jaravel.app.provider;

import com.weacsoft.jaravel.vendor.core.provider.ServiceProvider;
import com.weacsoft.jaravel.vendor.route.Router;
import com.weacsoft.jaravel.routes.Api;
import com.weacsoft.jaravel.routes.Web;
import com.weacsoft.jaravel.app.http.middleware.AppTrimStrings;
import com.weacsoft.jaravel.app.http.middleware.AppConvertEmptyStringsToNull;
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
 * 中间件引用方式（和路由控制器引用方式一致，无需 {@code getBean} 或 {@code new}）：
 * <ul>
 *   <li>预定义中间件：继承后标注 {@code @MiddlewareAlias}，通过类对象引用（如 {@code AppTrimStrings.class}）</li>
 *   <li>自定义中间件：标注 {@code @MiddlewareAlias("alias")} 后通过别名引用（如 {@code "auth:admin"}）</li>
 * </ul>
 * 控制器通过字符串引用（如 {@code "AuthController::adminLogin"}），无需 {@code getBean} 获取控制器实例。
 */
@Configuration
public class RouteServiceProvider extends ServiceProvider {

    @Autowired
    private ApplicationContext context;

    @Bean
    public Router configureRoutes() {
        Router baseRouter = new Router();
        // 全局中间件（对齐 Laravel Kernel $middleware），直接在根 Router 上声明，
        // 所有路由通过 Router.getAllMiddlewares() 继承。
        // 通过类对象引用（中间件标注 @MiddlewareAlias 后由 classpath 扫描自动注册），
        // 无需 context.getBean() 获取实例。
        baseRouter.middleware(AppTrimStrings.class)
                  .middleware(AppConvertEmptyStringsToNull.class);
        // 加载 API 路由（控制器通过字符串引用，无需传入 context）
        context.getBean(Api.class).register(baseRouter);
        // 加载 Web 路由
        context.getBean(Web.class).register(baseRouter);
        return baseRouter;
    }
}
