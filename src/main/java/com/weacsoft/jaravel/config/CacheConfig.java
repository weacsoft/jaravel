package com.weacsoft.jaravel.config;

import com.weacsoft.jaravel.vendor.cache.CacheStore;
import com.weacsoft.jaravel.vendor.cache.RegisterCacheStore;
import com.weacsoft.jaravel.vendor.cache.driver.ArrayCacheDriver;
import com.weacsoft.jaravel.vendor.cache.driver.FileCacheDriver;
import com.weacsoft.jaravel.vendor.cache.store.DefaultCacheStore;
import org.springframework.context.annotation.Configuration;

/**
 * 缓存配置，对齐 Laravel 的 {@code config/cache.php}。
 * <p>
 * 支持三种 Store 注册方式（优先级：注解声明 > 配置式 > 编程式）：
 * <ul>
 *   <li><b>配置式</b>：{@code application.yml} 的 {@code jaravel.cache.stores} 配置按需创建</li>
 *   <li><b>注解声明式</b>：在此类中用 {@link RegisterCacheStore @RegisterCacheStore} 注解方法，返回 {@link CacheStore}</li>
 *   <li><b>编程式</b>：通过 {@code CacheManager.addStore()} 手动注册</li>
 * </ul>
 *
 * <h3>注解声明式规则</h3>
 * <ul>
 *   <li>{@code @RegisterCacheStore("name")} 返回 {@link CacheStore} → 注册为名为 "name" 的额外 store</li>
 *   <li>不注册为 Spring Bean，因此不会与同名 {@code @Bean} 冲突</li>
 *   <li>方法参数从 Spring 容器按类型自动注入</li>
 * </ul>
 * 注解声明优先于配置式（同名时覆盖）。
 *
 * <h3>示例</h3>
 * <pre>
 * // 额外 store：@RegisterCacheStore("file") → store "file"
 * // 不会注册为 Spring Bean，不会与其他 @Bean("file") 冲突
 * &#64;RegisterCacheStore("file")
 * public CacheStore fileStore(CacheProperties properties) {
 *     return new DefaultCacheStore(new FileCacheDriver(properties.getFileDir()),
 *             properties.getPrefix());
 * }
 *
 * // 默认 store：标记 defaultStore = true，自动设为默认 store
 * &#64;RegisterCacheStore(value = "array", defaultStore = true)
 * public DefaultCacheStore arrayStore() {
 *     return new DefaultCacheStore(new ArrayCacheDriver(), "jaravel");
 * }
 * </pre>
 *
 * <h3>多 Store 使用</h3>
 * <pre>
 * Cache.put("key", "value", 60);               // 使用默认 store（array）
 * Cache.store("file").put("key", "value", 0);   // 使用 file store
 * </pre>
 */
@Configuration
public class CacheConfig {

    // ===== 注解声明式 Store（演示）=====
    // 如果使用注解声明式，取消下方注释即可覆盖配置式的同名 store。
    // 配置式（application.yml stores）和注解声明式可共存，注解优先。

    // /**
    //  * 额外 store：file 缓存（跨重启持久化）。
    //  * @RegisterCacheStore("file") → 注册为 store "file"，覆盖配置式同名 store。
    //  * 不会注册为 Spring Bean，因此不会与其他 @Bean("file") 冲突。
    //  */
    // @RegisterCacheStore("file")
    // public CacheStore fileStore(CacheProperties properties) {
    //     return new DefaultCacheStore(new FileCacheDriver(properties.getFileDir()),
    //             properties.getPrefix());
    // }

    // /**
    //  * 默认 store：array 内存缓存。
    //  * @RegisterCacheStore(value = "array", defaultStore = true) → 自动设为默认 store。
    //  */
    // @RegisterCacheStore(value = "array", defaultStore = true)
    // public DefaultCacheStore arrayStore() {
    //     return new DefaultCacheStore(new ArrayCacheDriver(), "jaravel");
    // }
}
