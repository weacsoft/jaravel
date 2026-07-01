package com.weacsoft.jaravel.app.model.admin.middle;

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
 * 角色↔权限 中间表模型，对应 {@code role_permission} 表（pivot）。
 * <p>
 * 一条记录描述「某角色拥有某权限」的关联关系。
 * <p>
 * 由于权限是树形层级，授予父权限即隐含授予其全部后代（见
 * {@link com.weacsoft.jaravel.app.model.admin.AdminPermission}），
 * 因此本表只需记录「显式授予」的权限节点，后代权限在权限判断时由祖先授权推导得出。
 *
 * @see com.weacsoft.jaravel.app.model.admin.AdminPermission 权限实体（树形）
 * @see com.weacsoft.jaravel.app.service.AdminRolePermissionService 树形权限判断逻辑
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Repository
@Table(name = "role_permission")
public class RolePermission extends BaseModel<RolePermission, Long> {

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

    // ---- 静态查询方法 ----

    /** 获取查询构造器 */
    public static QueryBuilder<RolePermission, Long> query() {
        return BaseModel.query(RolePermission.class);
    }

    /** 查询指定角色的全部权限关联记录 */
    public static List<RolePermission> findByRoleId(Long roleId) {
        return query().where("role_id", roleId).get().toObjectList();
    }

    /** 查询指定权限被哪些角色引用 */
    public static List<RolePermission> findByPermissionId(Long permissionId) {
        return query().where("permission_id", permissionId).get().toObjectList();
    }

    /** 判断指定角色是否已关联指定权限 */
    public static boolean exists(Long roleId, Long permissionId) {
        return query().where("role_id", roleId).where("permission_id", permissionId).first() != null;
    }
}
