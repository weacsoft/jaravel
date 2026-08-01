package com.weacsoft.jaravel.config;

import com.weacsoft.jaravel.vendor.storage.RegisterDisk;
import com.weacsoft.jaravel.vendor.storage.contract.DiskDefinition;
import com.weacsoft.jaravel.vendor.storage.contract.Visibility;
import org.springframework.context.annotation.Configuration;

/**
 * 文件存储配置（对齐 Laravel 的 {@code config/filesystems.php}）。
 * <p>
 * 由 {@code artisan vendor:publish --tag=storage} 发布生成，可自由修改。
 *
 * <h2>一、三层优先级：声明 → 配置 → 默认</h2>
 * <ol>
 *   <li><b>声明</b>：本类中的 {@link RegisterDisk @RegisterDisk} 方法。
 *       同名时<b>覆盖</b>配置式；不会成为 Spring Bean，磁盘名不会与同名 bean 冲突</li>
 *   <li><b>配置</b>：{@code application.yml} 的 {@code jaravel.storage.disks}（本项目主要用这个）</li>
 *   <li><b>默认（兜底）</b>：磁盘写了但没写 {@code driver} → 兜底为 {@code local}；
 *       整个 storage 都没配 → 默认 local 磁盘，根目录 {@code storage/app}</li>
 * </ol>
 *
 * <h2>二、"安装 ≠ 启用"：驱动按需装配</h2>
 * <ul>
 *   <li>{@code LocalFilesystemDriver} — 条件 {@code OnLocalDiskDriverCondition}：
 *       有磁盘声明 {@code driver: local/public}，<b>或</b>一个磁盘 driver 都没写（兜底）时装配</li>
 *   <li>{@code DatabaseFilesystemDriver} — 条件 {@code OnDatabaseDiskDriverCondition}：
 *       <b>严格按需</b>，必须显式出现 {@code driver: database} 才装配，
 *       此时才需要 {@code DataSource} 与 {@code storage_file} 系列表</li>
 * </ul>
 *
 * <h2>三、驱动一览</h2>
 * <table border="1">
 *   <caption>可用的 storage 驱动</caption>
 *   <tr><th>driver</th><th>依赖</th><th>说明</th></tr>
 *   <tr><td>{@code local}</td><td>storage（内置）</td><td><b>兜底默认</b>，私有本地目录</td></tr>
 *   <tr><td>{@code public}</td><td>storage（内置）</td><td>本地目录 + URL 前缀，可被 Web 访问</td></tr>
 *   <tr><td>{@code database}</td><td>storage + DataSource</td><td>文件存库，多机共享同一数据库即同步</td></tr>
 * </table>
 *
 * <h2>四、用法</h2>
 * <pre>
 * Storage.put("a.txt", bytes);              // 默认磁盘（本项目 = db）
 * Storage.disk("public").url("logo.png");   // 指定磁盘并取访问地址
 * </pre>
 */
@Configuration
public class StorageConfig {

    // =====================================================================
    // 方式一（本项目采用）：注解声明式 —— 优先级最高
    // =====================================================================

    /**
     * 本地私有磁盘，根目录 {@code storage/app}。
     * <p>
     * 声明了 {@code local} 驱动，{@code LocalFilesystemDriver} 因此被装配。
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

    // ---- 数据库磁盘（本项目改由 application.yml 的 disks.db 声明）----
    // 需要 DataSource 与 storage_file / storage_file_chunk 表（见 storage 模块文档）。
    // 只要出现 driver: database，DatabaseFilesystemDriver 才会装配。
    //
    // @RegisterDisk("db")
    // public DiskDefinition databaseDisk() {
    //     return DiskDefinition.of("database")
    //             .with("binary", true)             // true=BLOB；false=base64 文本
    //             .with("chunk-size", 1048576)      // 分片字节上限，0=整文件单条
    //             .with("table-prefix", "storage_");
    // }

    // =====================================================================
    // 方式二：兜底默认 —— 写了磁盘但不写 driver
    // =====================================================================
    // 不指定 driver 时自动兜底为 local：
    //
    // @RegisterDisk("tmp")
    // public DiskDefinition tmpDisk() {
    //     return DiskDefinition.of(null).with("root", "storage/tmp");  // driver 为空 -> local
    // }

    // =====================================================================
    // 方式三：配置式 —— application.yml（本项目的 db 磁盘就用这种）
    // =====================================================================
    // jaravel:
    //   storage:
    //     enabled: true
    //     default-disk: db
    //     disks:
    //       db:
    //         driver: database
    //         visibility: private
    //         options:
    //           binary: true
    //           chunk-size: 1048576
    //           table-prefix: storage_
    //       local:  { driver: local,  root: storage/app,        visibility: private }
    //       public: { driver: public, root: storage/app/public, url: /storage, visibility: public }
    //       # 不写 driver 时兜底为 local：
    //       # tmp:  { root: storage/tmp }

    // =====================================================================
    // 方式四：自定义驱动 —— 不改核心代码（如 S3 / OSS / MinIO）
    // =====================================================================
    // 实现 FilesystemDriver 并声明它 support 的驱动名，之后磁盘就能写 driver: oss：
    //
    // @RegisterFilesystemDriver("oss")
    // public FilesystemDriver ossDriver(OssClient client) {
    //     return new OssFilesystemDriver(client);
    // }
    //
    // @RegisterDisk("oss")
    // public DiskDefinition ossDisk() {
    //     return DiskDefinition.of("oss").with("bucket", "my-bucket");
    // }

    // =====================================================================
    // 方式五：全部删除 —— 回退框架默认
    // =====================================================================
    // 删除本文件且不配 jaravel.storage 时，框架默认提供 local 磁盘（根目录 storage/app）。
}
