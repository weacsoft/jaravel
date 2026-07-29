package com.weacsoft.jaravel.app.model.admin;

import com.weacsoft.jaravel.vendor.database.BaseModel;
import gaarason.database.annotation.Column;
import gaarason.database.annotation.Primary;
import gaarason.database.annotation.Table;
import gaarason.database.contract.eloquent.Record;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.stereotype.Repository;

/**
 * 角色模型，对齐 Laravel Eloquent 的 {@code app/Models/Admin/AdminRole.php}。
 * <p>
 * 角色是管理员与权限之间的桥梁：管理员通过 {@code admin_role} 中间表关联多个角色，
 * 角色又通过 {@code role_permission} 中间表关联多个权限。
 * <p>
 * {@link #assigned} 为非数据库字段（{@code @Column(inDatabase = false)}），
 * 仅用于「查询管理员的所有角色（含未分配）」时标记该角色是否已分配给指定管理员。
 *
 * @see com.weacsoft.jaravel.app.model.admin.middle.AdminRole 管理员↔角色 中间表模型
 * @see com.weacsoft.jaravel.app.model.admin.middle.RolePermission 角色↔权限 中间表模型
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Repository
@Table(name = "admin_roles")
public class AdminRole extends BaseModel<AdminRole, Long> {

    @Primary
    @Column(name = "id")
    private Long id;

    /** 角色名称，如「超级管理员」 */
    @Column(name = "name")
    private String name;

    /** 角色编码，唯一，如 "super_admin" */
    @Column(name = "code")
    private String code;

    @Column(name = "description")
    private String description;

    @Column(name = "created_at")
    private String createdAt;

    @Column(name = "updated_at")
    private String updatedAt;

    /**
     * 是否已分配（非数据库字段）。
     * <p>
     * 在 {@code getAdminRolesAll} 等方法中，根据管理员是否拥有该角色设置此标记，
     * 便于前端一次性渲染「已分配 / 未分配」的全部角色列表。
     */
    @Column(inDatabase = false)
    private Boolean assigned;

    // ---- 静态入口方法 ----

    /** 获取 Spring 管理的实例，可调用所有 gaarason 方法 */
    public static AdminRole self() {
        return BaseModel.self(AdminRole.class);
    }

    /** 按角色编码查询 */
    public static AdminRole findByCode(String code) {
        Record<AdminRole, Long> record = self().newQuery().where("code", code).first();
        return record == null ? null : record.toObject();
    }
}
