package com.weacsoft.jaravel.database.migration;

import com.weacsoft.jaravel.vendor.migration.Migration;
import com.weacsoft.jaravel.vendor.migration.Schema;
import org.springframework.stereotype.Component;

/**
 * 迁移：创建 products 表与 product_categories 表（演示多表迁移 + 多数据源）。
 * <p>
 * 类名采用 {@code Migration_YYYY_MM_DD_PascalCaseDescription} 约定，
 * {@link Migration#getName()} 默认返回类名 {@code "Migration_2024_01_02_CreateProductsTable"}，
 * 日期晚于 {@code Migration_2024_01_01_CreateUsersTable}，故排序在其后执行。
 * <p>
 * <b>多表演示</b>：本迁移的 {@code up()} 一次性创建 {@code product_categories} 与 {@code products} 两张表，
 * {@code down()} 对称删除。
 * <p>
 * <b>多数据源</b>：通过 {@link #getDataSourceName()} 指定使用第二数据源（secondaryDataSource），
 * 这两张表将创建在 database2.sqlite 中，用于多数据库测试。
 * <p>
 * 作为 Spring Bean（{@code @Component}）被 {@code Migrator} 自动发现。
 */
@Component
public class Migration_2024_01_02_CreateProductsTable implements Migration {

    /**
     * 指定使用第二数据源执行此迁移，对齐 Laravel 迁移的 {@code $connection} 属性。
     * 本迁移创建的表将位于 database2.sqlite 中。
     * 注意：此方法为演示用途，当前 Migrator 尚未实现多数据源迁移调度，
     * 实际执行时仍使用默认数据源。
     */
    public String getDataSourceName() {
        return "secondaryGaarasonDataSource";
    }

    /**
     * 正向迁移：创建 product_categories 与 products 两张关联表。
     * <p>
     * 先创建分类表再创建商品表（商品引用分类），保证外键依赖顺序正确。
     */
    @Override
    public void up(Schema schema) {
        // 第一张表：product_categories（分类表）
        schema.create("product_categories", table -> {
            table.id();
            table.string("name", 100);
            table.string("code", 50).unique();
            table.timestamps();
        });

        // 第二张表：products（商品表，引用分类）
        schema.create("products", table -> {
            table.id();
            table.string("name");
            table.bigInteger("category_id").unsigned().nullable();
            table.doubleColumn("price");
            table.timestamps();
        });
    }

    /**
     * 回滚迁移：删除 products 与 product_categories 表。
     * <p>
     * 按与 up() 相反的顺序删除，先删依赖方（products）再删被依赖方（product_categories）。
     */
    @Override
    public void down(Schema schema) {
        schema.dropIfExists("products");
        schema.dropIfExists("product_categories");
    }
}
