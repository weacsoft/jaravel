package com.weacsoft.jaravel.app.http.middleware;

import com.weacsoft.jaravel.vendor.auth.middleware.Authenticate;
import com.weacsoft.jaravel.vendor.http.middleware.Middleware;
import com.weacsoft.jaravel.vendor.http.middleware.MiddlewareResolver;
import com.weacsoft.jaravel.vendor.springboot.annotation.MiddlewareAlias;

/**
 * 认证中间件别名解析器，对齐 Laravel {@code auth} 中间件别名。
 * <p>
 * 标注 {@code @MiddlewareAlias("auth")} 后，SpringBoot 启动时由
 * {@code GlobalMiddlewareRegistry} 自动扫描并注册到全局别名注册表，
 * 路由中即可通过字符串别名引用：
 * <pre>
 * router.group(Map.of(), admin -> { ... }).middleware("auth:admin");   // 使用 admin 守卫
 * router.group(Map.of(), user  -> { ... }).middleware("auth:api");     // 使用 api 守卫
 * </pre>
 * 别名表达式冒号后的参数会传入 {@link #resolve(String...)},
 * 据此构造对应守卫的 {@link Authenticate} 实例，替代路由中直接 {@code new Authenticate(guard)}。
 */
@MiddlewareAlias("auth")
public class AuthMiddlewareResolver implements MiddlewareResolver {
    @Override
    public Middleware resolve(String... params) {
        String guard = params.length > 0 ? params[0] : "api";
        return new Authenticate(guard);
    }
}
