package com.weacsoft.jaravel.config;

import com.weacsoft.jaravel.vendor.cache.CacheStore;
import com.weacsoft.jaravel.vendor.cache.RegisterCacheStore;
import com.weacsoft.jaravel.vendor.cache.driver.ArrayCacheDriver;
import com.weacsoft.jaravel.vendor.cache.driver.FileCacheDriver;
import com.weacsoft.jaravel.vendor.cache.store.DefaultCacheStore;
import org.springframework.context.annotation.Configuration;

/**
 * 缓存配置（对齐 Laravel 的 {@code config/cache.php}）。
 *
 * <h2>一、三层优先级：声明 → 配置 → 默认</h2>
 * <ol>
 *   <li><b>声明</b>：本类中的 {@link RegisterCacheStore @RegisterCacheStore} 方法。
 *       同名时<b>覆盖</b>配置式；不注册为 Spring Bean，不会与同名 {@code @Bean} 冲突；
 *       方法参数按类型从 Spring 容器自动注入</li>
 *   <li><b>配置</b>：{@code application.yml} 的 {@code jaravel.cache.stores}（本项目采用）</li>
 *   <li><b>默认（兜底）</b>：一个 store 都没配 → 自动创建 {@code array}（内存）store</li>
 * </ol>
 * 另有<b>编程式</b>：运行期 {@code CacheManager.addStore()} 手动注册。
 *
 * <h2>二、"安装 ≠ 启用"：驱动按需装配</h2>
 * <ul>
 *   <li>{@code database} 驱动 —— 只有出现 {@code driver: database} 时才装配，
 *       此时才需要 {@code DataSource} 与 {@code jaravel_cache} 表。
 *       没用到就不会因为缺少 DataSource 而启动失败</li>
 *   <li>{@code redis} 驱动 —— 只有出现 {@code driver: redis} 时才装配，
 *       即使引入了 jaravel-redis 也不会主动连接</li>
 *   <li>只有<b>配置在 {@code stores} 里的 store 才会被创建</b>，
 *       驱动实例在 store 创建时才实例化（用到才构建）</li>
 * </ul>
 *
 * <h2>三、驱动一览</h2>
 * <table border="1">
 *   <caption>可用的 cache 驱动</caption>
 *   <tr><th>driver</th><th>依赖</th><th>说明</th></tr>
 *   <tr><td>{@code array}</td><td>cache（内置）</td><td><b>兜底默认</b>，进程内存，重启即失效</td></tr>
 *   <tr><td>{@code file}</td><td>cache（内置）</td><td>本地文件，跨重启持久化</td></tr>
 *   <tr><td>{@code database}</td><td>cache + DataSource</td><td>需 {@code artisan cache:table} 建表</td></tr>
 *   <tr><td>{@code redis}</td><td>jaravel-redis</td><td>多实例共享</td></tr>
 * </table>
 *
 * <h2>四、多 Store 用法</h2>
 * <pre>
 * Cache.put("key", "value", 60);                // 默认 store（本项目 = array）
 * Cache.store("file").put("key", "value", 0);   // 指定 file store
 * </pre>
 * 各模块也可通过独立配置项指定自己的 store，见 {@code application.yml}：
 * {@code jaravel.wechat.cache-store} / {@code jaravel.model-cache.store} / {@code jaravel.jwt.blacklist-store}。
 */
@Configuration
public class CacheConfig {

    // =====================================================================
    // 方式一（本项目采用）：配置式 —— application.yml
    // =====================================================================
    // jaravel:
    //   cache:
    //     default-store: array
    //     prefix: jaravel
    //     stores:
    //       array:    { driver: array }
    //       file:     { driver: file, dir: "" }             # 空则用系统临时目录
    //       database: { driver: database, table: jaravel_cache }
    //       # redis:  { driver: redis, connection: default }  # 需引入 jaravel-redis

    // =====================================================================
    // 方式二：注解声明式 —— 优先级最高，同名覆盖配置式
    // =====================================================================
    // 取消注释即可覆盖 application.yml 中的同名 store。
    // 注解式与配置式可共存，注解优先。

    // /**
    //  * 默认 store：array 内存缓存。
    //  * defaultStore = true 会自动设为默认 store（等价于 yml 的 default-store）。
    //  */
    // @RegisterCacheStore(value = "array", defaultStore = true)
    // public CacheStore arrayStore() {
    //     return new DefaultCacheStore(new ArrayCacheDriver(), "jaravel");
    // }

    // /**
    //  * 额外 store：file 缓存（跨重启持久化）。
    //  * 方法参数 CacheProperties 由 Spring 容器按类型注入。
    //  */
    // @RegisterCacheStore("file")
    // public CacheStore fileStore(CacheProperties properties) {
    //     return new DefaultCacheStore(new FileCacheDriver(properties.getFileDir()),
    //             properties.getPrefix());
    // }

    // /**
    //  * 额外 store：database 缓存。
    //  * 需先 artisan cache:table 建表；没有 DataSource 时不要注册本方法。
    //  */
    // @RegisterCacheStore("database")
    // public CacheStore databaseStore(DataSource dataSource) {
    //     return new DefaultCacheStore(new DatabaseCacheDriver(dataSource, "jaravel_cache"), "jaravel");
    // }

    // =====================================================================
    // 方式三：自定义驱动 —— 不改核心代码
    // =====================================================================
    // 实现 CacheDriver 接口，包一层 DefaultCacheStore 即可：
    //
    // @RegisterCacheStore("mongo")
    // public CacheStore mongoStore(MongoTemplate mongo) {
    //     return new DefaultCacheStore(new MongoCacheDriver(mongo), "jaravel");
    // }

    // =====================================================================
    // 方式四：编程式 —— 运行期动态注册（插件 / 多租户场景）
    // =====================================================================
    // @Autowired
    // public void registerTenantStore(CacheManager manager) {
    //     manager.addStore("tenant", new DefaultCacheStore(new ArrayCacheDriver(), "tenant"));
    // }

    // =====================================================================
    // 方式五：兜底默认 —— 什么都不写
    // =====================================================================
    // 删除本类、且 application.yml 里不写 jaravel.cache 时，
    // 框架自动创建 array（内存）store 作为默认 store，保证 Cache 门面可用。
}
