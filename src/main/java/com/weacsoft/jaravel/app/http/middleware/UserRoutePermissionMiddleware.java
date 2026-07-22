package com.weacsoft.jaravel.app.http.middleware;

import com.weacsoft.jaravel.app.service.UserRolePermissionService;
import com.weacsoft.jaravel.vendor.auth.facade.Auth;
import com.weacsoft.jaravel.vendor.http.controller.request.Request;
import com.weacsoft.jaravel.vendor.http.controller.response.Response;
import com.weacsoft.jaravel.vendor.http.controller.response.ResponseBuilder;
import com.weacsoft.jaravel.vendor.http.middleware.Middleware;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 用户路由权限中间件：在中间件层判断「当前用户有没有访问这个功能的权限」。
 * <p>
 * 由 {@link PermissionMiddleware} 在构造器中 {@code new} 创建，非 Spring Bean，
 * 不需要 {@code @Component} 或 {@code @MiddlewareAlias}。
 * <p>
 * 与 {@link RoutePermissionMiddleware}（Admin 版）对称，面向普通用户。
 * 用于多租户场景下的 Java/Jar 插件运行权限控制。
 * <p>
 * 工作流程：
 * <ol>
 *   <li>从 {@link Auth} 获取当前登录用户的 ID；</li>
 *   <li>从 {@code HttpServletRequest} 获取当前请求路径；</li>
 *   <li>调用 {@link UserRolePermissionService#userCanAccessRoute(Long, String)} 判断；</li>
 *   <li>有权放行，无权返回 403。</li>
 * </ol>
 *
 * <h3>使用方式</h3>
 * <pre>
 * // 由 PermissionMiddleware 内部创建，路由中通过 "permission:api" 别名引用：
 * router.group(Map.of(), user -> { ... }).middleware("permission:api");
 * </pre>
 *
 * @see UserRolePermissionService#userCanAccessRoute(Long, String)
 * @see RoutePermissionMiddleware Admin 版路由权限中间件
 */
public class UserRoutePermissionMiddleware implements Middleware {

    private static final Logger log = LoggerFactory.getLogger(UserRoutePermissionMiddleware.class);

    private final String guard;

    public UserRoutePermissionMiddleware() {
        this(null);
    }

    public UserRoutePermissionMiddleware(String guard) {
        this.guard = guard;
    }

    @Override
    public Response handle(Request request, NextFunction next, String... params) {
        // 优先使用 params 中的守卫（来自别名表达式如 permission:api），其次使用构造器指定的守卫
        String effectiveGuard = (params != null && params.length > 0) ? params[0] : this.guard;
        boolean authenticated = (effectiveGuard != null && !effectiveGuard.isEmpty())
                ? Auth.guard(effectiveGuard).check()
                : Auth.check();
        if (!authenticated) {
            return ResponseBuilder.error(401, "未登录");
        }

        Object idObj = (effectiveGuard != null && !effectiveGuard.isEmpty())
                ? Auth.guard(effectiveGuard).id()
                : Auth.id();
        Long userId = toLong(idObj);
        if (userId == null) {
            return ResponseBuilder.error(403, "无法获取用户信息，拒绝访问");
        }

        String path = null;
        if (request.getRequest() != null) {
            path = request.getRequest().getRequestURI();
        }
        if (path == null || path.isEmpty()) {
            return ResponseBuilder.error(403, "无法解析请求路径，拒绝访问");
        }

        if (UserRolePermissionService.userCanAccessRoute(userId, path)) {
            log.debug("[UserRoutePermission] 放行 userId={} path={}", userId, path);
            return next.apply(request);
        }

        log.info("[UserRoutePermission] 拒绝访问 userId={} path={}", userId, path);
        return ResponseBuilder.error(403, "无权访问此功能");
    }

    private Long toLong(Object obj) {
        if (obj == null) return null;
        if (obj instanceof Long) return (Long) obj;
        if (obj instanceof Number) return ((Number) obj).longValue();
        try {
            return Long.valueOf(obj.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
