package com.weacsoft.jaravel.app.http.middleware;

import com.weacsoft.jaravel.vendor.http.controller.request.Request;
import com.weacsoft.jaravel.vendor.http.controller.response.Response;
import com.weacsoft.jaravel.vendor.http.middleware.Middleware;
import com.weacsoft.jaravel.vendor.springboot.annotation.MiddlewareAlias;

/**
 * 路由权限中间件，对齐 Laravel {@code permission} 中间件别名。
 * <p>
 * 标注 {@code @MiddlewareAlias("permission")} 后，SpringBoot 启动时由
 * {@code MiddlewareAliasRegistrar} 自动扫描并注册到全局别名注册表，
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
 */
@MiddlewareAlias("permission")
public class PermissionMiddleware implements Middleware {

    private final RoutePermissionMiddleware adminMiddleware;
    private final UserRoutePermissionMiddleware userMiddleware;

    public PermissionMiddleware() {
        this.adminMiddleware = new RoutePermissionMiddleware();
        this.userMiddleware = new UserRoutePermissionMiddleware();
    }

    @Override
    public Response handle(Request request, NextFunction next, String... params) {
        String guard = (params != null && params.length > 0) ? params[0] : "admin";
        if ("admin".equals(guard)) {
            return adminMiddleware.handle(request, next, params);
        }
        return userMiddleware.handle(request, next, params);
    }
}
