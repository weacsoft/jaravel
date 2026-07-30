package com.weacsoft.jaravel.app.provider;

import com.weacsoft.jaravel.vendor.core.provider.ServiceProvider;
import com.weacsoft.jaravel.vendor.http.controller.ControllerRegistry;
import com.weacsoft.jaravel.vendor.route.Route;
import com.weacsoft.jaravel.vendor.route.Router;
import com.weacsoft.jaravel.routes.Api;
import com.weacsoft.jaravel.routes.Web;
import com.weacsoft.jaravel.app.http.middleware.AppTrimStrings;
import com.weacsoft.jaravel.app.http.middleware.AppConvertEmptyStringsToNull;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

/**
 * 路由服务提供者，对齐 Laravel 的 {@code App\Providers\RouteServiceProvider}。
 * <p>
 * 创建 {@code @Bean Router}，加载 API 路由（{@link Api}）与 Web 路由（{@link Web}），
 * 由 jaravel-springboot 自动装配转换为 Spring RouterFunction。
 * <p>
 * <b>控制器扫描范围</b>（对齐 Laravel RouteServiceProvider 指定路由加载范围）：
 * 通过 {@link ControllerRegistry#setScanBasePackages(String...)} 静态指定控制器所在包，
 * 框架通过 classpath 扫描发现控制器类并使用 AutowireCapableBeanFactory 自动注入依赖，
 * 此模式下控制器不需要标注 {@code @Component}。
 * <p>
 * <b>路由分组</b>（对齐 Laravel Kernel 的 api / web 中间件分组）：
 * <pre>
 * // Laravel:
 * Route::middleware('api')->prefix('api')->group(base_path('routes/api.php'));
 * Route::middleware('web')->group(base_path('routes/web.php'));
 *
 * // Jaravel:
 * Route.group(Map.of(Route.Group.MIDDLEWARE, new String[]{}), Api::register);
 * Route.group(Map.of(Route.Group.MIDDLEWARE, new String[]{}), Web::register);
 * </pre>
 * {@link Api} 和 {@link Web} 是纯静态类（非 Spring Bean），通过方法引用调用。
 * 每组的中间件数组即使为空也显式写出，方便后续扩展（如添加 {@code "throttle:api"}）。
 * <p>
 * <b>全局中间件</b>（对齐 Laravel Kernel {@code $middleware}）直接在根 {@link Router} 上声明，
 * 所有路由通过 {@code Router.getAllMiddlewares()} 继承。
 * <p>
 * <b>静态门面</b>（对齐 Laravel {@code Route::get()}）：通过 {@link Route#setRootRouter(Router)}
 * 初始化后，可在任意位置使用 {@code Route.get()}、{@code Route.group()} 等静态方法注册路由，
 * 无需传递 Router 实例。路由组闭包为无参 {@code Runnable}，系统通过 ThreadLocal 自动计算层级。
 */
@Configuration
public class RouteServiceProvider extends ServiceProvider {

    @Bean
    public Router configureRoutes() {
        // 指定控制器扫描范围（对齐 Laravel RouteServiceProvider 指定路由文件加载范围）
        // 框架将 classpath 扫描此包下所有实现了 Controllers 接口的类，
        // 使用 AutowireCapableBeanFactory 实例化并自动注入依赖。
        // 此模式下控制器不需要标注 @Component。
        ControllerRegistry.setScanBasePackages("com.weacsoft.jaravel.app.http.controller");

        Router baseRouter = new Router();

        // 初始化静态门面（对齐 Laravel Route Facade）
        // 初始化后可在 Api、Web 等路由定义类中使用 Route.get()、Route.group() 等静态方法
        Route.setRootRouter(baseRouter);

        // ===== 全局中间件（对齐 Laravel Kernel $middleware，所有路由继承） =====
        baseRouter.middleware(AppTrimStrings.class)
                  .middleware(AppConvertEmptyStringsToNull.class);

        // ===== API 路由组（对齐 Laravel Route::middleware('api')->group(base_path('routes/api.php'))） =====
        // Api 是纯静态类，通过方法引用调用，无需 Spring 容器获取
        // 中间件数组即使为空也显式写出，方便后续扩展（如添加 "throttle:api"）
        Route.group(Map.of(
                Route.Group.MIDDLEWARE, new String[]{}
        ), Api::register);

        // ===== Web 路由组（对齐 Laravel Route::middleware('web')->group(base_path('routes/web.php'))） =====
        // Web 是纯静态类，通过方法引用调用，无需 Spring 容器获取
        // 挂上 VerifyCsrfToken 中间件：POST/PUT/PATCH/DELETE 等请求需校验 CSRF token，
        // GET/HEAD/OPTIONS 及 VerifyCsrfToken 排除路由（如 api/、logout 等）自动放行。
        // token 由 VerifyCsrfToken 写入 HttpSession(csrf_token)，与模板 csrf_field() 共用同源值。
        Route.group(Map.of(
                Route.Group.MIDDLEWARE, new String[]{"VerifyCsrfToken"}
        ), Web::register);

        // 清理 ThreadLocal 上下文（防止线程池复用时泄漏）
        Route.clearContext();
        return baseRouter;
    }
}
