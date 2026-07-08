package com.weacsoft.jaravel.app.model.user.middle;

import com.weacsoft.jaravel.vendor.database.BaseModel;
import gaarason.database.annotation.Column;
import gaarason.database.annotation.Primary;
import gaarason.database.annotation.Table;
import gaarason.database.query.QueryBuilder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 角色↔权限 中间表模型，对应 {@code user_role_permission} 表（pivot）。
 * <p>
 * 命名为 {@code UserRolePermission}（而非 {@code RolePermission}）以避免与 Admin RBAC 的
 * {@code com.weacsoft.jaravel.app.model.admin.middle.RolePermission} 冲突。
 * <p>
 * 由于权限是树形层级，授予父权限即隐含授予全部后代，本表只需记录「显式授予」的权限节点。
 *
 * @see com.weacsoft.jaravel.app.model.user.UserPermission 权限实体（树形）
 * @see com.weacsoft.jaravel.app.service.UserRolePermissionService 树形权限判断逻辑
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Repository
@Table(name = "user_role_permission")
public class UserRolePermission extends BaseModel<UserRolePermission, Long> {

    @Primary
    @Column(name = "id")
    private Long id;

    @Column(name = "role_id")
    private Long roleId;

    @Column(name = "permission_id")
    private Long permissionId;

    @Column(name = "created_at")
    private String createdAt;

    @Column(name = "updated_at")
    private String updatedAt;

    public static QueryBuilder<UserRolePermission, Long> query() {
        return BaseModel.query(UserRolePermission.class);
    }

    public static List<UserRolePermission> findByRoleId(Long roleId) {
        return query().where("role_id", roleId).get().toObjectList();
    }

    public static List<UserRolePermission> findByPermissionId(Long permissionId) {
        return query().where("permission_id", permissionId).get().toObjectList();
    }

    public static boolean exists(Long roleId, Long permissionId) {
        return query().where("role_id", roleId).where("permission_id", permissionId).first() != null;
    }
}
