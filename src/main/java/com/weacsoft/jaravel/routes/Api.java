package com.weacsoft.jaravel.routes;

import com.weacsoft.jaravel.app.http.controller.AuthController;
import com.weacsoft.jaravel.app.http.controller.AdminRbacController;
import com.weacsoft.jaravel.app.http.controller.PageController;
import com.weacsoft.jaravel.app.http.controller.PluginController;
import com.weacsoft.jaravel.app.http.controller.PluginRunController;
import com.weacsoft.jaravel.app.http.controller.TenantController;
import com.weacsoft.jaravel.app.http.controller.RemoteController;
import com.weacsoft.jaravel.app.http.controller.UserController;
import com.weacsoft.jaravel.app.http.controller.UserRbacController;
import com.weacsoft.jaravel.app.http.middleware.RoutePermissionMiddleware;
import com.weacsoft.jaravel.app.http.middleware.UserRoutePermissionMiddleware;
import com.weacsoft.jaravel.vendor.auth.middleware.Authenticate;
import com.weacsoft.jaravel.vendor.route.Route;
import com.weacsoft.jaravel.vendor.route.Router;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * API 路由定义，对齐 Laravel 的 {@code routes/api.php}。
 * <p>
 * 路由分为三组：
 * <ul>
 *   <li>公开路由：管理员/用户登录、注册、token 刷新、插件系统总览</li>
 *   <li>Admin 路由：管理员 RBAC + 插件管理 + 多租户 + 远程执行，使用 admin guard + RoutePermissionMiddleware</li>
 *   <li>User 路由：用户 RBAC + 插件执行，使用 api guard + UserRoutePermissionMiddleware</li>
 * </ul>
 */
@Component
public class Api {

