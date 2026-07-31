package com.weacsoft.jaravel.config;

import com.weacsoft.jaravel.vendor.storage.RegisterDisk;
import com.weacsoft.jaravel.vendor.storage.contract.DiskDefinition;
import com.weacsoft.jaravel.vendor.storage.contract.Visibility;
import org.springframework.context.annotation.Configuration;

/**
 * 文件存储配置，对齐 Laravel config/filesystems.php。
 * <p>
 * 由 {@code artisan vendor:publish --tag=storage} 发布生成，可自由修改。
 *
 * <h3>说明</h3>
 * <ul>
 *   <li>{@code @RegisterDisk} 注册的磁盘<b>不会</b>成为 Spring Bean，
 *       磁盘名称不会与容器内同名 bean 冲突。</li>
 *   <li>注解式注册优先于配置式（{@code jaravel.storage.disks}），同名时覆盖。</li>
 *   <li>删除本文件即可回退到框架默认（local 磁盘，根目录 storage/app）。</li>
 * </ul>
 */
@Configuration
public class StorageConfig {

    /**
     * 本地私有磁盘，根目录 {@code storage/app}。
     */
    @RegisterDisk(value = "local", defaultDisk = true)
    public DiskDefinition localDisk() {
        return DiskDefinition.local("storage/app");
    }

    /**
     * 公开磁盘，可通过 {@code /storage} 前缀访问。
     * <p>
     * 使用 {@code Storage.disk("public").url(path)} 生成访问地址。
     */
    @RegisterDisk("public")
    public DiskDefinition publicDisk() {
        return DiskDefinition.local("storage/app/public")
                .url("/storage")
                .visibility(Visibility.PUBLIC);
    }

    // 数据库磁盘：需要 DataSource 与 jaravel_files 表（见 storage 模块文档）
    // @RegisterDisk("database")
    // public DiskDefinition databaseDisk() {
    //     return DiskDefinition.of("database").with("table", "jaravel_files");
    // }
}
