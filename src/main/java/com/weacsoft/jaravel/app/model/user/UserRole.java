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
 * 用户角色模型，对应 {@code user_roles} 表。
 * <p>
 * 与 Admin RBAC 的 {@link com.weacsoft.jaravel.app.model.admin.AdminRole} 对称，
 * 面向普通用户。角色是用户与权限之间的桥梁。
 * <p>
 * {@link #assigned} 为非数据库字段，用于「查询用户的所有角色（含未分配）」时标记。
 *
 * @see com.weacsoft.jaravel.app.model.user.middle.UserRole 用户↔角色 中间表
 * @see com.weacsoft.jaravel.app.model.user.middle.UserRolePermission 角色↔权限 中间表
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Repository
@Table(name = "user_roles")
public class UserRole extends BaseModel<UserRole, Long> {

    @Primary
    @Column(name = "id")
    private Long id;

    @Column(name = "name")
    private String name;

    /** 角色编码，唯一，如 "tenant_admin" */
    @Column(name = "code")
    private String code;

    @Column(name = "description")
    private String description;

    @Column(name = "created_at")
    private String createdAt;

    @Column(name = "updated_at")
    private String updatedAt;

    /** 是否已分配（非数据库字段） */
    @Column(inDatabase = false)
    private Boolean assigned;

    public static UserRole find(Long id) {
        return BaseModel.find(UserRole.class, id);
    }

    public static List<UserRole> all() {
        return BaseModel.all(UserRole.class);
    }

    public static QueryBuilder<UserRole, Long> query() {
        return BaseModel.query(UserRole.class);
    }

    public static UserRole findByCode(String code) {
        Record<UserRole, Long> record = query().where("code", code).first();
        return record == null ? null : record.toObject();
    }
}
