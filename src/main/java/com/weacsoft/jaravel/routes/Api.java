package com.weacsoft.jaravel.routes;

import com.weacsoft.jaravel.app.http.controller.AuthController;
import com.weacsoft.jaravel.app.http.controller.AdminRbacController;
import com.weacsoft.jaravel.app.http.controller.PluginDemoController;
import com.weacsoft.jaravel.app.http.controller.MultiTenantDemoController;
import com.weacsoft.jaravel.app.http.controller.RemoteDemoController;
import com.weacsoft.jaravel.app.http.controller.UserController;
import com.weacsoft.jaravel.app.http.controller.WelcomeController;
import com.weacsoft.jaravel.app.job.NotificationEvent;
import com.weacsoft.jaravel.vendor.artisan.ArtisanApplication;
import com.weacsoft.jaravel.vendor.artisan.ArtisanCommand;
import com.weacsoft.jaravel.vendor.auth.facade.Auth;
import com.weacsoft.jaravel.vendor.auth.middleware.Authenticate;
import com.weacsoft.jaravel.vendor.event.Dispatcher;
import com.weacsoft.jaravel.vendor.http.response.ResponseBuilder;
import com.weacsoft.jaravel.vendor.route.Route;
import com.weacsoft.jaravel.vendor.route.Router;
import com.weacsoft.jaravel.vendor.schedule.Schedule;
import com.weacsoft.jaravel.vendor.schedule.ScheduledTask;
import com.weacsoft.jaravel.vendor.wechat.AccessTokenManager;
import com.weacsoft.jaravel.vendor.wechat.WechatProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * API 路由定义，对齐 Laravel 的 {@code routes/api.php}。
 * <p>
 * 由 {@code RouteServiceProvider} 调用 {@link #register(Router, ApplicationContext)}
 * 将全部 API 路由注册到 Router，统一添加 {@code api} 前缀。
 * <p>
 * 包含：公开路由、需认证路由、多 guard 测试路由、多数据库测试路由、中间件顺序测试路由、Request 格式测试路由。
 * <p>
 * <b>中间件使用规范：</b>
 * <ul>
 *   <li>系统全局中间件（TrimStrings、ConvertEmptyStringsToNull 等）已标注 {@code @Component}，
 *       在 {@code RouteServiceProvider.boot()} 中统一注册为全局中间件，无需在路由中手动添加。</li>
 *   <li>需要构造参数的不可变中间件（如 {@code new Authenticate("api")}、{@code new OrderTestMiddleware("A")}），
 *       直接使用 {@code new} 创建即可——它们无状态、不可变，可安全地在并发请求间复用。</li>
 *   <li>用户自定义的无参数中间件，可标注 {@code @Component} 后通过
 *       {@code context.getBean(XxxMiddleware.class)} 从 Spring 容器获取。</li>
 * </ul>
 */
@Component
public class Api {

    public void register(Router router, ApplicationContext context) {
        AuthController authController = context.getBean(AuthController.class);
        UserController userController = context.getBean(UserController.class);
        WelcomeController welcomeController = context.getBean(WelcomeController.class);

        router.group(Map.of(Route.Group.PREFIX, "api"), api -> {
            // 公开路由
            api.get("/hello", welcomeController::hello);
            api.post("/auth/register", authController::register);
            api.post("/auth/login", authController::login);
            // 多 guard 登录：通过 guard 参数指定 api(JWT) / web(Session)
            api.post("/auth/login-via-guard", authController::loginViaGuard);
            // JWT refresh token 换取新 access token
            api.post("/auth/refresh", authController::refresh);

            // 需要认证的路由（默认使用 api guard）
            // Authenticate 是不可变中间件，使用 new 创建即可（无状态、可安全复用）
            api.group(Map.of(), auth -> {
                auth.post("/auth/logout", authController::logout);
                // 登出指定 guard（路径参数指定 api / web）
                auth.post("/auth/logout/{guard}", authController::logoutViaGuard);
                auth.get("/auth/me", authController::me);
                // 获取当前用户信息（含自动续期的新 token）
                auth.get("/auth/profile", authController::profile);
                auth.get("/users", userController::list);
                auth.get("/users/{id}", userController::show);
            }).middleware(new Authenticate());

            // 多 guard 测试路由
            // 检查指定 guard 的登录态（路径参数指定 api / web）
            api.get("/guard/{guard}", authController::checkGuard);

            // 显式使用 api guard（JWT 驱动）
            api.get("/guard/api", request -> ResponseBuilder.json(Map.of(
                "guard", "api",
                "message", "API guard 认证通过",
                "user", Auth.guard("api").user() != null ? Auth.guard("api").user().toString() : null
            ))).middleware(new Authenticate("api"));

            // 显式使用 web guard（Session 驱动）
            api.get("/guard/web", request -> ResponseBuilder.json(Map.of(
                "guard", "web",
                "message", "Web guard 认证通过"
            ))).middleware(new Authenticate("web"));

            // 多数据库测试路由
            api.get("/products", userController::products);
            api.get("/products/{id}", userController::product);

            // 中间件顺序测试路由
            // OrderTestMiddleware 需要名称参数，使用 new 创建（不可变、无状态）
            api.get("/middleware-test", request -> {
                java.util.List<String> order = request.getAttribute("_middleware_order", java.util.List.class);
                java.util.Map<String, Object> result = new java.util.LinkedHashMap<>();
                result.put("message", "中间件链执行完成");
                result.put("middleware_order", order);
                return ResponseBuilder.json(result);
            }).middleware(new com.weacsoft.jaravel.app.http.middleware.OrderTestMiddleware("A"))
              .middleware(new com.weacsoft.jaravel.app.http.middleware.OrderTestMiddleware("B"))
              .middleware(new com.weacsoft.jaravel.app.http.middleware.OrderTestMiddleware("C"));

            // Request 格式测试路由
            api.post("/request-test", welcomeController::requestTest);

            // Cache 功能演示路由
            api.get("/cache-demo", welcomeController::cacheDemo);

            // View (Blade模板) 演示路由
            api.get("/view-demo", welcomeController::viewDemo);

            // ===== 插件系统演示路由 =====
            // 注意：生产环境不应暴露这些 REST API，此处仅用于演示
            PluginDemoController pluginController = context.getBean(PluginDemoController.class);
            MultiTenantDemoController multiTenantController = context.getBean(MultiTenantDemoController.class);
            RemoteDemoController remoteController = context.getBean(RemoteDemoController.class);

            // JAR 插件管理
            api.get("/plugins/jar", pluginController::listJarPlugins);
            api.post("/plugins/jar/upload", pluginController::uploadJarPlugin);
            api.post("/plugins/jar/{pluginId}/enable", pluginController::enableJarPlugin);
            api.post("/plugins/jar/{pluginId}/disable", pluginController::disableJarPlugin);
            api.post("/plugins/jar/{pluginId}/routes", pluginController::registerRoute);
            api.delete("/plugins/jar/{pluginId}/routes", pluginController::unregisterRoute);

            // Java 文件插件管理
            api.get("/plugins/java", pluginController::listJavaPlugins);
            api.post("/plugins/java/register", pluginController::registerJavaPlugin);
            api.post("/plugins/java/{pluginId}/reload", pluginController::reloadJavaPlugin);
            api.post("/plugins/java/reload-all", pluginController::reloadAllChanged);
            api.post("/plugins/java/{pluginId}/disable", pluginController::disableJavaPlugin);

            // 可用路由（manual-register 模式）
            api.get("/plugins/jar/{pluginId}/available-routes", pluginController::listAvailableJarRoutes);
            api.post("/plugins/jar/{pluginId}/available-routes/register", pluginController::registerAvailableJarRoute);
            api.get("/plugins/java/{pluginId}/available-routes", pluginController::listAvailableJavaRoutes);
            api.post("/plugins/java/{pluginId}/available-routes/register", pluginController::registerAvailableJavaRoute);

            // ===== 多租户插件演示路由 =====
            api.get("/multi-tenant/status", multiTenantController::status);
            api.get("/multi-tenant/naming-demo", multiTenantController::namingDemo);
            api.get("/multi-tenant/tenants/{tenantId}/plugins", multiTenantController::listByTenant);
            api.post("/multi-tenant/tenants/{tenantId}/plugins", multiTenantController::registerForTenant);
            api.post("/multi-tenant/tenants/{tenantId}/plugins/{pluginId}/enable", multiTenantController::enableForTenant);
            api.post("/multi-tenant/tenants/{tenantId}/plugins/{pluginId}/disable", multiTenantController::disableForTenant);

            // ===== 远程插件执行演示路由 =====
            api.get("/remote/status", remoteController::status);
            api.get("/remote/sub-servers", remoteController::listSubServers);
            api.post("/remote/sub-servers", remoteController::registerSubServer);
            api.delete("/remote/sub-servers/{subServerId}", remoteController::unregisterSubServer);
            api.post("/remote/sub-servers/{subServerId}/connect", remoteController::connectSubServer);
            api.post("/remote/sub-servers/{subServerId}/disconnect", remoteController::disconnectSubServer);

            // ===== Artisan 命令演示路由 =====
            api.get("/artisan/demo", request -> {
                ArtisanApplication artisan = context.getBean(ArtisanApplication.class);
                Map<String, Object> result = new LinkedHashMap<>();

                // 列出所有已注册命令
                Map<String, ArtisanCommand> commands = artisan.all();
                List<Map<String, String>> commandList = new ArrayList<>();
                commands.forEach((name, cmd) -> {
                    Map<String, String> info = new LinkedHashMap<>();
                    info.put("name", name);
                    info.put("signature", cmd.signature());
                    info.put("description", cmd.description());
                    commandList.add(info);
                });
                result.put("commands", commandList);
                result.put("command_count", commands.size());

                // 执行 hello 命令演示（输出到控制台，此处返回退出码）
                int exitCode = artisan.call("hello", new String[]{"Jaravel"});
                result.put("demo_command", "hello Jaravel");
                result.put("exit_code", exitCode);
                result.put("message", "Artisan 命令演示完成，命令输出已打印到控制台日志");

                return ResponseBuilder.json(result);
            });

            // ===== 定时任务状态路由 =====
            api.get("/schedule/status", request -> {
                Schedule schedule = context.getBean(Schedule.class);
                Map<String, Object> result = new LinkedHashMap<>();

                List<Map<String, Object>> taskList = new ArrayList<>();
                for (ScheduledTask task : schedule.all()) {
                    Map<String, Object> taskInfo = new LinkedHashMap<>();
                    taskInfo.put("name", task.getName());
                    taskInfo.put("cron", task.getCronExpression());
                    taskInfo.put("distributed_lock", task.isDistributedLock());
                    taskInfo.put("lock_ttl_seconds", task.getLockTtlSeconds());
                    taskInfo.put("description", task.getDescription());
                    taskInfo.put("is_artisan_command", task.isArtisanCommand());
                    taskList.add(taskInfo);
                }
                result.put("tasks", taskList);
                result.put("task_count", schedule.size());
                result.put("message", "定时任务状态查询完成");

                return ResponseBuilder.json(result);
            });

            // ===== 队列任务演示路由 =====
            api.get("/queue/demo", request -> {
                Dispatcher dispatcher = context.getBean(Dispatcher.class);
                Map<String, Object> result = new LinkedHashMap<>();

                // 分发通知事件，SendNotificationJob（实现 ShouldQueue）将异步执行
                String type = request.query("type", "email");
                String recipient = request.query("recipient", "demo@jaravel.com");
                String content = request.query("content", "这是一条来自 jaravel-demo 的通知");

                dispatcher.dispatch(new NotificationEvent(type, recipient, content));

                result.put("event", "NotificationEvent");
                result.put("type", type);
                result.put("recipient", recipient);
                result.put("content", content);
                result.put("queue", "notification");
                result.put("async", true);
                result.put("message", "通知事件已分发，SendNotificationJob 将在 notification 队列异步执行（查看日志输出）");

                return ResponseBuilder.json(result);
            });

            // ===== 微信 access_token 演示路由 =====
            api.get("/wechat/token", request -> {
                Map<String, Object> result = new LinkedHashMap<>();

                try {
                    WechatProperties wechatProps = context.getBean(WechatProperties.class);
                    WechatProperties.OfficialAccountConfig mpConfig = wechatProps.getOfficialAccount("default");

                    if (mpConfig == null || mpConfig.getAppId() == null || mpConfig.getAppId().isEmpty()) {
                        result.put("message", "未配置公众号 appId/secret，请在 application.yml 中配置 jaravel.wechat.official-accounts.default");
                        result.put("configured", false);
                        return ResponseBuilder.json(result);
                    }

                    AccessTokenManager tokenManager = context.getBean(AccessTokenManager.class);
                    String token = tokenManager.getToken(mpConfig.getAppId(), mpConfig.getSecret());

                    result.put("access_token", token);
                    result.put("app_id", mpConfig.getAppId());
                    result.put("configured", true);
                    result.put("message", "access_token 获取成功");
                } catch (Exception e) {
                    result.put("configured", true);
                    result.put("error", e.getMessage());
                    result.put("message", "获取 access_token 失败（请检查 appId/secret 是否正确）");
                }

                return ResponseBuilder.json(result);
            });

            // ===== RBAC 权限管理演示路由 =====
            // 演示「管理员-角色-权限」三元权限体系，含树形权限与权限溯源
            AdminRbacController rbacController = context.getBean(AdminRbacController.class);

            // 管理员 CRUD
            api.get("/rbac/admins", rbacController::listAdmins);
            api.post("/rbac/admins", rbacController::createAdmin);
            api.get("/rbac/admins/{id}", rbacController::showAdmin);
            api.put("/rbac/admins/{id}", rbacController::updateAdmin);
            api.delete("/rbac/admins/{id}", rbacController::deleteAdmin);

            // 角色 CRUD
            api.get("/rbac/roles", rbacController::listRoles);
            api.post("/rbac/roles", rbacController::createRole);
            api.get("/rbac/roles/{id}", rbacController::showRole);
            api.put("/rbac/roles/{id}", rbacController::updateRole);
            api.delete("/rbac/roles/{id}", rbacController::deleteRole);

            // 权限 CRUD
            api.get("/rbac/permissions", rbacController::listPermissions);
            api.post("/rbac/permissions", rbacController::createPermission);
            api.get("/rbac/permissions/{id}", rbacController::showPermission);
            api.put("/rbac/permissions/{id}", rbacController::updatePermission);
            api.delete("/rbac/permissions/{id}", rbacController::deletePermission);

            // 管理员 ↔ 角色
            api.get("/rbac/admins/{id}/roles", rbacController::adminRolesAll);
            api.get("/rbac/admins/{id}/roles/assigned", rbacController::adminRolesAssigned);
            api.post("/rbac/admins/{id}/roles", rbacController::assignRole);
            api.delete("/rbac/admins/{id}/roles/{roleId}", rbacController::removeRole);

            // 角色 ↔ 权限
            api.get("/rbac/roles/{id}/permissions", rbacController::rolePermissionsAll);
            api.get("/rbac/roles/{id}/permissions/assigned", rbacController::rolePermissionsAssigned);
            api.post("/rbac/roles/{id}/permissions", rbacController::assignPermission);
            api.delete("/rbac/roles/{id}/permissions/{permissionId}", rbacController::removePermission);

            // 管理员 ↔ 权限（树形祖先授权 + 溯源）
            api.get("/rbac/admins/{id}/permissions", rbacController::adminPermissionsAll);
            api.get("/rbac/admins/{id}/permissions/assigned", rbacController::adminPermissionsAssigned);
            api.get("/rbac/admins/{id}/check-permission/{permissionId}", rbacController::checkPermission);
            api.get("/rbac/admins/{id}/check-role/{roleId}", rbacController::checkRole);
            api.get("/rbac/admins/{id}/permissions/{permissionId}/grantors", rbacController::permissionGrantors);
        });
    }
}
