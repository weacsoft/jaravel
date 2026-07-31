package com.weacsoft.jaravel.database.migration;

import com.weacsoft.jaravel.vendor.migration.Blueprint;
import com.weacsoft.jaravel.vendor.migration.Migration;
import com.weacsoft.jaravel.vendor.migration.MigrationAnnotation;
import com.weacsoft.jaravel.vendor.migration.Schema;

/**
 * 创建数据库文件存储所需的表，供 {@code driver: database} 的磁盘使用。
 * <p>
 * 说明：即使不执行此迁移，磁盘首次使用时也会通过 {@code CREATE TABLE IF NOT EXISTS} 自动建表。
 * 本迁移仅为显式管理表结构（例如用于独立文件数据库或版本回溯）。
 *
 * <h3>列名与类型</h3>
 * 文件内容统一存放在单列（默认列名 {@code content}），列类型由 {@code binary} 开关决定：
 * {@code binary=true} 时为二进制（BLOB），否则为 base64 文本（LONGTEXT）。本迁移默认使用与
 * driver 默认配置一致的 {@code content} 二进制列；若磁盘配置了不同的 {@code content-column}
 * 或 {@code binary=false}，请勿直接执行本迁移，应自行调整建表语句以保持一致。
 *
 * <ul>
 *   <li><b>storage_file</b> — 文件元信息，主键 (disk, path)。</li>
 *   <li><b>storage_file_chunk</b> — 文件内容分片，主键 (disk, path, chunk_index)。</li>
 * </ul>
 */
@MigrationAnnotation
public class Migration_2026_07_31_CreateStorageFileTables implements Migration {

    @Override
    public void up(Schema schema) {
        schema.create("storage_file", table -> {
            table.string("disk", 64).notNull().primary();
            table.string("path", 1024).notNull().primary();
            table.string("visibility", 16).notNull().defaultValue("private");
            table.string("mime_type", 255).nullable();
            table.bigInteger("size").notNull().defaultValue(0);
            table.integer("chunk_count").notNull().defaultValue(0);
            table.bigInteger("created_at").nullable();
            table.bigInteger("updated_at").nullable();
        });

        schema.create("storage_file_chunk", table -> {
            table.string("disk", 64).notNull().primary();
            table.string("path", 1024).notNull().primary();
            table.integer("chunk_index").notNull().primary();
            table.binary("content").nullable();
            table.integer("size").notNull().defaultValue(0);
            table.bigInteger("created_at").nullable();
            table.bigInteger("updated_at").nullable();
        });
    }

    @Override
    public void down(Schema schema) {
        schema.dropIfExists("storage_file_chunk");
        schema.dropIfExists("storage_file");
    }
}
