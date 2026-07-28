package com.weacsoft.jaravel.config;

import com.weacsoft.jaravel.vendor.cache.CacheManager;
import com.weacsoft.jaravel.vendor.cache.CacheStore;
import org.springframework.context.annotation.Configuration;

/**
 * 缓存配置，对齐 Laravel 的 {@code config/cache.php}。
 * <p>
 * 采用<b>工厂模式 + 按需创建</b>（对齐 Laravel {@code CacheManager}）：
 * <ul>
 *   <li>缓存模块内置 array、file、database 驱动工厂，启动时自动注册</li>
 *   <li>CacheManager 根据 {@code jaravel.cache.stores} 配置按需创建 Store</li>
 *   <li>只有配置在 stores 中的 Store 才会被创建，驱动实例在创建 Store 时才实例化</li>
 *   <li>stores 为空时只创建 default-store 对应的默认 Store（默认 array）</li>
 * </ul>
 *
 * <h3>配置式（推荐，对齐 Laravel stores 数组）</h3>
 * 在 {@code application.yml} 中配置即可，无需写 Java 代码：
 * <pre>
 * jaravel:
 *   cache:
 *     default-store: array
 *     prefix: jaravel
 *     stores:
 *       array:
 *         driver: array
 *       file:
 *         driver: file
 *         dir: /tmp/jaravel-cache
 *       database:
 *         driver: database
 *         table: jaravel_cache
 * </pre>
 * 只有 array、file、database 三个 Store 会被创建，其他驱动不会实例化。
 *
 * <h3>编程式注册（可选）</h3>
 * 如需注册自定义 Store，可在此类中注入 {@link CacheManager} 并调用 {@code addStore}：
 * <pre>
 * &#64;Bean
 * public ApplicationRunner customCacheStore(CacheManager cacheManager) {
 *     return args -> {
 *         // 注册自定义 Store（优先于配置式，同名时覆盖）
 *         CacheStore myStore = new DefaultCacheStore(new MyCacheDriver(), "jaravel");
 *         cacheManager.addStore("myStore", myStore);
 *     };
 * }
 * </pre>
 *
 * <h3>多 Store 使用</h3>
 * <pre>
 * Cache::put("key", "value", 60);               // 使用默认 store（array）
 * Cache::store("file").put("key", "value", 0);   // 使用 file store
 * Cache::store("database").put("key", "value", 0); // 使用 database store
 * </pre>
 *
 * <h3>各模块独立指定 Store</h3>
 * 各模块通过独立配置项指定使用的 cache store，空串表示使用 cache 模块的默认 store：
 * <ul>
 *   <li>{@code jaravel.wechat.cache-store=file} — wechat 模块使用 file store</li>
 *   <li>{@code jaravel.model-cache.store=database} — model-cache 模块使用 database store</li>
 *   <li>{@code jaravel.jwt.blacklist-store=} — jwt 模块使用默认 store</li>
 * </ul>
 * 注意：指定的 store 必须在 {@code stores} 中配置或通过编程式注册，否则会抛出
 * {@code "未注册的缓存 store"} 异常。
 */
@Configuration
public class CacheConfig {
    // 默认无需任何代码：CacheAutoConfiguration 会根据 application.yml 的
    // jaravel.cache.stores 配置按需创建 Store。
    //
    // 如需编程式注册自定义 Store，取消以下注释：
    //
    // @Bean
    // public ApplicationRunner customCacheStore(CacheManager cacheManager) {
    //     return args -> {
    //         CacheStore myStore = new DefaultCacheStore(new MyCacheDriver(), "jaravel");
    //         cacheManager.addStore("myStore", myStore);
    //     };
    // }
}
