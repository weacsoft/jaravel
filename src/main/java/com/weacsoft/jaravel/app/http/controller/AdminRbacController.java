package com.weacsoft.jaravel.app.http.controller;

import com.weacsoft.jaravel.app.model.admin.Admin;
import com.weacsoft.jaravel.app.model.admin.AdminPermission;
import com.weacsoft.jaravel.app.model.admin.AdminRole;
import com.weacsoft.jaravel.app.service.AdminRolePermissionService;
import com.weacsoft.jaravel.vendor.controller.Controllers;
import com.weacsoft.jaravel.vendor.http.request.Request;
import com.weacsoft.jaravel.vendor.http.response.Response;
import com.weacsoft.jaravel.vendor.http.response.ResponseBuilder;
import org.springframework.stereotype.Controller;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * RBAC 权限管理控制器，对齐 Laravel 的 {@code AdminRbacController}。
 * <p>
 * 演示用一套 RESTful API 完整覆盖「管理员-角色-权限」三元 RBAC 体系：
 * <ul>
 *   <li>三类主体的增删改查；</li>
 *   <li>管理员↔角色、角色↔权限 的分配与解除；</li>
 *   <li>管理员权限判断（默认拒绝 + 树形祖先授权）与权限溯源。</li>
 * </ul>
 * 全部业务逻辑由 {@link AdminRolePermissionService} 静态方法承担，控制器只做参数装配与响应构建，
 * 体现 jaravel「Controller 薄、Service 厚」的分层风格。
 */
@Controller
public class AdminRbacController implements Controllers {

    // ==================== 管理员 CRUD ====================

    public Response listAdmins(Request request) {
        return ResponseBuilder.json(Map.of("list", AdminRolePermissionService.listAdmins()));
    }

    public Response createAdmin(Request request) {
        Admin admin = AdminRolePermissionService.createAdmin(
                request.input("username"),
                request.input("password"),
                request.input("nickname"),
                request.input("description"));
        return ResponseBuilder.json(Map.of("admin", admin, "message", "管理员创建成功"));
    }

    public Response showAdmin(Request request) {
        Long id = request.routeParam("id", Long.class);
        Admin admin = AdminRolePermissionService.findAdmin(id);
        if (admin == null) {
            return ResponseBuilder.error(404, "管理员不存在");
        }
        return ResponseBuilder.json(Map.of("admin", admin));
    }

    public Response updateAdmin(Request request) {
        Long id = request.routeParam("id", Long.class);
        Map<String, Object> fields = collectInputs(request, "username", "password", "nickname", "description", "status");
        Admin admin = AdminRolePermissionService.updateAdmin(id, fields);
        if (admin == null) {
            return ResponseBuilder.error(404, "管理员不存在");
        }
        return ResponseBuilder.json(Map.of("admin", admin, "message", "管理员更新成功"));
    }

    public Response deleteAdmin(Request request) {
        Long id = request.routeParam("id", Long.class);
        boolean ok = AdminRolePermissionService.deleteAdmin(id);
        return ResponseBuilder.json(Map.of("success", ok, "message", ok ? "管理员已删除" : "管理员不存在"));
    }

    // ==================== 角色 CRUD ====================

    public Response listRoles(Request request) {
        return ResponseBuilder.json(Map.of("list", AdminRolePermissionService.listRoles()));
    }

    public Response createRole(Request request) {
        AdminRole role = AdminRolePermissionService.createRole(
                request.input("name"),
                request.input("code"),
                request.input("description"));
        return ResponseBuilder.json(Map.of("role", role, "message", "角色创建成功"));
    }

    public Response showRole(Request request) {
        Long id = request.routeParam("id", Long.class);
        AdminRole role = AdminRolePermissionService.findRole(id);
        if (role == null) {
            return ResponseBuilder.error(404, "角色不存在");
        }
        return ResponseBuilder.json(Map.of("role", role));
    }

    public Response updateRole(Request request) {
        Long id = request.routeParam("id", Long.class);
        Map<String, Object> fields = collectInputs(request, "name", "code", "description");
        AdminRole role = AdminRolePermissionService.updateRole(id, fields);
        if (role == null) {
            return ResponseBuilder.error(404, "角色不存在");
        }
        return ResponseBuilder.json(Map.of("role", role, "message", "角色更新成功"));
    }

