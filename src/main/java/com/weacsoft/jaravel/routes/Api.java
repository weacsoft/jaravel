package com.weacsoft.jaravel.routes;

import com.weacsoft.jaravel.vendor.route.Route;

import java.util.Map;

/**
 * API 路由定义，对齐 Laravel 的 {@code routes/api.php}。
 * <p>
 * 纯静态类，不注册为 Spring Bean。由 {@code RouteServiceProvider} 通过
 * {@code Route.group(Map.of(Route.Group.MIDDLEWARE, new String[]{}), Api::register)}
 * 以方法引用形式调用，对齐 Laravel {@code Route::middleware('api')->group(base_path('routes/api.php'))}。
 * <p>
 * 全部使用 {@link Route} 静态门面注册路由（对齐 Laravel {@code Route::get()} 静态调用风格），
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
 *   <li><b>用法一</b>（Map 参数式）：{@code Route.group(Map.of(Route.Group.PREFIX, "api"), () -> { ... })}
 *       — 对齐 Laravel {@code Route::group(['prefix' => 'api'], function () { ... })}</li>
 *   <li><b>用法二</b>（流式构建器）：{@code Route.middleware("auth:api").prefix("api").group(() -> { ... })}
 *       — 对齐 Laravel {@code Route::middleware('api')->prefix('api')->group(function () { ... })}</li>
 * </ul>
 */
public class Api {

