package com.weacsoft.jaravel.config;

import com.weacsoft.jaravel.vendor.cache.CacheManager;
import com.weacsoft.jaravel.vendor.cache.CacheStore;
import com.weacsoft.jaravel.vendor.http.controller.response.ResponseBuilder;
import com.weacsoft.jaravel.vendor.jblade.BladeEngine;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 视图配置，对齐 Laravel 的 Blade 模板引擎。
 * <p>
 * 创建 {@link BladeEngine} 并注入到 {@link ResponseBuilder}，使
 * {@code ResponseBuilder.view(...)} 能够渲染 .blade.java 模板。
 * <p>
 * 模板目录为 classpath 下的 {@code templates/}（BladeCompiler 通过 ClassPathResource 加载）。
 * 使用 .blade.java 后缀，让常见 IDE 仍能识别为 Java 相关文件并提供代码提示。
 * <p>
 * <b>模板缓存</b>：通过 cache 模块的 {@link CacheStore} 缓存编译后的模板字节码，
 * 避免每次渲染都重新编译。cache 模块未引入时自动回退到内存缓存。
 */
@Configuration
public class View {

    @Bean
    public BladeEngine bladeEngine(ObjectProvider<CacheManager> cacheManagerProvider) {
        CacheManager cacheManager = cacheManagerProvider.getIfAvailable();
        CacheStore cacheStore = null;
        if (cacheManager != null) {
            try {
                cacheStore = cacheManager.store();
            } catch (Exception e) {
                // cache 模块未正确配置，回退到内存缓存
            }
        }
        try {
            return new BladeEngine("templates", ".blade.java", cacheStore);
        } catch (IllegalAccessError e) {
            // JDK 21 模块系统限制：BladeEngine 继承 ClassLoader 受限
            // 跳过模板引擎，API 接口不受影响
            return null;
        }
    }

    // 将 BladeEngine 设置到 ResponseBuilder（引擎可能为 null）
    @Bean
    public Object initBladeEngine(BladeEngine engine) {
        if (engine != null) {
            ResponseBuilder.setBladeEngine(engine);
        }
        return engine != null ? engine : new Object();
    }
}
