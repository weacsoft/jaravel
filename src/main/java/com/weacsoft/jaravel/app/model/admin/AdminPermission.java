package com.weacsoft.jaravel.app.model.admin;

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
 * 权限模型，对齐 Laravel Eloquent 的 {@code app/Models/Admin/AdminPermission.php}。
 * <p>
 * <b>树形层级关系</b>：通过 {@link #parentId} 自引用 {@code admin_permissions} 表，
 * {@code parent_id} 为 {@code null} 表示根节点。授权语义遵循「父节点授权即等同旗下所有子节点授权」：
 * <ul>
 *   <li>当某角色被授予父权限 P 时，管理员对该权限树下 P 的所有后代节点均视为已授权；</li>
 *   <li>因此设计中<b>不会</b>出现「父节点有权限、子节点没有权限」的情况——
 *       权限判断时只要任一已授权权限是目标权限的祖先（或自身），即判定为拥有。</li>
 * </ul>
 * <p>
 * {@link #assigned} 为非数据库字段，用于「查询管理员的所有权限（含未分配）」时标记
 * 该权限是否对指定管理员生效（已考虑树形祖先授权）。
 *
 * @see AdminRolePermissionService 树形权限判断逻辑
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Repository
@Table(name = "admin_permissions")
public class AdminPermission extends BaseModel<AdminPermission, Long> {

    @Primary
    @Column(name = "id")
    private Long id;

    /** 权限名称，如「用户管理」 */
    @Column(name = "name")
    private String name;

    /** 权限编码，唯一，如 "user.create" */
    @Column(name = "code")
    private String code;

    /**
     * 父权限 ID。{@code null} 为根节点。
     * <p>
     * 自引用 {@code admin_permissions.id}，构成权限树。
     */
    @Column(name = "parent_id")
    private Long parentId;

    /**
     * 关联的路由模式，如 {@code /admin/*}（通配）或 {@code /admin/user}（全匹配）。
     * <p>
     * 路由以 {@code /} 开头，支持两种匹配方式：
     * <ul>
     *   <li><b>全匹配</b>：{@code /admin/user} 仅匹配 {@code /admin/user} 本身</li>
     *   <li><b>部分匹配</b>：{@code /admin/*} 匹配 {@code /admin} 下所有路由（含子路径）</li>
     * </ul>
     * 不使用正则，{@code *} 仅作为后缀通配符。{@code null} 表示此权限不关联路由。
     * <p>
     * 中间件 {@code RoutePermissionMiddleware} 会根据当前请求路径与此模式做匹配，
     * 判断管理员是否有权访问对应功能。
     *
     * @see com.weacsoft.jaravel.app.service.AdminRolePermissionService#routeMatches(String, String)
     * @see com.weacsoft.jaravel.app.http.middleware.RoutePermissionMiddleware
     */
    @Column(name = "route")
    private String route;

    @Column(name = "description")
    private String description;

    @Column(name = "created_at")
    private String createdAt;

    @Column(name = "updated_at")
    private String updatedAt;

    /**
     * 是否已生效（非数据库字段）。
     * <p>
     * 在 {@code getAdminPermissionsAll} 等方法中，根据管理员是否拥有该权限（含祖先授权）设置此标记。
     */
    @Column(inDatabase = false)
    private Boolean assigned;

    // ---- 静态查询方法（委托给 BaseModel 工具方法） ----

    /** 按主键查询 */
    public static AdminPermission find(Long id) {
        return BaseModel.find(AdminPermission.class, id);
    }

    /** 查询全部 */
    public static List<AdminPermission> all() {
        return BaseModel.all(AdminPermission.class);
    }

    /** 获取查询构造器 */
    public static QueryBuilder<AdminPermission, Long> query() {
        return BaseModel.query(AdminPermission.class);
    }

    /** 按权限编码查询 */
    public static AdminPermission findByCode(String code) {
        Record<AdminPermission, Long> record = query().where("code", code).first();
        return record == null ? null : record.toObject();
    }
}
