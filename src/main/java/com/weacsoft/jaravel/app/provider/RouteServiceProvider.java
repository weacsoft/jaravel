package com.weacsoft.jaravel.app.provider;

import com.weacsoft.jaravel.vendor.core.provider.ServiceProvider;
import com.weacsoft.jaravel.vendor.http.controller.ControllerRegistry;
import com.weacsoft.jaravel.vendor.http.middleware.ConvertEmptyStringsToNull;
import com.weacsoft.jaravel.vendor.http.middleware.TrimStrings;
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
 * <b>控制器扫描范围</b>（对齐 Laravel RouteServiceProvider 指定路由加载范围）：
 * 通过 {@link ControllerRegistry#setScanBasePackages(String...)} 静态指定控制器所在包，
 * 框架通过 classpath 扫描发现控制器类并使用 AutowireCapableBeanFactory 自动注入依赖，
 * 此模式下控制器不需要标注 {@code @Component}。
 * <p>
 * <b>全局中间件</b>（对齐 Laravel Kernel $middleware）直接在根 {@link Router} 上声明，
 * 所有路由通过 {@code Router.getAllMiddlewares()} 继承。
 * 预定义中间件支持两种使用方式：
 * <ul>
 *   <li><b>继承式</b>（推荐）：继承预定义中间件，覆盖 {@code protected} 方法自定义参数，
 *       标注 {@code @MiddlewareAlias} 后由 classpath 扫描自动注册，通过类对象引用</li>
 *   <li><b>手动实例化</b>：直接 {@code new} 并通过匿名类覆盖方法，或使用默认配置直接 {@code new}，
 *       通过 {@code router.middleware(instance)} 直接添加</li>
 * </ul>
 */
@Configuration
public class RouteServiceProvider extends ServiceProvider {

    @Autowired
    private ApplicationContext context;

    @Bean
    public Router configureRoutes() {
        // 指定控制器扫描范围（对齐 Laravel RouteServiceProvider 指定路由文件加载范围）
        // 框架将 classpath 扫描此包下所有实现了 Controllers 接口的类，
        // 使用 AutowireCapableBeanFactory 实例化并自动注入依赖。
        // 此模式下控制器不需要标注 @Component。
        ControllerRegistry.setScanBasePackages("com.weacsoft.jaravel.app.http.controller");

        Router baseRouter = new Router();

        // 全局中间件演示两种使用方式：

        // 方式一：继承式（推荐）— 继承预定义中间件，覆盖 protected 方法自定义参数，
        //         标注 @MiddlewareAlias 后由 classpath 扫描自动注册，通过类对象引用
        baseRouter.middleware(AppTrimStrings.class)
                  .middleware(AppConvertEmptyStringsToNull.class);

        // 方式二：手动实例化 — 直接 new 并通过匿名类覆盖方法自定义参数，
        //         或使用默认配置直接 new，通过 router.middleware(instance) 直接添加
        // baseRouter.middleware(new TrimStrings() {
        //     @Override
        //     protected String[] except() {
        //         return new String[]{"password", "password_confirmation"};
        //     }
        // });
        // baseRouter.middleware(new ConvertEmptyStringsToNull()); // 使用默认排除列表

        // 加载 API 路由（控制器通过字符串引用，无需传入 context）
        context.getBean(Api.class).register(baseRouter);
        // 加载 Web 路由
        context.getBean(Web.class).register(baseRouter);
        return baseRouter;
    }
}