    public Response deleteRole(Request request) {
        Long id = request.routeParam("id", Long.class);
        boolean ok = AdminRolePermissionService.deleteRole(id);
        return ResponseBuilder.json(Map.of("success", ok, "message", ok ? "角色已删除" : "角色不存在"));
    }

    // ==================== 权限 CRUD ====================

    public Response listPermissions(Request request) {
        return ResponseBuilder.json(Map.of("list", AdminRolePermissionService.listPermissions()));
    }

    public Response createPermission(Request request) {
        Long parentId = longInput(request, "parent_id");
        AdminPermission permission = AdminRolePermissionService.createPermission(
                request.input("name"),
                request.input("code"),
                parentId,
                request.input("route"),
                request.input("description"));
        return ResponseBuilder.json(Map.of("permission", permission, "message", "权限创建成功"));
    }

    public Response showPermission(Request request) {
        Long id = request.routeParam("id", Long.class);
        AdminPermission permission = AdminRolePermissionService.findPermission(id);
        if (permission == null) {
            return ResponseBuilder.error(404, "权限不存在");
        }
        return ResponseBuilder.json(Map.of("permission", permission));
    }

    public Response updatePermission(Request request) {
        Long id = request.routeParam("id", Long.class);
        Map<String, Object> fields = collectInputs(request, "name", "code", "parent_id", "route", "description");
        AdminPermission permission = AdminRolePermissionService.updatePermission(id, fields);
        if (permission == null) {
            return ResponseBuilder.error(404, "权限不存在");
        }
        return ResponseBuilder.json(Map.of("permission", permission, "message", "权限更新成功"));
    }

    public Response deletePermission(Request request) {
        Long id = request.routeParam("id", Long.class);
        boolean ok = AdminRolePermissionService.deletePermission(id);
        return ResponseBuilder.json(Map.of("success", ok, "message", ok ? "权限已删除（子节点已上移）" : "权限不存在"));
    }

    // ==================== 管理员 ↔ 角色 ====================

    /** 管理员的全部角色（含未分配，带 assigned 标记） */
    public Response adminRolesAll(Request request) {
        Long id = request.routeParam("id", Long.class);
        return ResponseBuilder.json(Map.of("list", AdminRolePermissionService.getAdminRolesAll(id)));
    }

    /** 管理员已分配的角色 */
    public Response adminRolesAssigned(Request request) {
        Long id = request.routeParam("id", Long.class);
        return ResponseBuilder.json(Map.of("list", AdminRolePermissionService.getAdminRolesAssigned(id)));
    }

    /** 为管理员分配角色 */
    public Response assignRole(Request request) {
        Long adminId = request.routeParam("id", Long.class);
        Long roleId = longInput(request, "role_id");
        boolean ok = AdminRolePermissionService.assignRoleToAdmin(adminId, roleId);
        return ResponseBuilder.json(Map.of("success", ok, "message", ok ? "角色已分配" : "角色已分配过或参数无效"));
    }

    /** 解除管理员的角色 */
    public Response removeRole(Request request) {
        Long adminId = request.routeParam("id", Long.class);
        Long roleId = request.routeParam("roleId", Long.class);
        int affected = AdminRolePermissionService.removeRoleFromAdmin(adminId, roleId);
        return ResponseBuilder.json(Map.of("affected", affected, "message", affected > 0 ? "角色已解除" : "无关联记录"));
    }

    // ==================== 角色 ↔ 权限 ====================

    /** 角色的全部权限（含未分配，带 assigned 标记） */
    public Response rolePermissionsAll(Request request) {
        Long id = request.routeParam("id", Long.class);
        return ResponseBuilder.json(Map.of("list", AdminRolePermissionService.getRolePermissionsAll(id)));
    }

    /** 角色已分配的权限 */
    public Response rolePermissionsAssigned(Request request) {
        Long id = request.routeParam("id", Long.class);
        return ResponseBuilder.json(Map.of("list", AdminRolePermissionService.getRolePermissionsAssigned(id)));
    }

    /** 为角色分配权限 */
    public Response assignPermission(Request request) {
        Long roleId = request.routeParam("id", Long.class);
        Long permissionId = longInput(request, "permission_id");
        boolean ok = AdminRolePermissionService.assignPermissionToRole(roleId, permissionId);
        return ResponseBuilder.json(Map.of("success", ok, "message", ok ? "权限已分配" : "权限已分配过或参数无效"));
    }

