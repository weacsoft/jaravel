package com.weacsoft.jaravel.database.migration;

import com.weacsoft.jaravel.vendor.migration.Migration;
import com.weacsoft.jaravel.vendor.migration.MigrationAnnotation;
import com.weacsoft.jaravel.vendor.migration.Schema;

/**
 * 迁移：创建 users 表。
 * <p>
 * 类名采用 {@code Migration_YYYY_MM_DD_PascalCaseDescription} 约定，
 * {@code Migrator} 按类名字典序排序即可保证执行顺序。
 * <p>
 * users 表包含 model_shadow 列，因为 gaarason 的 ModelBase 父类声明了 modelShadow 字段
 * （无 @Column 注解，默认 inDatabase=true），gaarason 的字段扫描会将其加入 SELECT 列表。
 * 即使 BaseModel 通过字段隐藏标注 @Column(inDatabase=false)，父类的字段仍会被扫描到。
 */
@MigrationAnnotation
public class Migration_2024_01_01_CreateUsersTable implements Migration {

    @Override
    public void up(Schema schema) {
        schema.create("users", table -> {
            table.id();
            table.string("name", 50);
            table.string("number", 50).unique();
            table.string("password", 100);
            table.string("email", 50).nullable();
            table.string("description", 255).nullable();
            table.text("model_shadow").nullable();
            table.timestamps();
        });
    }

    @Override
    public void down(Schema schema) {
        schema.dropIfExists("users");
    }
}
