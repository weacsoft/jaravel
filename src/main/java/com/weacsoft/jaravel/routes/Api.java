package com.weacsoft.jaravel.routes;

import com.weacsoft.jaravel.vendor.route.Route;
import com.weacsoft.jaravel.vendor.route.Router;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * API 路由定义，对齐 Laravel 的 {@code routes/api.php}。
 * <p>
 * 路由分为三组：
 * <ul>
 *   <li>公开路由：管理员/用户登录、注册、token 刷新、插件系统总览</li>
 *   <li>Admin 路由：管理员 RBAC + 插件管理 + 多租户 + 远程执行，使用 admin guard + 权限中间件</li>
 *   <li>User 路由：用户 RBAC + 插件执行，使用 api guard + 权限中间件</li>
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
 */
@Component
public class Api {

    public void register(Router router) {
        // 控制器通过字符串引用（对齐 Laravel Route::get('/users', 'UserController@index')），
        // 无需 context.getBean() 获取控制器实例：
        //   "AuthController::adminLogin" -> 从 ControllerRegistry 查找 AuthController，反射调用 adminLogin(Request)
        // 控制器由 SpringBoot 自动扫描注册，路由在首次请求时延迟解析。

        // ===== 页面路由（jblade 模板渲染，无需认证） =====
        router.get("/", "PageController::index");
        router.get("/admin", "PageController::admin");
        router.get("/user", "PageController::user");

        router.group(Map.of(Route.Group.PREFIX, "api"), api -> {
            // ===== 验证码接口（无需认证） =====
            api.get("/captcha/generate", "CaptchaController::generate");
            api.post("/captcha/generate", "CaptchaController::generate");
            api.post("/captcha/verify", "CaptchaController::verify");

            // ===== 公开路由（无需认证） =====
            api.post("/auth/admin/login", "AuthController::adminLogin");
            api.post("/auth/user/register", "AuthController::register");
            api.post("/auth/user/login", "AuthController::userLogin");
            api.post("/auth/refresh", "AuthController::refresh");
            api.get("/plugin/overview", "PluginRunController::overview");

            // ===== Admin 路由（admin guard + admin 路由权限中间件） =====
            api.group(Map.of(), admin -> {
                admin.post("/auth/admin/logout", "AuthController::adminLogout");
                admin.get("/auth/admin/me", "AuthController::adminMe");

                // Admin RBAC 管理端点
                // 管理员 CRUD
                admin.get("/rbac/admins", "AdminRbacController::listAdmins");
                admin.post("/rbac/admins", "AdminRbacController::createAdmin");
                admin.get("/rbac/admins/{id}", "AdminRbacController::showAdmin");
                admin.put("/rbac/admins/{id}", "AdminRbacController::updateAdmin");
                admin.delete("/rbac/admins/{id}", "AdminRbacController::deleteAdmin");

                // 角色 CRUD
                admin.get("/rbac/roles", "AdminRbacController::listRoles");
                admin.post("/rbac/roles", "AdminRbacController::createRole");
                admin.get("/rbac/roles/{id}", "AdminRbacController::showRole");
                admin.put("/rbac/roles/{id}", "AdminRbacController::updateRole");
                admin.delete("/rbac/roles/{id}", "AdminRbacController::deleteRole");

                // 权限 CRUD
                admin.get("/rbac/permissions", "AdminRbacController::listPermissions");
                admin.post("/rbac/permissions", "AdminRbacController::createPermission");
                admin.get("/rbac/permissions/{id}", "AdminRbacController::showPermission");
                admin.put("/rbac/permissions/{id}", "AdminRbacController::updatePermission");
                admin.delete("/rbac/permissions/{id}", "AdminRbacController::deletePermission");

                // 管理员 ↔ 角色
                admin.get("/rbac/admins/{id}/roles", "AdminRbacController::adminRolesAll");
                admin.get("/rbac/admins/{id}/roles/assigned", "AdminRbacController::adminRolesAssigned");
                admin.post("/rbac/admins/{id}/roles", "AdminRbacController::assignRole");
                admin.delete("/rbac/admins/{id}/roles/{roleId}", "AdminRbacController::removeRole");

                // 角色 ↔ 权限
                admin.get("/rbac/roles/{id}/permissions", "AdminRbacController::rolePermissionsAll");
                admin.get("/rbac/roles/{id}/permissions/assigned", "AdminRbacController::rolePermissionsAssigned");
                admin.post("/rbac/roles/{id}/permissions", "AdminRbacController::assignPermission");
                admin.delete("/rbac/roles/{id}/permissions/{permissionId}", "AdminRbacController::removePermission");

                // 管理员 ↔ 权限（树形祖先授权 + 溯源）
                admin.get("/rbac/admins/{id}/permissions", "AdminRbacController::adminPermissionsAll");
                admin.get("/rbac/admins/{id}/permissions/assigned", "AdminRbacController::adminPermissionsAssigned");
                admin.get("/rbac/admins/{id}/check-permission/{permissionId}", "AdminRbacController::checkPermission");
                admin.get("/rbac/admins/{id}/check-role/{roleId}", "AdminRbacController::checkRole");
                admin.get("/rbac/admins/{id}/permissions/{permissionId}/grantors", "AdminRbacController::permissionGrantors");

                // 用户管理（Admin 管理平台用户）
                admin.get("/user-rbac/users", "UserRbacController::listUsers");
                admin.post("/user-rbac/users", "UserRbacController::createUser");
                admin.get("/user-rbac/users/{id}", "UserRbacController::showUser");
                admin.put("/user-rbac/users/{id}", "UserRbacController::updateUser");
                admin.delete("/user-rbac/users/{id}", "UserRbacController::deleteUser");

                // 用户角色 CRUD
                admin.get("/user-rbac/roles", "UserRbacController::listRoles");
                admin.post("/user-rbac/roles", "UserRbacController::createRole");
                admin.get("/user-rbac/roles/{id}", "UserRbacController::showRole");
                admin.put("/user-rbac/roles/{id}", "UserRbacController::updateRole");
                admin.delete("/user-rbac/roles/{id}", "UserRbacController::deleteRole");

                // 用户权限 CRUD
                admin.get("/user-rbac/permissions", "UserRbacController::listPermissions");
                admin.post("/user-rbac/permissions", "UserRbacController::createPermission");
                admin.get("/user-rbac/permissions/{id}", "UserRbacController::showPermission");
                admin.put("/user-rbac/permissions/{id}", "UserRbacController::updatePermission");
                admin.delete("/user-rbac/permissions/{id}", "UserRbacController::deletePermission");

                // 用户 ↔ 角色
                admin.get("/user-rbac/users/{id}/roles", "UserRbacController::userRolesAll");
                admin.get("/user-rbac/users/{id}/roles/assigned", "UserRbacController::userRolesAssigned");
                admin.post("/user-rbac/users/{id}/roles", "UserRbacController::assignRole");
                admin.delete("/user-rbac/users/{id}/roles/{roleId}", "UserRbacController::removeRole");

                // 角色 ↔ 权限
                admin.get("/user-rbac/roles/{id}/permissions", "UserRbacController::rolePermissionsAll");
                admin.get("/user-rbac/roles/{id}/permissions/assigned", "UserRbacController::rolePermissionsAssigned");
                admin.post("/user-rbac/roles/{id}/permissions", "UserRbacController::assignPermission");
                admin.delete("/user-rbac/roles/{id}/permissions/{permissionId}", "UserRbacController::removePermission");

                // 用户 ↔ 权限（树形 + 路由 + 溯源）
                admin.get("/user-rbac/users/{id}/permissions", "UserRbacController::userPermissionsAll");
                admin.get("/user-rbac/users/{id}/permissions/assigned", "UserRbacController::userPermissionsAssigned");
                admin.get("/user-rbac/users/{id}/check-permission/{permissionId}", "UserRbacController::checkPermission");
                admin.get("/user-rbac/users/{id}/check-role/{roleId}", "UserRbacController::checkRole");
                admin.get("/user-rbac/users/{id}/check-route", "UserRbacController::checkRoute");
                admin.get("/user-rbac/users/{id}/accessible-routes", "UserRbacController::accessibleRoutes");
                admin.get("/user-rbac/users/{id}/permissions/{permissionId}/grantors", "UserRbacController::permissionGrantors");

                // Jar 插件管理
                admin.get("/plugins/jar", "PluginController::listJarPlugins");
                admin.post("/plugins/jar/upload", "PluginController::uploadJarPlugin");
                admin.post("/plugins/jar/{pluginId}/enable", "PluginController::enableJarPlugin");
                admin.post("/plugins/jar/{pluginId}/disable", "PluginController::disableJarPlugin");
                admin.post("/plugins/jar/{pluginId}/routes", "PluginController::registerRoute");
                admin.delete("/plugins/jar/{pluginId}/routes", "PluginController::unregisterRoute");
                admin.get("/plugins/jar/{pluginId}/available-routes", "PluginController::listAvailableJarRoutes");
                admin.post("/plugins/jar/{pluginId}/available-routes/register", "PluginController::registerAvailableJarRoute");

                // Java 文件插件管理
                admin.get("/plugins/java", "PluginController::listJavaPlugins");
                admin.post("/plugins/java/register", "PluginController::registerJavaPlugin");
                admin.post("/plugins/java/{pluginId}/reload", "PluginController::reloadJavaPlugin");
                admin.post("/plugins/java/reload-all", "PluginController::reloadAllChanged");
                admin.post("/plugins/java/{pluginId}/disable", "PluginController::disableJavaPlugin");
                admin.get("/plugins/java/{pluginId}/available-routes", "PluginController::listAvailableJavaRoutes");
                admin.post("/plugins/java/{pluginId}/available-routes/register", "PluginController::registerAvailableJavaRoute");

                // 多租户插件管理
                admin.get("/multi-tenant/status", "TenantController::status");
                admin.get("/multi-tenant/naming-demo", "TenantController::namingDemo");
                admin.get("/multi-tenant/tenants/{tenantId}/plugins", "TenantController::listByTenant");
                admin.post("/multi-tenant/tenants/{tenantId}/plugins", "TenantController::registerForTenant");
                admin.post("/multi-tenant/tenants/{tenantId}/upload", "TenantController::uploadAndRegister");
                admin.post("/multi-tenant/tenants/{tenantId}/plugins/{pluginId}/enable", "TenantController::enableForTenant");
                admin.post("/multi-tenant/tenants/{tenantId}/plugins/{pluginId}/disable", "TenantController::disableForTenant");

                // 共享接口管理（全手动指定，反射调用）
                admin.post("/multi-tenant/shared-interfaces/register", "TenantController::registerSharedInterface");
                admin.post("/multi-tenant/shared-interfaces/{name}/invoke", "TenantController::invokeSharedInterface");
                admin.get("/multi-tenant/shared-interfaces", "TenantController::listSharedInterfaces");

                // 远程插件执行管理
                admin.get("/remote/status", "RemoteController::status");
                admin.get("/remote/sub-servers", "RemoteController::listSubServers");
                admin.post("/remote/sub-servers", "RemoteController::registerSubServer");
                admin.delete("/remote/sub-servers/{subServerId}", "RemoteController::unregisterSubServer");
                admin.post("/remote/sub-servers/{subServerId}/connect", "RemoteController::connectSubServer");
                admin.post("/remote/sub-servers/{subServerId}/disconnect", "RemoteController::disconnectSubServer");
            }).middleware("auth:admin", "permission:admin");

            // ===== User 路由（api guard + user 路由权限中间件） =====
            api.group(Map.of(), user -> {
                user.post("/auth/user/logout", "AuthController::logout");
                user.get("/auth/user/me", "AuthController::me");
                user.get("/users", "UserController::list");
                user.get("/users/{id}", "UserController::show");

                // 插件执行端点
                user.post("/plugin/java/run", "PluginRunController::runJava");
                user.get("/plugin/java/status", "PluginRunController::javaStatus");
                user.post("/plugin/jar/run", "PluginRunController::runJar");
                user.get("/plugin/jar/status", "PluginRunController::jarStatus");
            }).middleware("auth:api", "permission:api");
        });
    }
}
