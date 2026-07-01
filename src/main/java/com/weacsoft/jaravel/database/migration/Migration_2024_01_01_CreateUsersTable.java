package com.weacsoft.jaravel.database.migration;

import com.weacsoft.jaravel.vendor.migration.Migration;
import com.weacsoft.jaravel.vendor.migration.Schema;
import org.springframework.stereotype.Component;

/**
 * 迁移：创建 users 表与 user_profiles 表（演示一次 up() 处理多张表）。
 * <p>
 * 类名采用 {@code Migration_YYYY_MM_DD_PascalCaseDescription} 约定，
 * {@link Migration#getName()} 默认返回类名 {@code "Migration_2024_01_01_CreateUsersTable"}，
 * {@code Migrator} 按类名字典序排序即可保证执行顺序，无需额外时间戳字段。
 * <p>
 * <b>多表演示</b>：本迁移的 {@code up()} 一次性创建 {@code users} 与 {@code user_profiles} 两张表，
 * {@code down()} 对称地删除两张表。这展示了「一个 up/down 可处理多张表」的能力，
 * 而非 Laravel 默认的「一个迁移只处理一张表」。
 * <p>
 * 作为 Spring Bean（{@code @Component}）被 {@code Migrator} 自动发现。
 */
@Component
public class Migration_2024_01_01_CreateUsersTable implements Migration {

    /**
     * 正向迁移：创建 users 表与 user_profiles 表。
     * <p>
     * 一次 up() 可连续调用多次 {@code schema.create()} 处理多张关联表，
     * 比拆成多个迁移更利于保持相关表结构的原子性。
     */
    @Override
    public void up(Schema schema) {
        // 第一张表：users
        schema.create("users", table -> {
            table.id();
            table.string("name", 50);
            table.string("number", 50).unique();
            table.string("password", 100);
            table.string("email", 50).nullable();
            table.timestamps();
        });

        // 第二张表：user_profiles（与 users 一对一，演示多表迁移）
        schema.create("user_profiles", table -> {
            table.id();
            table.bigInteger("user_id").unsigned();
            table.string("nickname", 50).nullable();
            table.string("avatar", 255).nullable();
            table.text("bio").nullable();
            table.timestamps();
        });
    }

    /**
     * 回滚迁移：删除 user_profiles 与 users 表。
     * <p>
     * down() 应按与 up() 相反的顺序删除所有在 up() 中创建的表，
     * 先删依赖方（user_profiles）再删被依赖方（users）。
     */
    @Override
    public void down(Schema schema) {
        schema.dropIfExists("user_profiles");
        schema.dropIfExists("users");
    }
}