    public void register(Router router, ApplicationContext context) {
        AuthController authController = context.getBean(AuthController.class);
        UserController userController = context.getBean(UserController.class);
        AdminRbacController rbacController = context.getBean(AdminRbacController.class);
        UserRbacController userRbac = context.getBean(UserRbacController.class);
        PluginController pluginController = context.getBean(PluginController.class);
        PluginRunController pluginRun = context.getBean(PluginRunController.class);
        TenantController tenantController = context.getBean(TenantController.class);
        RemoteController remoteController = context.getBean(RemoteController.class);
        PageController pageController = context.getBean(PageController.class);

        // 使用指定守卫名称创建中间件实例，确保 admin 路由用 admin guard，user 路由用 api guard
        RoutePermissionMiddleware adminRbacMiddleware = new RoutePermissionMiddleware("admin");
        UserRoutePermissionMiddleware userRbacMiddleware = new UserRoutePermissionMiddleware("api");

        // ===== 页面路由（jblade 模板渲染，无需认证） =====
        router.get("/", pageController::index);
        router.get("/admin", pageController::admin);
        router.get("/user", pageController::user);

        router.group(Map.of(Route.Group.PREFIX, "api"), api -> {
            // ===== 公开路由（无需认证） =====
            api.post("/auth/admin/login", authController::adminLogin);
            api.post("/auth/user/register", authController::register);
            api.post("/auth/user/login", authController::userLogin);
            api.post("/auth/refresh", authController::refresh);
            api.get("/plugin/overview", pluginRun::overview);

            // ===== Admin 路由（admin guard + admin 路由权限中间件） =====
            api.group(Map.of(), admin -> {
                admin.post("/auth/admin/logout", authController::adminLogout);
                admin.get("/auth/admin/me", authController::adminMe);

                // Admin RBAC 管理端点
                // 管理员 CRUD
                admin.get("/rbac/admins", rbacController::listAdmins);
                admin.post("/rbac/admins", rbacController::createAdmin);
                admin.get("/rbac/admins/{id}", rbacController::showAdmin);
                admin.put("/rbac/admins/{id}", rbacController::updateAdmin);
                admin.delete("/rbac/admins/{id}", rbacController::deleteAdmin);

                // 角色 CRUD
                admin.get("/rbac/roles", rbacController::listRoles);
                admin.post("/rbac/roles", rbacController::createRole);
                admin.get("/rbac/roles/{id}", rbacController::showRole);
                admin.put("/rbac/roles/{id}", rbacController::updateRole);
                admin.delete("/rbac/roles/{id}", rbacController::deleteRole);

                // 权限 CRUD
                admin.get("/rbac/permissions", rbacController::listPermissions);
                admin.post("/rbac/permissions", rbacController::createPermission);
                admin.get("/rbac/permissions/{id}", rbacController::showPermission);
                admin.put("/rbac/permissions/{id}", rbacController::updatePermission);
                admin.delete("/rbac/permissions/{id}", rbacController::deletePermission);

                // 管理员 ↔ 角色
                admin.get("/rbac/admins/{id}/roles", rbacController::adminRolesAll);
                admin.get("/rbac/admins/{id}/roles/assigned", rbacController::adminRolesAssigned);
                admin.post("/rbac/admins/{id}/roles", rbacController::assignRole);
                admin.delete("/rbac/admins/{id}/roles/{roleId}", rbacController::removeRole);

                // 角色 ↔ 权限
                admin.get("/rbac/roles/{id}/permissions", rbacController::rolePermissionsAll);
                admin.get("/rbac/roles/{id}/permissions/assigned", rbacController::rolePermissionsAssigned);
                admin.post("/rbac/roles/{id}/permissions", rbacController::assignPermission);
                admin.delete("/rbac/roles/{id}/permissions/{permissionId}", rbacController::removePermission);

                // 管理员 ↔ 权限（树形祖先授权 + 溯源）
                admin.get("/rbac/admins/{id}/permissions", rbacController::adminPermissionsAll);
                admin.get("/rbac/admins/{id}/permissions/assigned", rbacController::adminPermissionsAssigned);
                admin.get("/rbac/admins/{id}/check-permission/{permissionId}", rbacController::checkPermission);
                admin.get("/rbac/admins/{id}/check-role/{roleId}", rbacController::checkRole);
                admin.get("/rbac/admins/{id}/permissions/{permissionId}/grantors", rbacController::permissionGrantors);

                // 用户管理（Admin 管理平台用户）
                admin.get("/user-rbac/users", userRbac::listUsers);
                admin.post("/user-rbac/users", userRbac::createUser);
                admin.get("/user-rbac/users/{id}", userRbac::showUser);
                admin.put("/user-rbac/users/{id}", userRbac::updateUser);
                admin.delete("/user-rbac/users/{id}", userRbac::deleteUser);

                // 用户角色 CRUD
                admin.get("/user-rbac/roles", userRbac::listRoles);
                admin.post("/user-rbac/roles", userRbac::createRole);
                admin.get("/user-rbac/roles/{id}", userRbac::showRole);
                admin.put("/user-rbac/roles/{id}", userRbac::updateRole);
                admin.delete("/user-rbac/roles/{id}", userRbac::deleteRole);

                // 用户权限 CRUD
                admin.get("/user-rbac/permissions", userRbac::listPermissions);
                admin.post("/user-rbac/permissions", userRbac::createPermission);
                admin.get("/user-rbac/permissions/{id}", userRbac::showPermission);
                admin.put("/user-rbac/permissions/{id}", userRbac::updatePermission);
                admin.delete("/user-rbac/permissions/{id}", userRbac::deletePermission);

                // 用户 ↔ 角色
                admin.get("/user-rbac/users/{id}/roles", userRbac::userRolesAll);
                admin.get("/user-rbac/users/{id}/roles/assigned", userRbac::userRolesAssigned);
                admin.post("/user-rbac/users/{id}/roles", userRbac::assignRole);
                admin.delete("/user-rbac/users/{id}/roles/{roleId}", userRbac::removeRole);

                // 角色 ↔ 权限
                admin.get("/user-rbac/roles/{id}/permissions", userRbac::rolePermissionsAll);
                admin.get("/user-rbac/roles/{id}/permissions/assigned", userRbac::rolePermissionsAssigned);
                admin.post("/user-rbac/roles/{id}/permissions", userRbac::assignPermission);
                admin.delete("/user-rbac/roles/{id}/permissions/{permissionId}", userRbac::removePermission);

                // 用户 ↔ 权限（树形 + 路由 + 溯源）
                admin.get("/user-rbac/users/{id}/permissions", userRbac::userPermissionsAll);
                admin.get("/user-rbac/users/{id}/permissions/assigned", userRbac::userPermissionsAssigned);
                admin.get("/user-rbac/users/{id}/check-permission/{permissionId}", userRbac::checkPermission);
                admin.get("/user-rbac/users/{id}/check-role/{roleId}", userRbac::checkRole);
                admin.get("/user-rbac/users/{id}/check-route", userRbac::checkRoute);
                admin.get("/user-rbac/users/{id}/accessible-routes", userRbac::accessibleRoutes);
                admin.get("/user-rbac/users/{id}/permissions/{permissionId}/grantors", userRbac::permissionGrantors);

                // Jar 插件管理
                admin.get("/plugins/jar", pluginController::listJarPlugins);
                admin.post("/plugins/jar/upload", pluginController::uploadJarPlugin);
                admin.post("/plugins/jar/{pluginId}/enable", pluginController::enableJarPlugin);
                admin.post("/plugins/jar/{pluginId}/disable", pluginController::disableJarPlugin);
                admin.post("/plugins/jar/{pluginId}/routes", pluginController::registerRoute);
                admin.delete("/plugins/jar/{pluginId}/routes", pluginController::unregisterRoute);
                admin.get("/plugins/jar/{pluginId}/available-routes", pluginController::listAvailableJarRoutes);
                admin.post("/plugins/jar/{pluginId}/available-routes/register", pluginController::registerAvailableJarRoute);

                // Java 文件插件管理
                admin.get("/plugins/java", pluginController::listJavaPlugins);
                admin.post("/plugins/java/register", pluginController::registerJavaPlugin);
                admin.post("/plugins/java/{pluginId}/reload", pluginController::reloadJavaPlugin);
                admin.post("/plugins/java/reload-all", pluginController::reloadAllChanged);
                admin.post("/plugins/java/{pluginId}/disable", pluginController::disableJavaPlugin);
                admin.get("/plugins/java/{pluginId}/available-routes", pluginController::listAvailableJavaRoutes);
                admin.post("/plugins/java/{pluginId}/available-routes/register", pluginController::registerAvailableJavaRoute);

                // 多租户插件管理
                admin.get("/multi-tenant/status", tenantController::status);
                admin.get("/multi-tenant/naming-demo", tenantController::namingDemo);
                admin.get("/multi-tenant/tenants/{tenantId}/plugins", tenantController::listByTenant);
                admin.post("/multi-tenant/tenants/{tenantId}/plugins", tenantController::registerForTenant);
                admin.post("/multi-tenant/tenants/{tenantId}/plugins/{pluginId}/enable", tenantController::enableForTenant);
                admin.post("/multi-tenant/tenants/{tenantId}/plugins/{pluginId}/disable", tenantController::disableForTenant);

                // 远程插件执行管理
                admin.get("/remote/status", remoteController::status);
                admin.get("/remote/sub-servers", remoteController::listSubServers);
                admin.post("/remote/sub-servers", remoteController::registerSubServer);
                admin.delete("/remote/sub-servers/{subServerId}", remoteController::unregisterSubServer);
                admin.post("/remote/sub-servers/{subServerId}/connect", remoteController::connectSubServer);
                admin.post("/remote/sub-servers/{subServerId}/disconnect", remoteController::disconnectSubServer);
            }).middleware(new Authenticate("admin"), adminRbacMiddleware);

            // ===== User 路由（api guard + user 路由权限中间件） =====
            api.group(Map.of(), user -> {
                user.post("/auth/user/logout", authController::logout);
                user.get("/auth/user/me", authController::me);
                user.get("/users", userController::list);
                user.get("/users/{id}", userController::show);

                // 插件执行端点
                user.post("/plugin/java/run", pluginRun::runJava);
                user.get("/plugin/java/status", pluginRun::javaStatus);
                user.post("/plugin/jar/run", pluginRun::runJar);
                user.get("/plugin/jar/status", pluginRun::jarStatus);
            }).middleware(new Authenticate("api"), userRbacMiddleware);
        });
    }
}
