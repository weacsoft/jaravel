package com.weacsoft.jaravel.app.http.middleware;

import com.weacsoft.jaravel.vendor.http.middleware.Middleware;
import com.weacsoft.jaravel.vendor.http.middleware.MiddlewareResolver;
import com.weacsoft.jaravel.vendor.springboot.annotation.MiddlewareAlias;

/**
 * 路由权限中间件别名解析器，对齐 Laravel {@code permission} 中间件别名。
 * <p>
 * 标注 {@code @MiddlewareAlias("permission")} 后，SpringBoot 启动时由
 * {@code GlobalMiddlewareRegistry} 自动扫描并注册到全局别名注册表，
 * 路由中即可通过字符串别名引用：
 * <pre>
 * router.group(Map.of(), admin -> { ... }).middleware("permission:admin"); // 管理员 RBAC
 * router.group(Map.of(), user  -> { ... }).middleware("permission:api");   // 用户 RBAC
 * </pre>
 * 根据守卫参数选择对应的权限中间件：
 * <ul>
 *   <li>{@code admin} 守卫 → {@link RoutePermissionMiddleware}（Admin RBAC）</li>
 *   <li>其它守卫（如 {@code api}） → {@link UserRoutePermissionMiddleware}（User RBAC）</li>
 * </ul>
 * 替代路由中直接 {@code new RoutePermissionMiddleware(guard)} /
 * {@code new UserRoutePermissionMiddleware(guard)} 的写法。
 */
@MiddlewareAlias("permission")
public class PermissionMiddlewareResolver implements MiddlewareResolver {
    @Override
    public Middleware resolve(String... params) {
        String guard = params.length > 0 ? params[0] : "admin";
        if ("admin".equals(guard)) {
            return new RoutePermissionMiddleware(guard);
        }
        return new UserRoutePermissionMiddleware(guard);
    }
}
