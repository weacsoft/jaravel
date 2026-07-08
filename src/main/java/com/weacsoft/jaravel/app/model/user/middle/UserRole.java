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
 * 用户↔角色 中间表模型，对应 {@code user_role} 表（pivot）。
 * <p>
 * 与主体角色模型 {@link com.weacsoft.jaravel.app.model.user.UserRole}（对应 {@code user_roles} 表）
 * 同名但不同包，分别表示「中间表记录」与「角色实体」。
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Repository("userRolePivot")
@Table(name = "user_role")
public class UserRole extends BaseModel<UserRole, Long> {

    @Primary
    @Column(name = "id")
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "role_id")
    private Long roleId;

    @Column(name = "created_at")
    private String createdAt;

    @Column(name = "updated_at")
    private String updatedAt;

    public static QueryBuilder<UserRole, Long> query() {
        return BaseModel.query(UserRole.class);
    }

    public static List<UserRole> findByUserId(Long userId) {
        return query().where("user_id", userId).get().toObjectList();
    }

    public static List<UserRole> findByRoleId(Long roleId) {
        return query().where("role_id", roleId).get().toObjectList();
    }

    public static boolean exists(Long userId, Long roleId) {
        return query().where("user_id", userId).where("role_id", roleId).first() != null;
    }
}
