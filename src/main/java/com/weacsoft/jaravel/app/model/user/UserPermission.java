package com.weacsoft.jaravel.app.model.user;

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
 * 用户权限模型，对应 {@code user_permissions} 表。
 * <p>
 * 与 Admin RBAC 的 {@link com.weacsoft.jaravel.app.model.admin.AdminPermission} 对称。
 * <p>
 * <b>树形层级关系</b>：通过 {@link #parentId} 自引用，授权父权限即隐含授予所有子权限。
 * <p>
 * <b>路由关联</b>：{@link #route} 字段关联路由模式（如 {@code /plugin/java/*}），
 * 中间件 {@link com.weacsoft.jaravel.app.http.middleware.UserRoutePermissionMiddleware}
 * 根据此字段判断用户是否有权访问对应功能。{@code null} 表示不关联路由的纯功能权限。
 *
 * @see com.weacsoft.jaravel.app.service.UserRolePermissionService 树形权限判断逻辑
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Repository
@Table(name = "user_permissions")
public class UserPermission extends BaseModel<UserPermission, Long> {

    @Primary
    @Column(name = "id")
    private Long id;

    @Column(name = "name")
    private String name;

    /** 权限编码，唯一，如 "plugin.java.run" */
    @Column(name = "code")
    private String code;

    /** 父权限 ID，null 为根节点 */
    @Column(name = "parent_id")
    private Long parentId;

    /**
     * 关联的路由模式，如 {@code /plugin/java/*}（通配）或 {@code /plugin/jar/upload}（全匹配）。
     * {@code null} 表示不关联路由。
     */
    @Column(name = "route")
    private String route;

    @Column(name = "description")
    private String description;

    @Column(name = "created_at")
    private String createdAt;

    @Column(name = "updated_at")
    private String updatedAt;

    /** 是否已生效（非数据库字段，含树形祖先授权推导） */
    @Column(inDatabase = false)
    private Boolean assigned;

    public static UserPermission find(Long id) {
        return BaseModel.find(UserPermission.class, id);
    }

    public static List<UserPermission> all() {
        return BaseModel.all(UserPermission.class);
    }

    public static QueryBuilder<UserPermission, Long> query() {
        return BaseModel.query(UserPermission.class);
    }

    public static UserPermission findByCode(String code) {
        Record<UserPermission, Long> record = query().where("code", code).first();
        return record == null ? null : record.toObject();
    }
}
