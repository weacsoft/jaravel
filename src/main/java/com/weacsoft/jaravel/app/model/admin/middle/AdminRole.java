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
 * 管理员↔角色 中间表模型，对应 {@code admin_role} 表（pivot）。
 * <p>
 * 与主体角色模型 {@link com.weacsoft.jaravel.app.model.admin.AdminRole}（对应 {@code admin_roles} 表）
 * 同名但处于不同包（{@code middle}），分别表示「中间表记录」与「角色实体」，
 * 表名亦不同（{@code admin_role} 单数 vs {@code admin_roles} 复数），对齐 Laravel 中间表命名约定。
 * <p>
 * 一条记录描述「某管理员拥有某角色」的关联关系。
 *
 * @see com.weacsoft.jaravel.app.model.admin.AdminRole 角色实体
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Repository
@Table(name = "admin_role")
public class AdminRole extends BaseModel<AdminRole, Long> {

    @Primary
    @Column(name = "id")
    private Long id;

    @Column(name = "admin_id")
    private Long adminId;

    @Column(name = "role_id")
    private Long roleId;

    @Column(name = "created_at")
    private String createdAt;

    @Column(name = "updated_at")
    private String updatedAt;

    // ---- 静态查询方法 ----

    /** 获取查询构造器 */
    public static QueryBuilder<AdminRole, Long> query() {
        return BaseModel.query(AdminRole.class);
    }

    /** 查询指定管理员的全部角色关联记录 */
    public static List<AdminRole> findByAdminId(Long adminId) {
        return query().where("admin_id", adminId).get().toObjectList();
    }

    /** 查询指定角色的全部管理员关联记录 */
    public static List<AdminRole> findByRoleId(Long roleId) {
        return query().where("role_id", roleId).get().toObjectList();
    }

    /** 判断指定管理员是否已关联指定角色 */
    public static boolean exists(Long adminId, Long roleId) {
        return query().where("admin_id", adminId).where("role_id", roleId).first() != null;
    }
}
