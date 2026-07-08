package com.weacsoft.jaravel.database.migration;

import com.weacsoft.jaravel.vendor.migration.Migration;
import com.weacsoft.jaravel.vendor.migration.MigrationAnnotation;
import com.weacsoft.jaravel.vendor.migration.Schema;

/**
 * 迁移：创建用户层 RBAC 权限管理所需的 4 张表。
 * <p>
 * 与 Admin RBAC（{@link Migration_2024_01_03_CreateAdminRbacTables}）对称，
 * 但面向普通用户，用于多租户场景下的 Java/Jar 插件运行权限控制。
 * <ul>
 *   <li>{@code user_roles} —— 用户角色表（主体）</li>
 *   <li>{@code user_permissions} —— 用户权限表（主体，树形层级 + route 路由关联）</li>
 *   <li>{@code user_role} —— 用户↔角色 中间表（pivot）</li>
 *   <li>{@code user_role_permission} —— 角色↔权限 中间表（pivot）</li>
 * </ul>
 */
@MigrationAnnotation
public class Migration_2024_01_04_CreateUserRbacTables implements Migration {

    @Override
    public void up(Schema schema) {
        // 1. 用户角色表
        schema.create("user_roles", table -> {
            table.id();
            table.string("name", 50);
            table.string("code", 50).unique();
            table.string("description", 255).nullable();
            table.text("model_shadow").nullable();
            table.timestamps();
        });

        // 2. 用户权限表（树形层级 + route 路由关联）
        schema.create("user_permissions", table -> {
            table.id();
            table.string("name", 50);
            table.string("code", 80).unique();
            table.bigInteger("parent_id").unsigned().nullable();
            table.string("route", 200).nullable();
            table.string("description", 255).nullable();
            table.text("model_shadow").nullable();
            table.timestamps();
            table.index("parent_id");
        });

        // 3. 用户↔角色 中间表
        schema.create("user_role", table -> {
            table.id();
            table.bigInteger("user_id").unsigned();
            table.bigInteger("role_id").unsigned();
            table.text("model_shadow").nullable();
            table.timestamps();
            table.unique("user_id", "role_id");
        });

        // 4. 角色↔权限 中间表（与 Admin 的 role_permission 区分，命名为 user_role_permission）
        schema.create("user_role_permission", table -> {
            table.id();
            table.bigInteger("role_id").unsigned();
            table.bigInteger("permission_id").unsigned();
            table.text("model_shadow").nullable();
            table.timestamps();
            table.unique("role_id", "permission_id");
        });
    }

    @Override
    public void down(Schema schema) {
        schema.dropIfExists("user_role_permission");
        schema.dropIfExists("user_role");
        schema.dropIfExists("user_permissions");
        schema.dropIfExists("user_roles");
    }
}
