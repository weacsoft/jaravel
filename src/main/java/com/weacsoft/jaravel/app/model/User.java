package com.weacsoft.jaravel.app.model;

import com.weacsoft.jaravel.app.service.UserRolePermissionService;
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
 * 用户模型，对齐 Laravel Eloquent 的 {@code app/Models/User.php}。
 * <p>
 * 单一类即包含数据字段与查询能力，无需分离 Entity 和 Model：
 * <pre>
 * // 创建新记录
 * User user = new User();
 * user.setName("alice");
 * user.save();
 *
 * // 静态查询
 * User found = User.find(1L);
 * List&lt;User&gt; all = User.all();
 * User u = User.query().where("name", "alice").first().toObject();
 *
 * // 克隆
 * User clone = user.replicate();
 * </pre>
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Repository
@Table(name = "users")
public class User extends BaseModel<User, Long> implements Authenticatable {

    @Primary
    @Column(name = "id")
    private Long id;

    @Column(name = "name")
    private String name;

    @Column(name = "number")
    private String number;

    @Column(name = "password")
    private String password;

    @Column(name = "email")
    private String email;

    /** 备注说明 */
    @Column(name = "description")
    private String description;

    @Column(name = "created_at")
    private String createdAt;

    @Column(name = "updated_at")
    private String updatedAt;

    // ---- 静态查询方法（委托给 BaseModel 工具方法） ----

    /** 按主键查询，对齐 Laravel User::find(1) */
    public static User find(Long id) {
        return BaseModel.find(User.class, id);
    }

    /** 查询全部，对齐 Laravel User::all() */
    public static List<User> all() {
        return BaseModel.all(User.class);
    }

    /** 获取查询构造器，对齐 Laravel User::query() */
    public static QueryBuilder<User, Long> query() {
        return BaseModel.query(User.class);
    }

    /** 按工号查询，对齐 Laravel User::where('number', x)->first() */
    public static User findByNumber(String number) {
        Record<User, Long> record = query().where("number", number).first();
        return record == null ? null : record.toObject();
    }

    // ---- Authenticatable ----
    // 仅实现以主键标识用户的方法；密码校验由应用层（Service/Controller）负责，
    // Authenticatable 契约不再包含 getAuthPassword()。

    @Override
    public Object getAuthIdentifier() {
        return id;
    }

    @Override
    public String getAuthIdentifierName() {
        return "id";
    }

    // ---- RBAC 权限判断实例方法 ----

    /**
     * 判断当前用户是否拥有指定角色（按角色 ID）。
     *
     * @param roleId 角色 ID
     * @return 拥有该角色返回 true
     */
    public boolean hasRole(Long roleId) {
        if (id == null || roleId == null) {
            return false;
        }
        return UserRolePermissionService.userHasRole(id, roleId);
    }

    /**
     * 判断当前用户是否拥有指定角色（按角色 code，如 "tenant_admin"）。
     *
     * @param roleCode 角色编码
     * @return 拥有该角色返回 true
     */
    public boolean hasRole(String roleCode) {
        if (id == null || roleCode == null) {
            return false;
        }
        return UserRolePermissionService.userHasRole(id, roleCode);
    }

    /**
     * 判断当前用户是否拥有指定权限（按权限 ID，含树形祖先授权推导）。
     *
     * @param permissionId 权限 ID
     * @return 拥有该权限返回 true
     */
    public boolean hasPermission(Long permissionId) {
        if (id == null || permissionId == null) {
            return false;
        }
        return UserRolePermissionService.userHasPermission(id, permissionId);
    }

    /**
     * 判断当前用户是否拥有指定权限（按权限 code，如 "plugin.java.run"）。
     * <p>
     * 典型用法（多租户插件运行场景）：
     * <pre>
     * User user = User.find(1L);
     * if (user.hasPermission("plugin.java.run")) {
     *     // 允许运行 Java 插件
     * }
     * if (user.hasPermission("plugin.jar.run")) {
     *     // 允许运行 Jar 插件
     * }
     * </pre>
     *
     * @param permissionCode 权限编码
     * @return 拥有该权限（含祖先授权）返回 true
     */
    public boolean hasPermission(String permissionCode) {
        if (id == null || permissionCode == null) {
            return false;
        }
        return UserRolePermissionService.userHasPermission(id, permissionCode);
    }

    /**
     * 判断当前用户是否有权访问指定路由。
     * <p>
     * 遍历用户拥有的所有权限（含树形祖先授权推导），检查每个权限的 route 模式是否匹配目标路由。
     * 路由匹配支持全匹配（{@code /plugin/jar/upload}）和通配匹配（{@code /plugin/java/*}）。
     * 默认拒绝。
     *
     * @param route 目标路由路径，以 {@code /} 开头
     * @return 有权访问返回 true
     */
    public boolean canAccessRoute(String route) {
        if (id == null || route == null) {
            return false;
        }
        return UserRolePermissionService.userCanAccessRoute(id, route);
    }

    /**
     * 获取当前用户可以访问的全部路由模式列表。
     *
     * @return 可访问的路由模式列表（可能含通配符），无权限时返回空列表
     */
    public List<String> getAccessibleRoutes() {
        if (id == null) {
            return List.of();
        }
        return UserRolePermissionService.getUserAccessibleRoutes(id);
    }
}
