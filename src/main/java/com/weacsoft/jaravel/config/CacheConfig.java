package com.weacsoft.jaravel.config;

import com.weacsoft.jaravel.vendor.cache.CacheStore;
import com.weacsoft.jaravel.vendor.cache.driver.ArrayCacheDriver;
import com.weacsoft.jaravel.vendor.cache.driver.FileCacheDriver;
import com.weacsoft.jaravel.vendor.cache.store.DefaultCacheStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 缓存配置，对齐 Laravel 的 {@code config/cache.php}。
 * <p>
 * 支持三种 Store 注册方式（优先级：Bean 声明 > 配置式 > 编程式）：
 * <ul>
 *   <li><b>配置式</b>：{@code application.yml} 的 {@code jaravel.cache.stores} 配置按需创建</li>
 *   <li><b>Bean 声明式</b>：在此类中用 {@code @Bean} 声明 CacheStore</li>
 *   <li><b>编程式</b>：通过 {@code CacheManager.addStore()} 手动注册</li>
 * </ul>
 *
 * <h3>Bean 声明式规则</h3>
 * <ul>
 *   <li>{@code @Bean("name")} 返回 {@link CacheStore} → 注册为名为 "name" 的额外 store</li>
 *   <li>{@code @Bean} 返回 {@link DefaultCacheStore}，方法名匹配 {@code default-store} → 默认 store</li>
 * </ul>
 * Bean 声明优先于配置式（同名时覆盖）。
 *
 * <h3>示例</h3>
 * <pre>
 * // 额外 store：@Bean("file") 返回 CacheStore → store "file"
 * &#64;Bean("file")
 * public CacheStore fileStore() {
 *     return new DefaultCacheStore(new FileCacheDriver("/tmp/cache"), "jaravel");
 * }
 *
 * // 默认 store：@Bean 返回 DefaultCacheStore，方法名 "array" 匹配 default-store="array"
 * &#64;Bean
 * public DefaultCacheStore array() {
 *     return new DefaultCacheStore(new ArrayCacheDriver(), "jaravel");
 * }
 * </pre>
 *
 * <h3>多 Store 使用</h3>
 * <pre>
 * Cache::put("key", "value", 60);               // 使用默认 store（array）
 * Cache::store("file").put("key", "value", 0);   // 使用 file store
 * </pre>
 */
@Configuration
public class CacheConfig {

    // ===== Bean 声明式 Store（演示）=====
    // 如果使用 Bean 声明式，取消下方注释即可覆盖配置式的同名 store。
    // 配置式（application.yml stores）和 Bean 声明式可共存，Bean 优先。

    // /**
    //  * 额外 store：file 缓存（跨重启持久化）。
    //  * @Bean("file") 返回 CacheStore → 注册为 store "file"，覆盖配置式同名 store。
    //  */
    // @Bean("file")
    // public CacheStore fileStore() {
    //     return new DefaultCacheStore(new FileCacheDriver(), "jaravel");
    // }

    // /**
    //  * 默认 store：array 内存缓存。
    //  * @Bean 返回 DefaultCacheStore，方法名 "array" 匹配 default-store="array" → 默认 store。
    //  */
    // @Bean
    // public DefaultCacheStore array() {
    //     return new DefaultCacheStore(new ArrayCacheDriver(), "jaravel");
    // }
}
