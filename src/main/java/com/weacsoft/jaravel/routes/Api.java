package com.weacsoft.jaravel.routes;

import com.weacsoft.jaravel.vendor.route.Route;
import com.weacsoft.jaravel.vendor.route.Routes;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * API 路由定义，对齐 Laravel 的 {@code routes/api.php}。
 * <p>
 * 全部使用 {@link Routes} 静态门面注册路由（对齐 Laravel {@code Route::get()} 静态调用风格），
 * 无需传递 {@code Router} 实例。路由组闭包为无参 {@code Runnable}，系统通过 ThreadLocal 自动计算层级。
 * <p>
 * 路由分为四组：
 * <ul>
 *   <li>公开路由：管理员/用户/Session 登录、注册、token 刷新、插件系统总览</li>
 *   <li>Admin 路由：管理员 RBAC + 插件管理 + 多租户 + 远程执行，使用 admin guard + 权限中间件</li>
 *   <li>User 路由：用户 RBAC + 插件执行，使用 api guard + 权限中间件</li>
 *   <li>Session 路由：Session 登出与信息，使用 web guard（Session 驱动 + cookie 存储）</li>
 * </ul>
 * <p>
 * <b>控制器引用</b>：通过字符串 {@code "ControllerName::method"} 引用控制器方法
 * （对齐 Laravel {@code Route::get('/users', 'UserController@index')}），无需 {@code getBean} 获取控制器实例。
 * 控制器由 SpringBoot 自动扫描注册到 {@code ControllerRegistry}，路由在首次请求时延迟解析。
 * <p>
 * <b>中间件引用</b>：通过字符串别名引用（对齐 Laravel {@code Route::middleware('auth:api')}）：
 * <ul>
 *   <li>{@code "auth:<guard>"} — 由 {@code AuthMiddleware} 处理，使用对应守卫进行认证</li>
 *   <li>{@code "permission:<guard>"} — 由 {@code PermissionMiddleware} 处理，
 *       admin 守卫走管理员 RBAC，其它守卫走用户 RBAC</li>
 * </ul>
 * 中间件标注 {@code @MiddlewareAlias} 后由 SpringBoot classpath 扫描自动注册（非 Spring Bean），无需手动 new。
 * <p>
 * <b>两种分组写法演示</b>：
 * <ul>
 *   <li><b>用法一</b>（Map 参数式）：{@code Routes.group(Map.of(Route.Group.PREFIX, "api"), () -> { ... })}
 *       — 对齐 Laravel {@code Route::group(['prefix' => 'api'], function () { ... })}</li>
 *   <li><b>用法二</b>（流式构建器）：{@code Routes.middleware("auth:api").prefix("api").group(() -> { ... })}
 *       — 对齐 Laravel {@code Route::middleware('api')->prefix('api')->group(function () { ... })}</li>
 * </ul>
 */
@Component
public class Api {

