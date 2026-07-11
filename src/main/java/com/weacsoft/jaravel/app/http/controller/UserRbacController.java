package com.weacsoft.jaravel.app.http.controller;

import com.weacsoft.jaravel.app.model.User;
import com.weacsoft.jaravel.app.model.user.UserPermission;
import com.weacsoft.jaravel.app.model.user.UserRole;
import com.weacsoft.jaravel.app.service.UserRolePermissionService;
import com.weacsoft.jaravel.vendor.http.controller.Controllers;
import com.weacsoft.jaravel.vendor.http.controller.request.Request;
import com.weacsoft.jaravel.vendor.http.controller.response.Response;
import com.weacsoft.jaravel.vendor.http.controller.response.ResponseBuilder;
import org.springframework.stereotype.Controller;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 用户 RBAC 权限管理控制器，面向多租户场景。
 * <p>
 * 与 {@link AdminRbacController} 对称，但面向普通用户。
 * 提供用户/角色/权限的 CRUD、分配管理、权限判断、路由权限检查等内部方法。
 * <p>
 * 本控制器为系统内部使用，方法通过 Javadoc 说明用途，不作为对外 REST API 文档。
 * <p>
 * 典型场景：多租户 Java/Jar 插件运行权限控制——
 * 不同用户拥有不同的插件运行权限（有的允许 Java，有的仅允许 Jar），
 * 通过 {@link UserRolePermissionService#userCanAccessRoute(Long, String)} 在中间件层自动拦截。
 *
 * @see UserRolePermissionService 业务逻辑
 * @see com.weacsoft.jaravel.app.http.middleware.UserRoutePermissionMiddleware 路由权限中间件
 */
@Controller
public class UserRbacController implements Controllers {

    // ==================== 用户 CRUD ====================

    /** 用户列表 */
    public Response listUsers(Request request) {
        return ResponseBuilder.json(Map.of("list", UserRolePermissionService.listUsers()));
    }

    /** 创建用户 */
    public Response createUser(Request request) {
        User user = UserRolePermissionService.createUser(
                request.input("name"),
                request.input("number"),
                request.input("password"),
                request.input("email"),
                request.input("description"));
        return ResponseBuilder.json(Map.of("user", user, "message", "用户创建成功"));
    }

    /** 查询单个用户 */
    public Response showUser(Request request) {
        Long id = request.routeParam("id", Long.class);
        User user = UserRolePermissionService.findUser(id);
        if (user == null) {
            return ResponseBuilder.error(404, "用户不存在");
        }
        return ResponseBuilder.json(Map.of("user", user));
    }

    /** 更新用户 */
    public Response updateUser(Request request) {
        Long id = request.routeParam("id", Long.class);
        Map<String, Object> fields = collectInputs(request, "name", "number", "password", "email", "description");
        User user = UserRolePermissionService.updateUser(id, fields);
        if (user == null) {
            return ResponseBuilder.error(404, "用户不存在");
        }
        return ResponseBuilder.json(Map.of("user", user, "message", "用户更新成功"));
    }

    /** 删除用户（级联清理关联） */
    public Response deleteUser(Request request) {
        Long id = request.routeParam("id", Long.class);
        boolean ok = UserRolePermissionService.deleteUser(id);
        return ResponseBuilder.json(Map.of("success", ok, "message", ok ? "用户已删除" : "用户不存在"));
    }

    // ==================== 角色 CRUD ====================

    /** 角色列表 */
    public Response listRoles(Request request) {
        return ResponseBuilder.json(Map.of("list", UserRolePermissionService.listRoles()));
    }

    /** 创建角色 */
    public Response createRole(Request request) {
        UserRole role = UserRolePermissionService.createRole(
                request.input("name"),
                request.input("code"),
                request.input("description"));
        return ResponseBuilder.json(Map.of("role", role, "message", "角色创建成功"));
    }

    /** 查询单个角色 */
    public Response showRole(Request request) {
        Long id = request.routeParam("id", Long.class);
        UserRole role = UserRolePermissionService.findRole(id);
        if (role == null) {
            return ResponseBuilder.error(404, "角色不存在");
        }
        return ResponseBuilder.json(Map.of("role", role));
    }

    /** 更新角色 */
    public Response updateRole(Request request) {
        Long id = request.routeParam("id", Long.class);
        Map<String, Object> fields = collectInputs(request, "name", "code", "description");
        UserRole role = UserRolePermissionService.updateRole(id, fields);
        if (role == null) {
            return ResponseBuilder.error(404, "角色不存在");
        }
        return ResponseBuilder.json(Map.of("role", role, "message", "角色更新成功"));
    }

    /** 删除角色（级联清理关联） */
    public Response deleteRole(Request request) {
        Long id = request.routeParam("id", Long.class);
        boolean ok = UserRolePermissionService.deleteRole(id);
        return ResponseBuilder.json(Map.of("success", ok, "message", ok ? "角色已删除" : "角色不存在"));
    }

    // ==================== 权限 CRUD ====================

    /** 权限列表 */
    public Response listPermissions(Request request) {
        return ResponseBuilder.json(Map.of("list", UserRolePermissionService.listPermissions()));
    }

    /** 创建权限（支持 parent_id 树形层级和 route 路由关联） */
    public Response createPermission(Request request) {
        Long parentId = longInput(request, "parent_id");
        UserPermission permission = UserRolePermissionService.createPermission(
                request.input("name"),
                request.input("code"),
                parentId,
                request.input("route"),
                request.input("description"));
        return ResponseBuilder.json(Map.of("permission", permission, "message", "权限创建成功"));
    }

    /** 查询单个权限 */
    public Response showPermission(Request request) {
        Long id = request.routeParam("id", Long.class);
        UserPermission permission = UserRolePermissionService.findPermission(id);
        if (permission == null) {
            return ResponseBuilder.error(404, "权限不存在");
        }
        return ResponseBuilder.json(Map.of("permission", permission));
    }

    /** 更新权限 */
    public Response updatePermission(Request request) {
        Long id = request.routeParam("id", Long.class);
        Map<String, Object> fields = collectInputs(request, "name", "code", "parent_id", "route", "description");
        UserPermission permission = UserRolePermissionService.updatePermission(id, fields);
        if (permission == null) {
            return ResponseBuilder.error(404, "权限不存在");
        }
        return ResponseBuilder.json(Map.of("permission", permission, "message", "权限更新成功"));
    }

    /** 删除权限（子节点上移到祖父） */
    public Response deletePermission(Request request) {
        Long id = request.routeParam("id", Long.class);
        boolean ok = UserRolePermissionService.deletePermission(id);
        return ResponseBuilder.json(Map.of("success", ok, "message", ok ? "权限已删除（子节点已上移）" : "权限不存在"));
    }

    // ==================== 用户 ↔ 角色 ====================

    /** 用户的全部角色（含未分配，带 assigned 标记） */
    public Response userRolesAll(Request request) {
        Long id = request.routeParam("id", Long.class);
        return ResponseBuilder.json(Map.of("list", UserRolePermissionService.getUserRolesAll(id)));
    }

    /** 用户已分配的角色 */
    public Response userRolesAssigned(Request request) {
        Long id = request.routeParam("id", Long.class);
        return ResponseBuilder.json(Map.of("list", UserRolePermissionService.getUserRolesAssigned(id)));
    }

    /** 为用户分配角色 */
    public Response assignRole(Request request) {
        Long userId = request.routeParam("id", Long.class);
        Long roleId = longInput(request, "role_id");
        boolean ok = UserRolePermissionService.assignRoleToUser(userId, roleId);
        return ResponseBuilder.json(Map.of("success", ok, "message", ok ? "角色已分配" : "角色已分配过或参数无效"));
    }

    /** 解除用户的角色 */
    public Response removeRole(Request request) {
        Long userId = request.routeParam("id", Long.class);
        Long roleId = request.routeParam("roleId", Long.class);
        int affected = UserRolePermissionService.removeRoleFromUser(userId, roleId);
        return ResponseBuilder.json(Map.of("affected", affected, "message", affected > 0 ? "角色已解除" : "无关联记录"));
    }

    // ==================== 角色 ↔ 权限 ====================

    /** 角色的全部权限（含未分配，带 assigned 标记） */
    public Response rolePermissionsAll(Request request) {
        Long id = request.routeParam("id", Long.class);
        return ResponseBuilder.json(Map.of("list", UserRolePermissionService.getRolePermissionsAll(id)));
    }

    /** 角色已分配的权限 */
    public Response rolePermissionsAssigned(Request request) {
        Long id = request.routeParam("id", Long.class);
        return ResponseBuilder.json(Map.of("list", UserRolePermissionService.getRolePermissionsAssigned(id)));
    }

    /** 为角色分配权限 */
    public Response assignPermission(Request request) {
        Long roleId = request.routeParam("id", Long.class);
        Long permissionId = longInput(request, "permission_id");
        boolean ok = UserRolePermissionService.assignPermissionToRole(roleId, permissionId);
        return ResponseBuilder.json(Map.of("success", ok, "message", ok ? "权限已分配" : "权限已分配过或参数无效"));
    }

    /** 解除角色的权限 */
    public Response removePermission(Request request) {
        Long roleId = request.routeParam("id", Long.class);
        Long permissionId = request.routeParam("permissionId", Long.class);
        int affected = UserRolePermissionService.removePermissionFromRole(roleId, permissionId);
        return ResponseBuilder.json(Map.of("affected", affected, "message", affected > 0 ? "权限已解除" : "无关联记录"));
    }

    // ==================== 用户 ↔ 权限（树形 + 路由） ====================

    /** 用户的全部权限（含未分配，带 assigned 标记，树形祖先授权感知） */
    public Response userPermissionsAll(Request request) {
        Long id = request.routeParam("id", Long.class);
        return ResponseBuilder.json(Map.of("list", UserRolePermissionService.getUserPermissionsAll(id)));
    }

    /** 用户已生效的权限（含树形推导） */
    public Response userPermissionsAssigned(Request request) {
        Long id = request.routeParam("id", Long.class);
        return ResponseBuilder.json(Map.of("list", UserRolePermissionService.getUserPermissionsAssigned(id)));
    }

    /**
     * 检查用户权限（支持 ?code=plugin.java.run 或路径 {permissionId}）。
     * <p>
     * 典型用法（多租户插件运行场景）：
     * <pre>
     * // 检查用户是否能运行 Java 插件
     * GET /api/user-rbac/users/{id}/check-permission/0?code=plugin.java.run
     *
     * // 检查用户是否能运行 Jar 插件
     * GET /api/user-rbac/users/{id}/check-permission/0?code=plugin.jar.run
     * </pre>
     */
    public Response checkPermission(Request request) {
        Long userId = request.routeParam("id", Long.class);
        Long permissionId = request.routeParam("permissionId", Long.class);
        boolean has;
        String by;
        String code = request.query("code");
        if (code != null && !code.isEmpty()) {
            has = UserRolePermissionService.userHasPermission(userId, code);
            by = "code:" + code;
        } else {
            has = UserRolePermissionService.userHasPermission(userId, permissionId);
            by = "id:" + permissionId;
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("user_id", userId);
        result.put("permission", by);
        result.put("has_permission", has);
        result.put("message", has ? "拥有该权限" : "无该权限（默认拒绝）");
        return ResponseBuilder.json(result);
    }

    /** 检查用户是否拥有指定角色（支持 ?code=tenant_admin 或路径 {roleId}） */
    public Response checkRole(Request request) {
        Long userId = request.routeParam("id", Long.class);
        Long roleId = request.routeParam("roleId", Long.class);
        boolean has;
        String by;
        String code = request.query("code");
        if (code != null && !code.isEmpty()) {
            has = UserRolePermissionService.userHasRole(userId, code);
            by = "code:" + code;
        } else {
            has = UserRolePermissionService.userHasRole(userId, roleId);
            by = "id:" + roleId;
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("user_id", userId);
        result.put("role", by);
        result.put("has_role", has);
        return ResponseBuilder.json(result);
    }

    /**
     * 检查用户是否有权访问指定路由（路由模式匹配）。
     * <p>
     * 用法：{@code GET /api/user-rbac/users/{id}/check-route?route=/plugin/java/run}
     */
    public Response checkRoute(Request request) {
        Long userId = request.routeParam("id", Long.class);
        String route = request.query("route");
        boolean has = UserRolePermissionService.userCanAccessRoute(userId, route);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("user_id", userId);
        result.put("route", route);
        result.put("can_access", has);
        result.put("message", has ? "有权访问" : "无权访问（默认拒绝）");
        return ResponseBuilder.json(result);
    }

    /** 获取用户可访问的全部路由模式列表 */
    public Response accessibleRoutes(Request request) {
        Long id = request.routeParam("id", Long.class);
        List<String> routes = UserRolePermissionService.getUserAccessibleRoutes(id);
        return ResponseBuilder.json(Map.of("user_id", id, "routes", routes, "count", routes.size()));
    }

    /** 溯源：查询用户的某权限由哪个角色授予 */
    public Response permissionGrantors(Request request) {
        Long userId = request.routeParam("id", Long.class);
        Long permissionId = request.routeParam("permissionId", Long.class);
        List<UserRole> grantors = UserRolePermissionService.findRolesGrantingPermission(userId, permissionId);
        return ResponseBuilder.json(Map.of("user_id", userId, "permission_id", permissionId,
                "grantor_roles", grantors, "count", grantors.size()));
    }

    // ==================== 工具方法 ====================

    private Map<String, Object> collectInputs(Request request, String... keys) {
        Map<String, Object> fields = new LinkedHashMap<>();
        for (String key : keys) {
            String value = request.input(key);
            if (value != null && !value.isEmpty()) {
                if ("parent_id".equals(key)) {
                    try {
                        fields.put(key, Long.valueOf(value));
                    } catch (NumberFormatException e) {
                        fields.put(key, value);
                    }
                } else {
                    fields.put(key, value);
                }
            }
        }
        return fields;
    }

    private Long longInput(Request request, String key) {
        String value = request.input(key);
        if (value == null || value.isEmpty()) {
            return null;
        }
        try {
            return Long.valueOf(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
