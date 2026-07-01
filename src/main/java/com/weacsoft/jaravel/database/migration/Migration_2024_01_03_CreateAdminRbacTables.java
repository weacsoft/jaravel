package com.weacsoft.jaravel.database.migration;

import com.weacsoft.jaravel.vendor.migration.Migration;
import com.weacsoft.jaravel.vendor.migration.Schema;
import org.springframework.stereotype.Component;

/**
 * 迁移：创建 RBAC 权限管理所需的 5 张表（演示完整的用户权限管理）。
 * <p>
 * 一次 up() 创建 5 张表，down() 对称删除，展示「一个迁移处理多张关联表」的能力：
 * <ul>
 *   <li>{@code admins} —— 管理员表（主体）</li>
 *   <li>{@code admin_roles} —— 角色表（主体）</li>
 *   <li>{@code admin_permissions} —— 权限表（主体，树形层级，parent_id 自引用）</li>
 *   <li>{@code admin_role} —— 管理员↔角色 中间表（pivot）</li>
 *   <li>{@code role_permission} —— 角色↔权限 中间表（pivot）</li>
 * </ul>
 * <p>
 * 类名日期晚于 {@code Migration_2024_01_02_CreateProductsTable}，按字典序排在其后执行。
 * 作为 Spring Bean（{@code @Component}）被 {@code Migrator} 自动发现。
 */
@Component
public class Migration_2024_01_03_CreateAdminRbacTables implements Migration {

    /**
     * 正向迁移：创建 RBAC 5 张表。
     * <p>
     * 先创建无依赖的主体表，再创建带树形自引用的权限表，最后创建两张中间表。
     */
    @Override
    public void up(Schema schema) {
        // 1. 管理员表
        schema.create("admins", table -> {
            table.id();
            table.string("username", 50).unique();
            table.string("password", 100);
            table.string("nickname", 50).nullable();
            table.string("description", 255).nullable();
            table.tinyInteger("status").defaultValue(1);
            table.timestamps();
        });

        // 2. 角色表
        schema.create("admin_roles", table -> {
            table.id();
            table.string("name", 50);
            table.string("code", 50).unique();
            table.string("description", 255).nullable();
            table.timestamps();
        });

        // 3. 权限表（树形层级：parent_id 自引用，null 为根节点；route 关联路由模式）
        schema.create("admin_permissions", table -> {
            table.id();
            table.string("name", 50);
            table.string("code", 80).unique();
            table.bigInteger("parent_id").unsigned().nullable();
            table.string("route", 200).nullable();
            table.string("description", 255).nullable();
            table.timestamps();
            table.index("parent_id");
        });

        // 4. 管理员↔角色 中间表
        schema.create("admin_role", table -> {
            table.id();
            table.bigInteger("admin_id").unsigned();
            table.bigInteger("role_id").unsigned();
            table.timestamps();
            table.unique("admin_id", "role_id");
        });

        // 5. 角色↔权限 中间表
        schema.create("role_permission", table -> {
            table.id();
            table.bigInteger("role_id").unsigned();
            table.bigInteger("permission_id").unsigned();
            table.timestamps();
            table.unique("role_id", "permission_id");
        });
    }

    /**
     * 回滚迁移：按与 up() 相反的顺序删除 5 张表，先删中间表再删主体表。
     */
    @Override
    public void down(Schema schema) {
        schema.dropIfExists("role_permission");
        schema.dropIfExists("admin_role");
        schema.dropIfExists("admin_permissions");
        schema.dropIfExists("admin_roles");
        schema.dropIfExists("admins");
    }
}