    /**
     * 注册 API 路由。使用 {@link Routes} 静态门面，无需传递 Router 实例。
     * <p>
     * 前提：{@code RouteServiceProvider} 中已调用 {@code Routes.setRootRouter(baseRouter)} 初始化静态门面。
     */
    public void register() {
        // 控制器通过字符串引用（对齐 Laravel Route::get('/users', 'UserController@index')），
        // 无需 context.getBean() 获取控制器实例：
        //   "AuthController::adminLogin" -> 从 ControllerRegistry 查找 AuthController，反射调用 adminLogin(Request)
        // 控制器由 SpringBoot 自动扫描注册，路由在首次请求时延迟解析。

        // ===== 页面路由（jblade 模板渲染，无需认证） =====
        Routes.get("/", "PageController::index");
        Routes.get("/admin", "PageController::admin");
        Routes.get("/user", "PageController::user");

        // ===== 用法一：Map 参数式分组（对齐 Laravel Route::group(['prefix' => 'api'], ...)） =====
        Routes.group(Map.of(Route.Group.PREFIX, "api"), () -> {
            // ===== 验证码接口（无需认证） =====
            Routes.get("/captcha/generate", "CaptchaController::generate");
            Routes.post("/captcha/generate", "CaptchaController::generate");
            Routes.post("/captcha/verify", "CaptchaController::verify");

            // ===== 公开路由（无需认证） =====
            // Session 认证（web guard，cookie 存储）
            Routes.post("/auth/session/login", "AuthController::sessionLogin");
            // JWT 认证
            Routes.post("/auth/admin/login", "AuthController::adminLogin");
            Routes.post("/auth/user/register", "AuthController::register");
            Routes.post("/auth/user/login", "AuthController::userLogin");
            Routes.post("/auth/refresh", "AuthController::refresh");
            Routes.get("/plugin/overview", "PluginRunController::overview");

            // ===== 事件与缓存演示路由（无需认证） =====
            Routes.get("/demo/cache", "EventCacheDemoController::demoMultiCache");
            Routes.get("/demo/event/user", "EventCacheDemoController::demoUserEvent");
            Routes.get("/demo/event/order", "EventCacheDemoController::demoOrderEvent");

            // ===== 用法一（嵌套）：Map 参数式分组 — Admin 路由 =====
            // 对齐 Laravel Route::group(['middleware' => ['auth:admin', 'permission:admin']], ...)
            Routes.group(Map.of(
                    Route.Group.MIDDLEWARE, new String[]{"auth:admin", "permission:admin"}
            ), () -> {
                Routes.post("/auth/admin/logout", "AuthController::adminLogout");
                Routes.get("/auth/admin/me", "AuthController::adminMe");

                // Admin RBAC 管理端点
                // 管理员 CRUD
                Routes.get("/rbac/admins", "AdminRbacController::listAdmins");
                Routes.post("/rbac/admins", "AdminRbacController::createAdmin");
                Routes.get("/rbac/admins/{id}", "AdminRbacController::showAdmin");
                Routes.put("/rbac/admins/{id}", "AdminRbacController::updateAdmin");
                Routes.delete("/rbac/admins/{id}", "AdminRbacController::deleteAdmin");

                // 角色 CRUD
                Routes.get("/rbac/roles", "AdminRbacController::listRoles");
                Routes.post("/rbac/roles", "AdminRbacController::createRole");
                Routes.get("/rbac/roles/{id}", "AdminRbacController::showRole");
                Routes.put("/rbac/roles/{id}", "AdminRbacController::updateRole");
                Routes.delete("/rbac/roles/{id}", "AdminRbacController::deleteRole");

                // 权限 CRUD
                Routes.get("/rbac/permissions", "AdminRbacController::listPermissions");
                Routes.post("/rbac/permissions", "AdminRbacController::createPermission");
                Routes.get("/rbac/permissions/{id}", "AdminRbacController::showPermission");
                Routes.put("/rbac/permissions/{id}", "AdminRbacController::updatePermission");
                Routes.delete("/rbac/permissions/{id}", "AdminRbacController::deletePermission");

                // 管理员 ↔ 角色
                Routes.get("/rbac/admins/{id}/roles", "AdminRbacController::adminRolesAll");
                Routes.get("/rbac/admins/{id}/roles/assigned", "AdminRbacController::adminRolesAssigned");
                Routes.post("/rbac/admins/{id}/roles", "AdminRbacController::assignRole");
                Routes.delete("/rbac/admins/{id}/roles/{roleId}", "AdminRbacController::removeRole");

                // 角色 ↔ 权限
                Routes.get("/rbac/roles/{id}/permissions", "AdminRbacController::rolePermissionsAll");
                Routes.get("/rbac/roles/{id}/permissions/assigned", "AdminRbacController::rolePermissionsAssigned");
                Routes.post("/rbac/roles/{id}/permissions", "AdminRbacController::assignPermission");
                Routes.delete("/rbac/roles/{id}/permissions/{permissionId}", "AdminRbacController::removePermission");

                // 管理员 ↔ 权限（树形祖先授权 + 溯源）
                Routes.get("/rbac/admins/{id}/permissions", "AdminRbacController::adminPermissionsAll");
                Routes.get("/rbac/admins/{id}/permissions/assigned", "AdminRbacController::adminPermissionsAssigned");
                Routes.get("/rbac/admins/{id}/check-permission/{permissionId}", "AdminRbacController::checkPermission");
                Routes.get("/rbac/admins/{id}/check-role/{roleId}", "AdminRbacController::checkRole");
                Routes.get("/rbac/admins/{id}/permissions/{permissionId}/grantors", "AdminRbacController::permissionGrantors");

                // 用户管理（Admin 管理平台用户）
                Routes.get("/user-rbac/users", "UserRbacController::listUsers");
                Routes.post("/user-rbac/users", "UserRbacController::createUser");
                Routes.get("/user-rbac/users/{id}", "UserRbacController::showUser");
                Routes.put("/user-rbac/users/{id}", "UserRbacController::updateUser");
                Routes.delete("/user-rbac/users/{id}", "UserRbacController::deleteUser");

                // 用户角色 CRUD
                Routes.get("/user-rbac/roles", "UserRbacController::listRoles");
                Routes.post("/user-rbac/roles", "UserRbacController::createRole");
                Routes.get("/user-rbac/roles/{id}", "UserRbacController::showRole");
                Routes.put("/user-rbac/roles/{id}", "UserRbacController::updateRole");
                Routes.delete("/user-rbac/roles/{id}", "UserRbacController::deleteRole");

                // 用户权限 CRUD
                Routes.get("/user-rbac/permissions", "UserRbacController::listPermissions");
                Routes.post("/user-rbac/permissions", "UserRbacController::createPermission");
                Routes.get("/user-rbac/permissions/{id}", "UserRbacController::showPermission");
                Routes.put("/user-rbac/permissions/{id}", "UserRbacController::updatePermission");
                Routes.delete("/user-rbac/permissions/{id}", "UserRbacController::deletePermission");

                // 用户 ↔ 角色
                Routes.get("/user-rbac/users/{id}/roles", "UserRbacController::userRolesAll");
                Routes.get("/user-rbac/users/{id}/roles/assigned", "UserRbacController::userRolesAssigned");
                Routes.post("/user-rbac/users/{id}/roles", "UserRbacController::assignRole");
                Routes.delete("/user-rbac/users/{id}/roles/{roleId}", "UserRbacController::removeRole");

                // 角色 ↔ 权限
                Routes.get("/user-rbac/roles/{id}/permissions", "UserRbacController::rolePermissionsAll");
                Routes.get("/user-rbac/roles/{id}/permissions/assigned", "UserRbacController::rolePermissionsAssigned");
                Routes.post("/user-rbac/roles/{id}/permissions", "UserRbacController::assignPermission");
                Routes.delete("/user-rbac/roles/{id}/permissions/{permissionId}", "UserRbacController::removePermission");

                // 用户 ↔ 权限（树形 + 路由 + 溯源）
                Routes.get("/user-rbac/users/{id}/permissions", "UserRbacController::userPermissionsAll");
                Routes.get("/user-rbac/users/{id}/permissions/assigned", "UserRbacController::userPermissionsAssigned");
                Routes.get("/user-rbac/users/{id}/check-permission/{permissionId}", "UserRbacController::checkPermission");
                Routes.get("/user-rbac/users/{id}/check-role/{roleId}", "UserRbacController::checkRole");
                Routes.get("/user-rbac/users/{id}/check-route", "UserRbacController::checkRoute");
                Routes.get("/user-rbac/users/{id}/accessible-routes", "UserRbacController::accessibleRoutes");
                Routes.get("/user-rbac/users/{id}/permissions/{permissionId}/grantors", "UserRbacController::permissionGrantors");

                // Jar 插件管理
                Routes.get("/plugins/jar", "PluginController::listJarPlugins");
                Routes.post("/plugins/jar/upload", "PluginController::uploadJarPlugin");
                Routes.post("/plugins/jar/{pluginId}/enable", "PluginController::enableJarPlugin");
                Routes.post("/plugins/jar/{pluginId}/disable", "PluginController::disableJarPlugin");
                Routes.post("/plugins/jar/{pluginId}/routes", "PluginController::registerRoute");
                Routes.delete("/plugins/jar/{pluginId}/routes", "PluginController::unregisterRoute");
                Routes.get("/plugins/jar/{pluginId}/available-routes", "PluginController::listAvailableJarRoutes");
                Routes.post("/plugins/jar/{pluginId}/available-routes/register", "PluginController::registerAvailableJarRoute");

                // Java 文件插件管理
                Routes.get("/plugins/java", "PluginController::listJavaPlugins");
                Routes.post("/plugins/java/register", "PluginController::registerJavaPlugin");
                Routes.post("/plugins/java/{pluginId}/reload", "PluginController::reloadJavaPlugin");
                Routes.post("/plugins/java/reload-all", "PluginController::reloadAllChanged");
                Routes.post("/plugins/java/{pluginId}/disable", "PluginController::disableJavaPlugin");
                Routes.get("/plugins/java/{pluginId}/available-routes", "PluginController::listAvailableJavaRoutes");
                Routes.post("/plugins/java/{pluginId}/available-routes/register", "PluginController::registerAvailableJavaRoute");

                // 多租户插件管理
                Routes.get("/multi-tenant/status", "TenantController::status");
                Routes.get("/multi-tenant/naming-demo", "TenantController::namingDemo");
                Routes.get("/multi-tenant/tenants/{tenantId}/plugins", "TenantController::listByTenant");
                Routes.post("/multi-tenant/tenants/{tenantId}/plugins", "TenantController::registerForTenant");
                Routes.post("/multi-tenant/tenants/{tenantId}/upload", "TenantController::uploadAndRegister");
                Routes.post("/multi-tenant/tenants/{tenantId}/plugins/{pluginId}/enable", "TenantController::enableForTenant");
                Routes.post("/multi-tenant/tenants/{tenantId}/plugins/{pluginId}/disable", "TenantController::disableForTenant");

                // 共享接口管理（全手动指定，反射调用）
                Routes.post("/multi-tenant/shared-interfaces/register", "TenantController::registerSharedInterface");
                Routes.post("/multi-tenant/shared-interfaces/{name}/invoke", "TenantController::invokeSharedInterface");
                Routes.get("/multi-tenant/shared-interfaces", "TenantController::listSharedInterfaces");

                // 远程插件执行管理
                Routes.get("/remote/status", "RemoteController::status");
                Routes.get("/remote/sub-servers", "RemoteController::listSubServers");
                Routes.post("/remote/sub-servers", "RemoteController::registerSubServer");
                Routes.delete("/remote/sub-servers/{subServerId}", "RemoteController::unregisterSubServer");
                Routes.post("/remote/sub-servers/{subServerId}/connect", "RemoteController::connectSubServer");
                Routes.post("/remote/sub-servers/{subServerId}/disconnect", "RemoteController::disconnectSubServer");
            });

            // ===== 用法二：流式构建器分组 — User 路由 =====
            // 对齐 Laravel Route::middleware('auth:api', 'permission:api')->group(function () { ... })
            Routes.middleware("auth:api", "permission:api").group(() -> {
                Routes.post("/auth/user/logout", "AuthController::logout");
                Routes.get("/auth/user/me", "AuthController::me");
                Routes.get("/users", "UserController::list");
                Routes.get("/users/{id}", "UserController::show");

                // 插件执行端点
                Routes.post("/plugin/java/run", "PluginRunController::runJava");
                Routes.get("/plugin/java/status", "PluginRunController::javaStatus");
                Routes.post("/plugin/jar/run", "PluginRunController::runJar");
                Routes.get("/plugin/jar/status", "PluginRunController::jarStatus");
            });

            // ===== 用法二（单个中间件）：流式构建器分组 — Session 路由 =====
            // 对齐 Laravel Route::middleware('auth:web')->group(function () { ... })
            Routes.middleware("auth:web").group(() -> {
                Routes.post("/auth/session/logout", "AuthController::sessionLogout");
                Routes.get("/auth/session/me", "AuthController::sessionMe");
            });
        });
    }
}