    /**
     * 注册 API 路由。使用 {@link Route} 静态门面，无需传递 Router 实例。
     * <p>
     * 由 {@code RouteServiceProvider} 以方法引用 {@code Api::register} 调用，
     * 外层已通过 {@code Route.group(Map.of(Route.Group.MIDDLEWARE, new String[]{}), Api::register)}
     * 提供 API 组的中间件数组（对齐 Laravel {@code Route::middleware('api')->group(...)}）。
     */
    public static void register() {
        // 控制器通过字符串引用（对齐 Laravel Route::get('/users', 'UserController@index')），
        // 无需 context.getBean() 获取控制器实例：
        //   "AuthController::adminLogin" -> 从 ControllerRegistry 查找 AuthController，反射调用 adminLogin(Request)
        // 控制器由 SpringBoot 自动扫描注册，路由在首次请求时延迟解析。

        // ===== 页面路由（jblade 模板渲染，无需认证） =====
        Route.get("/", "PageController::index");
        Route.get("/admin", "PageController::admin");
        Route.get("/user", "PageController::user");

        // ===== 用法一：Map 参数式分组（对齐 Laravel Route::group(['prefix' => 'api'], ...)） =====
        Route.group(Map.of(Route.Group.PREFIX, "api"), () -> {
            // ===== 验证码接口（无需认证） =====
            Route.get("/captcha/generate", "CaptchaController::generate");
            Route.post("/captcha/generate", "CaptchaController::generate");
            Route.post("/captcha/verify", "CaptchaController::verify");

            // ===== 公开路由（无需认证） =====
            // Session 认证（web guard，cookie 存储）
            Route.post("/auth/session/login", "AuthController::sessionLogin");
            // JWT 认证
            Route.post("/auth/admin/login", "AuthController::adminLogin");
            Route.post("/auth/user/register", "AuthController::register");
            Route.post("/auth/user/login", "AuthController::userLogin");
            Route.post("/auth/refresh", "AuthController::refresh");
            Route.get("/plugin/overview", "PluginRunController::overview");

            // ===== 事件与缓存演示路由（无需认证） =====
            Route.get("/demo/cache", "EventCacheDemoController::demoMultiCache");
            Route.get("/demo/event/user", "EventCacheDemoController::demoUserEvent");
            Route.get("/demo/event/order", "EventCacheDemoController::demoOrderEvent");

            // ===== 用法一（嵌套）：Map 参数式分组 — Admin 路由 =====
            // 对齐 Laravel Route::group(['middleware' => ['auth:admin', 'permission:admin']], ...)
            Route.group(Map.of(
                    Route.Group.MIDDLEWARE, new String[]{"auth:admin", "permission:admin"}
            ), () -> {
                Route.post("/auth/admin/logout", "AuthController::adminLogout");
                Route.get("/auth/admin/me", "AuthController::adminMe");

                // Admin RBAC 管理端点
                // 管理员 CRUD
                Route.get("/rbac/admins", "AdminRbacController::listAdmins");
                Route.post("/rbac/admins", "AdminRbacController::createAdmin");
                Route.get("/rbac/admins/{id}", "AdminRbacController::showAdmin");
                Route.put("/rbac/admins/{id}", "AdminRbacController::updateAdmin");
                Route.delete("/rbac/admins/{id}", "AdminRbacController::deleteAdmin");

                // 角色 CRUD
                Route.get("/rbac/roles", "AdminRbacController::listRoles");
                Route.post("/rbac/roles", "AdminRbacController::createRole");
                Route.get("/rbac/roles/{id}", "AdminRbacController::showRole");
                Route.put("/rbac/roles/{id}", "AdminRbacController::updateRole");
                Route.delete("/rbac/roles/{id}", "AdminRbacController::deleteRole");

                // 权限 CRUD
                Route.get("/rbac/permissions", "AdminRbacController::listPermissions");
                Route.post("/rbac/permissions", "AdminRbacController::createPermission");
                Route.get("/rbac/permissions/{id}", "AdminRbacController::showPermission");
                Route.put("/rbac/permissions/{id}", "AdminRbacController::updatePermission");
                Route.delete("/rbac/permissions/{id}", "AdminRbacController::deletePermission");

                // 管理员 ↔ 角色
                Route.get("/rbac/admins/{id}/roles", "AdminRbacController::adminRolesAll");
                Route.get("/rbac/admins/{id}/roles/assigned", "AdminRbacController::adminRolesAssigned");
                Route.post("/rbac/admins/{id}/roles", "AdminRbacController::assignRole");
                Route.delete("/rbac/admins/{id}/roles/{roleId}", "AdminRbacController::removeRole");

                // 角色 ↔ 权限
                Route.get("/rbac/roles/{id}/permissions", "AdminRbacController::rolePermissionsAll");
                Route.get("/rbac/roles/{id}/permissions/assigned", "AdminRbacController::rolePermissionsAssigned");
                Route.post("/rbac/roles/{id}/permissions", "AdminRbacController::assignPermission");
                Route.delete("/rbac/roles/{id}/permissions/{permissionId}", "AdminRbacController::removePermission");

                // 管理员 ↔ 权限（树形祖先授权 + 溯源）
                Route.get("/rbac/admins/{id}/permissions", "AdminRbacController::adminPermissionsAll");
                Route.get("/rbac/admins/{id}/permissions/assigned", "AdminRbacController::adminPermissionsAssigned");
                Route.get("/rbac/admins/{id}/check-permission/{permissionId}", "AdminRbacController::checkPermission");
                Route.get("/rbac/admins/{id}/check-role/{roleId}", "AdminRbacController::checkRole");
                Route.get("/rbac/admins/{id}/permissions/{permissionId}/grantors", "AdminRbacController::permissionGrantors");

                // 用户管理（Admin 管理平台用户）
                Route.get("/user-rbac/users", "UserRbacController::listUsers");
                Route.post("/user-rbac/users", "UserRbacController::createUser");
                Route.get("/user-rbac/users/{id}", "UserRbacController::showUser");
                Route.put("/user-rbac/users/{id}", "UserRbacController::updateUser");
                Route.delete("/user-rbac/users/{id}", "UserRbacController::deleteUser");

                // 用户角色 CRUD
                Route.get("/user-rbac/roles", "UserRbacController::listRoles");
                Route.post("/user-rbac/roles", "UserRbacController::createRole");
                Route.get("/user-rbac/roles/{id}", "UserRbacController::showRole");
                Route.put("/user-rbac/roles/{id}", "UserRbacController::updateRole");
                Route.delete("/user-rbac/roles/{id}", "UserRbacController::deleteRole");

                // 用户权限 CRUD
                Route.get("/user-rbac/permissions", "UserRbacController::listPermissions");
                Route.post("/user-rbac/permissions", "UserRbacController::createPermission");
                Route.get("/user-rbac/permissions/{id}", "UserRbacController::showPermission");
                Route.put("/user-rbac/permissions/{id}", "UserRbacController::updatePermission");
                Route.delete("/user-rbac/permissions/{id}", "UserRbacController::deletePermission");

                // 用户 ↔ 角色
                Route.get("/user-rbac/users/{id}/roles", "UserRbacController::userRolesAll");
                Route.get("/user-rbac/users/{id}/roles/assigned", "UserRbacController::userRolesAssigned");
                Route.post("/user-rbac/users/{id}/roles", "UserRbacController::assignRole");
                Route.delete("/user-rbac/users/{id}/roles/{roleId}", "UserRbacController::removeRole");

                // 角色 ↔ 权限
                Route.get("/user-rbac/roles/{id}/permissions", "UserRbacController::rolePermissionsAll");
                Route.get("/user-rbac/roles/{id}/permissions/assigned", "UserRbacController::rolePermissionsAssigned");
                Route.post("/user-rbac/roles/{id}/permissions", "UserRbacController::assignPermission");
                Route.delete("/user-rbac/roles/{id}/permissions/{permissionId}", "UserRbacController::removePermission");

                // 用户 ↔ 权限（树形 + 路由 + 溯源）
                Route.get("/user-rbac/users/{id}/permissions", "UserRbacController::userPermissionsAll");
                Route.get("/user-rbac/users/{id}/permissions/assigned", "UserRbacController::userPermissionsAssigned");
                Route.get("/user-rbac/users/{id}/check-permission/{permissionId}", "UserRbacController::checkPermission");
                Route.get("/user-rbac/users/{id}/check-role/{roleId}", "UserRbacController::checkRole");
                Route.get("/user-rbac/users/{id}/check-route", "UserRbacController::checkRoute");
                Route.get("/user-rbac/users/{id}/accessible-routes", "UserRbacController::accessibleRoutes");
                Route.get("/user-rbac/users/{id}/permissions/{permissionId}/grantors", "UserRbacController::permissionGrantors");

                // Jar 插件管理
                Route.get("/plugins/jar", "PluginController::listJarPlugins");
                Route.post("/plugins/jar/upload", "PluginController::uploadJarPlugin");
                Route.post("/plugins/jar/{pluginId}/enable", "PluginController::enableJarPlugin");
                Route.post("/plugins/jar/{pluginId}/disable", "PluginController::disableJarPlugin");
                Route.post("/plugins/jar/{pluginId}/routes", "PluginController::registerRoute");
                Route.delete("/plugins/jar/{pluginId}/routes", "PluginController::unregisterRoute");
                Route.get("/plugins/jar/{pluginId}/available-routes", "PluginController::listAvailableJarRoutes");
                Route.post("/plugins/jar/{pluginId}/available-routes/register", "PluginController::registerAvailableJarRoute");

                // Java 文件插件管理
                Route.get("/plugins/java", "PluginController::listJavaPlugins");
                Route.post("/plugins/java/register", "PluginController::registerJavaPlugin");
                Route.post("/plugins/java/{pluginId}/reload", "PluginController::reloadJavaPlugin");
                Route.post("/plugins/java/reload-all", "PluginController::reloadAllChanged");
                Route.post("/plugins/java/{pluginId}/disable", "PluginController::disableJavaPlugin");
                Route.get("/plugins/java/{pluginId}/available-routes", "PluginController::listAvailableJavaRoutes");
                Route.post("/plugins/java/{pluginId}/available-routes/register", "PluginController::registerAvailableJavaRoute");

                // 多租户插件管理
                Route.get("/multi-tenant/status", "TenantController::status");
                Route.get("/multi-tenant/naming-demo", "TenantController::namingDemo");
                Route.get("/multi-tenant/tenants/{tenantId}/plugins", "TenantController::listByTenant");
                Route.post("/multi-tenant/tenants/{tenantId}/plugins", "TenantController::registerForTenant");
                Route.post("/multi-tenant/tenants/{tenantId}/upload", "TenantController::uploadAndRegister");
                Route.post("/multi-tenant/tenants/{tenantId}/plugins/{pluginId}/enable", "TenantController::enableForTenant");
                Route.post("/multi-tenant/tenants/{tenantId}/plugins/{pluginId}/disable", "TenantController::disableForTenant");

                // 共享接口管理（全手动指定，反射调用）
                Route.post("/multi-tenant/shared-interfaces/register", "TenantController::registerSharedInterface");
                Route.post("/multi-tenant/shared-interfaces/{name}/invoke", "TenantController::invokeSharedInterface");
                Route.get("/multi-tenant/shared-interfaces", "TenantController::listSharedInterfaces");

                // 远程插件执行管理
                Route.get("/remote/status", "RemoteController::status");
                Route.get("/remote/sub-servers", "RemoteController::listSubServers");
                Route.post("/remote/sub-servers", "RemoteController::registerSubServer");
                Route.delete("/remote/sub-servers/{subServerId}", "RemoteController::unregisterSubServer");
                Route.post("/remote/sub-servers/{subServerId}/connect", "RemoteController::connectSubServer");
                Route.post("/remote/sub-servers/{subServerId}/disconnect", "RemoteController::disconnectSubServer");
            });

            // ===== 用法二：流式构建器分组 — User 路由 =====
            // 对齐 Laravel Route::middleware('auth:api', 'permission:api')->group(function () { ... })
            Route.middleware("auth:api", "permission:api").group(() -> {
                Route.post("/auth/user/logout", "AuthController::logout");
                Route.get("/auth/user/me", "AuthController::me");
                Route.get("/users", "UserController::list");
                Route.get("/users/{id}", "UserController::show");

                // 插件执行端点
                Route.post("/plugin/java/run", "PluginRunController::runJava");
                Route.get("/plugin/java/status", "PluginRunController::javaStatus");
                Route.post("/plugin/jar/run", "PluginRunController::runJar");
                Route.get("/plugin/jar/status", "PluginRunController::jarStatus");
            });

            // ===== 用法二（单个中间件）：流式构建器分组 — Session 路由 =====
            // 对齐 Laravel Route::middleware('auth:web')->group(function () { ... })
            Route.middleware("auth:web").group(() -> {
                Route.post("/auth/session/logout", "AuthController::sessionLogout");
                Route.get("/auth/session/me", "AuthController::sessionMe");
            });
        });
    }
}