    /** 解除角色的权限 */
    public Response removePermission(Request request) {
        Long roleId = request.routeParam("id", Long.class);
        Long permissionId = request.routeParam("permissionId", Long.class);
        int affected = AdminRolePermissionService.removePermissionFromRole(roleId, permissionId);
        return ResponseBuilder.json(Map.of("affected", affected, "message", affected > 0 ? "权限已解除" : "无关联记录"));
    }

    // ==================== 管理员 ↔ 权限（树形） ====================

    /** 管理员的全部权限（含未分配，带 assigned 标记，树形祖先授权感知） */
    public Response adminPermissionsAll(Request request) {
        Long id = request.routeParam("id", Long.class);
        return ResponseBuilder.json(Map.of("list", AdminRolePermissionService.getAdminPermissionsAll(id)));
    }

    /** 管理员已生效的权限（含树形推导的后代权限） */
    public Response adminPermissionsAssigned(Request request) {
        Long id = request.routeParam("id", Long.class);
        return ResponseBuilder.json(Map.of("list", AdminRolePermissionService.getAdminPermissionsAssigned(id)));
    }

    /**
     * 检查管理员权限（默认拒绝 + 树形祖先授权）。
     * <p>
     * 支持两种用法：
     * <ul>
     *   <li>路径参数指定权限 ID：{@code /admins/{id}/check-permission/{permissionId}}</li>
     *   <li>查询参数指定权限 code：{@code /admins/{id}/check-permission/0?code=user.create}</li>
     * </ul>
     */
    public Response checkPermission(Request request) {
        Long adminId = request.routeParam("id", Long.class);
        Long permissionId = request.routeParam("permissionId", Long.class);
        boolean has;
        String by;
        String code = request.query("code");
        if (code != null && !code.isEmpty()) {
            has = AdminRolePermissionService.adminHasPermission(adminId, code);
            by = "code:" + code;
        } else {
            has = AdminRolePermissionService.adminHasPermission(adminId, permissionId);
            by = "id:" + permissionId;
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("admin_id", adminId);
        result.put("permission", by);
        result.put("has_permission", has);
        result.put("message", has ? "拥有该权限" : "无该权限（默认拒绝）");
        return ResponseBuilder.json(result);
    }

    /** 检查管理员是否拥有指定角色（支持 ?code=super_admin 或路径 {roleId}） */
    public Response checkRole(Request request) {
        Long adminId = request.routeParam("id", Long.class);
        Long roleId = request.routeParam("roleId", Long.class);
        boolean has;
        String by;
        String code = request.query("code");
        if (code != null && !code.isEmpty()) {
            has = AdminRolePermissionService.adminHasRole(adminId, code);
            by = "code:" + code;
        } else {
            has = AdminRolePermissionService.adminHasRole(adminId, roleId);
            by = "id:" + roleId;
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("admin_id", adminId);
        result.put("role", by);
        result.put("has_role", has);
        return ResponseBuilder.json(result);
    }

    /** 溯源：查询管理员的某权限是由哪个（些）角色授予的 */
    public Response permissionGrantors(Request request) {
        Long adminId = request.routeParam("id", Long.class);
        Long permissionId = request.routeParam("permissionId", Long.class);
        List<AdminRole> grantors = AdminRolePermissionService.findRolesGrantingPermission(adminId, permissionId);
        return ResponseBuilder.json(Map.of("admin_id", adminId, "permission_id", permissionId,
                "grantor_roles", grantors, "count", grantors.size()));
    }

    // ==================== 工具方法 ====================

    /**
     * 从请求中收集指定字段的非空输入，构建更新字段映射。
     * 仅收集存在且非空的字段，避免误置空。
     */
    private Map<String, Object> collectInputs(Request request, String... keys) {
        Map<String, Object> fields = new LinkedHashMap<>();
        for (String key : keys) {
            String value = request.input(key);
            if (value != null && !value.isEmpty()) {
                if ("status".equals(key) || "parent_id".equals(key)) {
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

    /**
     * 读取 Long 类型输入。以字符串方式读取后解析，兼容 JSON 数字与字符串两种形式，
     * 避免 {@code input(key, Class)} 在类型不匹配时返回 null。
     */
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
