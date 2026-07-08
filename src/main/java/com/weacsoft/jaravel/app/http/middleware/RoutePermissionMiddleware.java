package com.weacsoft.jaravel.app.http.middleware;

import com.weacsoft.jaravel.app.service.AdminRolePermissionService;
import com.weacsoft.jaravel.vendor.auth.facade.Auth;
import com.weacsoft.jaravel.vendor.http.request.Request;
import com.weacsoft.jaravel.vendor.http.response.Response;
import com.weacsoft.jaravel.vendor.http.response.ResponseBuilder;
import com.weacsoft.jaravel.vendor.middleware.Middleware;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 路由权限中间件：在中间件层判断「当前管理员有没有访问这个功能的权限」。
 * <p>
 * 标注 {@code @Component} 后由 Spring 容器管理为单例，路由注册时通过
 * {@code context.getBean(RoutePermissionMiddleware.class)} 获取，无需每次 {@code new}。
 * <p>
 * 工作流程：
 * <ol>
 *   <li>从 {@link Auth} 获取当前登录管理员的 ID（支持指定守卫）；</li>
 *   <li>从 {@code HttpServletRequest} 获取当前请求的路由路径；</li>
 *   <li>调用 {@link AdminRolePermissionService#adminCanAccessRoute(Long, String)}
 *       判断管理员是否有权访问该路由（默认拒绝 + 树形祖先授权 + 路由模式匹配）；</li>
 *   <li>有权则放行 {@code next.apply(request)}，无权则返回 403。</li>
 * </ol>
 *
 * <h3>路由匹配规则</h3>
 * 权限的 {@code route} 字段支持两种匹配方式（不使用正则，路由以 {@code /} 开头）：
 * <ul>
 *   <li><b>全匹配</b>：{@code /admin/user} 仅匹配 {@code /admin/user}</li>
 *   <li><b>通配匹配</b>：{@code /admin/*} 匹配 {@code /admin} 下所有路由</li>
 * </ul>
 *
 * <h3>使用方式（Spring Bean 模式，推荐）</h3>
 * <pre>
 * // 中间件已标注 @Component，从容器获取即可，无需 new
 * RoutePermissionMiddleware rbacMiddleware = context.getBean(RoutePermissionMiddleware.class);
 * Authenticate authMiddleware = context.getBean(Authenticate.class);
 *
 * router.group(Map.of(Route.Group.PREFIX, "admin"), admin -> {
 *     admin.middleware(authMiddleware, rbacMiddleware);
 *     admin.get("/dashboard", controller::dashboard);
 *     admin.get("/user/list", controller::userList);
 * });
 * </pre>
 *
 * <h3>配合权限配置</h3>
 * 需要在 {@code admin_permissions} 表中为权限设置 {@code route} 字段：
 * <pre>
 * // 创建带路由的权限
 * AdminRolePermissionService.createPermission("用户管理", "user", null, "/admin/user/*", "用户管理模块");
 * AdminRolePermissionService.createPermission("仪表盘", "dashboard", null, "/admin/dashboard", "首页仪表盘");
 *
 * // 将权限分配给角色，再将角色分配给管理员
 * AdminRolePermissionService.assignPermissionToRole(roleId, permissionId);
 * AdminRolePermissionService.assignRoleToAdmin(adminId, roleId);
 * </pre>
 * 管理员访问 {@code /admin/user/create} 时，中间件会匹配到 {@code /admin/user/*} 权限，允许通过。
 *
 * @see AdminRolePermissionService#adminCanAccessRoute(Long, String)
 * @see AdminRolePermissionService#routeMatches(String, String)
 * @see com.weacsoft.jaravel.vendor.auth.middleware.Authenticate
 */
@Component
public class RoutePermissionMiddleware implements Middleware {

    private static final Logger log = LoggerFactory.getLogger(RoutePermissionMiddleware.class);

    private final String guard;

    /** 使用默认守卫 */
    public RoutePermissionMiddleware() {
        this(null);
    }

    /**
     * 指定守卫名称，对齐 Laravel {@code auth:api} 语法。
     *
     * @param guard 守卫名称，{@code null} 使用默认守卫
     */
    public RoutePermissionMiddleware(String guard) {
        this.guard = guard;
    }

    @Override
    public Response handle(Request request, NextFunction next) {
        // 1. 检查登录状态
        boolean authenticated = (guard != null && !guard.isEmpty())
                ? Auth.guard(guard).check()
                : Auth.check();
        if (!authenticated) {
            return ResponseBuilder.error(401, "未登录");
        }

        // 2. 获取管理员 ID
        Object idObj = (guard != null && !guard.isEmpty())
                ? Auth.guard(guard).id()
                : Auth.id();
        Long adminId = toLong(idObj);
        if (adminId == null) {
            log.warn("[RoutePermission] 无法获取管理员 ID，拒绝访问");
            return ResponseBuilder.error(403, "无法获取管理员信息，拒绝访问");
        }

        // 3. 获取当前请求路径
        String path = null;
        if (request.getRequest() != null) {
            path = request.getRequest().getRequestURI();
        }
        if (path == null || path.isEmpty()) {
            log.warn("[RoutePermission] 无法获取请求路径，拒绝访问，adminId={}", adminId);
            return ResponseBuilder.error(403, "无法解析请求路径，拒绝访问");
        }

        // 4. 路由权限判断（默认拒绝 + 树形祖先授权 + 路由模式匹配）
        if (AdminRolePermissionService.adminCanAccessRoute(adminId, path)) {
            log.debug("[RoutePermission] 放行 adminId={} path={}", adminId, path);
            return next.apply(request);
        }

        log.info("[RoutePermission] 拒绝访问 adminId={} path={}", adminId, path);
        return ResponseBuilder.error(403, "无权访问此功能");
    }

    /**
     * 将 Auth.id() 返回的 Object 转换为 Long，兼容 Long / Integer / String / Number 类型。
     */
    private Long toLong(Object obj) {
        if (obj == null) {
            return null;
        }
        if (obj instanceof Long) {
            return (Long) obj;
        }
        if (obj instanceof Number) {
            return ((Number) obj).longValue();
        }
        try {
            return Long.valueOf(obj.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
