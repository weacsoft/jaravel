package com.weacsoft.jaravel.app.model.admin;

import com.weacsoft.jaravel.app.service.AdminRolePermissionService;
import com.weacsoft.jaravel.vendor.auth.contract.Authenticatable;
import com.weacsoft.jaravel.vendor.database.BaseModel;
import gaarason.database.annotation.Column;
import gaarason.database.annotation.Primary;
import gaarason.database.annotation.Table;
import gaarason.database.contract.eloquent.Record;
import gaarason.database.query.QueryBuilder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.stereotype.Repository;

import java.util.List;
/**
 * 管理员模型，对齐 Laravel Eloquent 的 {@code app/Models/Admin/Admin.php}。
 * <p>
 * 作为 RBAC 权限管理的主体，通过 {@code admin_role} 中间表与 {@link AdminRole} 多对多关联。
 * <p>
 * 除了常规的静态查询方法外，还提供面向对象的权限判断实例方法：
 * <pre>
 * Admin admin = Admin.find(1L);
 * if (admin.hasRole("super_admin") && admin.hasPermission("user.create")) {
 *     // 执行受保护的操作
 * }
 * </pre>
 *
 * @see AdminRolePermissionService 权限判断逻辑委托给此服务
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Repository
@Table(name = "admins")
public class Admin extends BaseModel<Admin, Long> implements Authenticatable {

    @Primary
    @Column(name = "id")
    private Long id;

    @Column(name = "username")
    private String username;

    @Column(name = "password")
    private String password;

    @Column(name = "nickname")
    private String nickname;

    /** 备注说明 */
    @Column(name = "description")
    private String description;

    /** 状态：1=启用，0=禁用 */
    @Column(name = "status")
    private Integer status;

    @Column(name = "created_at")
    private String createdAt;

    @Column(name = "updated_at")
    private String updatedAt;

    // ---- 静态查询方法（委托给 BaseModel 工具方法） ----

    /** 按主键查询，对齐 Laravel Admin::find(1) */
    public static Admin find(Long id) {
        return BaseModel.find(Admin.class, id);
    }

    /** 查询全部，对齐 Laravel Admin::all() */
    public static List<Admin> all() {
        return BaseModel.all(Admin.class);
    }

    /** 获取查询构造器，对齐 Laravel Admin::query() */
    public static QueryBuilder<Admin, Long> query() {
        return BaseModel.query(Admin.class);
    }

    /** 按用户名查询，对齐 Laravel Admin::where('username', x)->first() */
    public static Admin findByUsername(String username) {
        Record<Admin, Long> record = query().where("username", username).first();
        return record == null ? null : record.toObject();
    }

    // ---- Authenticatable 接口实现 ----

    @Override
    public Object getAuthIdentifier() {
        return id;
    }

    @Override
    public String getAuthIdentifierName() {
        return "id";
    }

    // ---- 面向对象的权限判断实例方法 ----

    /**
     * 判断当前管理员是否拥有指定角色（按角色 ID）。
     * <p>
     * 委托给 {@link AdminRolePermissionService#adminHasRole(Long, Long)}。
     *
     * @param roleId 角色 ID
     * @return 拥有该角色返回 true
     */
    public boolean hasRole(Long roleId) {
        if (id == null || roleId == null) {
            return false;
        }
        return AdminRolePermissionService.adminHasRole(id, roleId);
    }

    /**
     * 判断当前管理员是否拥有指定角色（按角色 code，如 "super_admin"）。
     *
     * @param roleCode 角色编码
     * @return 拥有该角色返回 true
     */
    public boolean hasRole(String roleCode) {
        if (id == null || roleCode == null) {
            return false;
        }
        return AdminRolePermissionService.adminHasRole(id, roleCode);
    }

    /**
     * 判断当前管理员是否拥有指定权限（按权限 ID）。
     * <p>
     * 权限为树形层级：若管理员拥有的任一权限是本权限的祖先（或自身），即视为拥有。
     * 默认拒绝（无任何授权时返回 false）。
     *
     * @param permissionId 权限 ID
     * @return 拥有该权限（含祖先授权）返回 true
     */
    public boolean hasPermission(Long permissionId) {
        if (id == null || permissionId == null) {
            return false;
        }
        return AdminRolePermissionService.adminHasPermission(id, permissionId);
    }

    /**
     * 判断当前管理员是否拥有指定权限（按权限 code，如 "user.create"）。
     *
     * @param permissionCode 权限编码
     * @return 拥有该权限（含祖先授权）返回 true
     */
    public boolean hasPermission(String permissionCode) {
        if (id == null || permissionCode == null) {
            return false;
        }
        return AdminRolePermissionService.adminHasPermission(id, permissionCode);
    }

    // ---- 路由权限判断实例方法 ----

    /**
     * 判断当前管理员是否有权访问指定路由。
     * <p>
     * 遍历管理员拥有的所有权限（含树形祖先授权推导），检查每个权限的 {@code route} 模式是否匹配目标路由。
     * 路由匹配支持：
     * <ul>
     *   <li>全匹配：{@code /admin/user} 仅匹配 {@code /admin/user}</li>
     *   <li>通配匹配：{@code /admin/*} 匹配 {@code /admin} 下所有路由</li>
     * </ul>
     * 默认拒绝（无任何权限或无路由匹配时返回 false）。
     * <p>
     * 典型用法：
     * <pre>
     * Admin admin = Admin.find(1L);
     * if (admin.canAccessRoute("/admin/user/create")) {
     *     // 允许访问
     * }
     * </pre>
     *
     * @param route 目标路由路径，以 {@code /} 开头，如 {@code /admin/user/create}
     * @return 有权访问返回 true，否则 false
     * @see AdminRolePermissionService#adminCanAccessRoute(Long, String)
     */
    public boolean canAccessRoute(String route) {
        if (id == null || route == null) {
            return false;
        }
        return AdminRolePermissionService.adminCanAccessRoute(id, route);
    }

    /**
     * 获取当前管理员可以访问的全部路由模式列表。
     * <p>
     * 遍历管理员拥有的所有权限（含树形祖先授权推导），收集每个权限的 {@code route} 模式。
     * 返回的路由模式可能包含通配符（如 {@code /admin/*}），可用于前端渲染菜单或权限展示。
     *
     * @return 可访问的路由模式列表（可能含通配符），无权限时返回空列表
     * @see AdminRolePermissionService#getAdminAccessibleRoutes(Long)
     */
    public List<String> getAccessibleRoutes() {
        if (id == null) {
            return List.of();
        }
        return AdminRolePermissionService.getAdminAccessibleRoutes(id);
    }
}
