package com.weacsoft.jaravel.config.view;

import com.weacsoft.jaravel.vendor.cache.CacheManager;
import com.weacsoft.jaravel.vendor.cache.CacheStore;
import com.weacsoft.jaravel.vendor.http.controller.response.ResponseBuilder;
import com.weacsoft.jaravel.vendor.jblade.BladeEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 视图配置，对齐 Laravel 的 Blade 模板引擎。
 * <p>
 * 创建 {@link BladeEngine} 并注入到 {@link ResponseBuilder}，使
 * {@code ResponseBuilder.view(...)} 能够渲染 .blade.java 模板。
 * <p>
 * 支持两种运行模式：
 * <ul>
 *   <li><b>运行时编译模式</b>（默认）：从 classpath 读取模板源码，运行时编译（需要 JDK）。
 *       配置 {@code jaravel.view.precompiled-mode=false}</li>
 *   <li><b>预编译模式</b>：从预编译的打包文件或 class 目录加载（仅需 JRE）。
 *       配置 {@code jaravel.view.precompiled-mode=true}，
 *       通过 {@code jaravel.view.precompiled-package} 或 {@code jaravel.view.precompiled-classes-dir} 指定路径</li>
 * </ul>
 * <p>
 * 模板目录为 classpath 下的 {@code templates/}（BladeCompiler 通过 ClassPathResource 加载）。
 * 使用 .blade.java 后缀，让常见 IDE 仍能识别为 Java 相关文件并提供代码提示。
 * <p>
 * <b>模板缓存</b>：通过 cache 模块的 {@link CacheStore} 缓存编译后的模板字节码，
 * 避免每次渲染都重新编译。cache 模块未引入时自动回退到内存缓存。
 */
@Configuration
public class ViewConfig {
    private static final Logger log = LoggerFactory.getLogger(ViewConfig.class);

    @Bean
    public BladeEngine bladeEngine(ObjectProvider<CacheManager> cacheManagerProvider,
                                   @Value("${jaravel.view.precompiled-mode:false}") boolean precompiledMode,
                                   @Value("${jaravel.view.precompiled-package:}") String precompiledPackage,
                                   @Value("${jaravel.view.precompiled-classes-dir:}") String precompiledClassesDir,
                                   @Value("${jaravel.view.template-dir:templates}") String templateDir,
                                   @Value("${jaravel.view.suffix:.blade.java}") String suffix) {
        // 预编译模式：从打包文件或 class 目录加载（仅需 JRE）
        if (precompiledMode) {
            try {
                if (precompiledPackage != null && !precompiledPackage.isEmpty()) {
                    log.info("[view] 使用预编译打包文件: {}", precompiledPackage);
                    return BladeEngine.fromPrecompiledPackage(precompiledPackage);
                } else if (precompiledClassesDir != null && !precompiledClassesDir.isEmpty()) {
                    log.info("[view] 使用预编译 class 目录: {}", precompiledClassesDir);
                    return BladeEngine.fromPrecompiledClasses(precompiledClassesDir);
                } else {
                    log.warn("[view] 预编译模式已启用，但未指定 precompiled-package 或 precompiled-classes-dir，回退到运行时编译模式");
                }
            } catch (Exception e) {
                log.error("[view] 加载预编译模板失败，回退到运行时编译模式", e);
            }
        }

        // 运行时编译模式（需要 JDK）
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
            log.info("[view] 使用运行时编译模式: templateDir={}, suffix={}", templateDir, suffix);
            return new BladeEngine(templateDir, suffix, cacheStore);
        } catch (IllegalAccessError e) {
            // JDK 21 模块系统限制：BladeEngine 继承 ClassLoader 受限
            // 跳过模板引擎，API 接口不受影响
            log.warn("[view] BladeEngine 初始化失败（JDK 模块系统限制），模板功能不可用");
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
