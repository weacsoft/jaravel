package com.weacsoft.jaravel.database.migration;

import com.weacsoft.jaravel.vendor.migration.Migration;
import com.weacsoft.jaravel.vendor.migration.MigrationAnnotation;
import com.weacsoft.jaravel.vendor.migration.Schema;

/**
 * 迁移：创建 tasks 表（Wire 列表演示用）。
 * <p>
 * 基于 SQLite 真实数据库，替代原来的内存 Map 演示。
 */
@MigrationAnnotation
public class Migration_2026_08_04_CreateTasksTable implements Migration {

    @Override
    public void up(Schema schema) {
        schema.create("tasks", table -> {
            table.id();
            table.string("name", 100);
            table.booleanColumn("done").defaultValue(false);
            table.text("model_shadow").nullable();
            table.timestamps();
        });
    }

    @Override
    public void down(Schema schema) {
        schema.dropIfExists("tasks");
    }
}
