package com.weacsoft.jaravel.app.http.middleware;

import com.weacsoft.jaravel.vendor.auth.middleware.Authenticate;
import com.weacsoft.jaravel.vendor.http.controller.request.Request;
import com.weacsoft.jaravel.vendor.http.controller.response.Response;
import com.weacsoft.jaravel.vendor.http.middleware.Middleware;
import com.weacsoft.jaravel.vendor.springboot.annotation.MiddlewareAlias;

/**
 * 认证中间件，对齐 Laravel {@code auth} 中间件别名。
 * <p>
 * 标注 {@code @MiddlewareAlias("auth")} 后，SpringBoot 启动时由
 * {@code SpringBootRouteAutoConfiguration} 自动扫描并注册到全局别名注册表，
 * 路由中即可通过字符串别名引用：
 * <pre>
 * router.group(Map.of(), admin -> { ... }).middleware("auth:admin");   // 使用 admin 守卫
 * router.group(Map.of(), user  -> { ... }).middleware("auth:api");     // 使用 api 守卫
 * </pre>
 * 别名表达式冒号后的参数会传入 {@link #handle} 的 {@code params}，
 * {@code params[0]} 作为守卫名称，委托给 {@link Authenticate} 执行实际认证逻辑。
 */
@MiddlewareAlias("auth")
public class AuthMiddleware implements Middleware {

    private final Authenticate authenticate;

    public AuthMiddleware() {
        this.authenticate = new Authenticate();
    }

    public AuthMiddleware(String loginPath) {
        this.authenticate = new Authenticate(null, loginPath);
    }

    @Override
    public Response handle(Request request, NextFunction next, String... params) {
        // params[0] 作为守卫名称（来自别名表达式如 auth:api），委托给 Authenticate 处理
        return authenticate.handle(request, next, params);
    }
}
