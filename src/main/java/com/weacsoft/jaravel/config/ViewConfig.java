package com.weacsoft.jaravel.config;

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
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.File;

/**
 * 视图与静态资源配置（对齐 Laravel 的 Blade 模板引擎 + 静态资源）。
 * <p>
 * 包含两部分：
 * <ul>
 *   <li><b>BladeEngine</b>：创建模板引擎并注入到 {@link ResponseBuilder}，使
 *       {@code ResponseBuilder.view(...)} 能够渲染 .blade.java 模板</li>
 *   <li><b>静态资源</b>：支持从文件系统 {@code ./public/} 目录优先加载静态资源，
 *       回退到 ClassPath {@code classpath:/static/}</li>
 * </ul>
 *
 * <h3>BladeEngine 运行模式</h3>
 * <ul>
 *   <li><b>运行时编译模式</b>（默认）：从 classpath 读取模板源码，运行时编译（需要 JDK）。
 *       配置 {@code jaravel.view.precompiled-mode=false}</li>
 *   <li><b>预编译模式</b>：从预编译的打包文件或 class 目录加载（仅需 JRE）。
 *       配置 {@code jaravel.view.precompiled-mode=true}</li>
 * </ul>
 * <p>
 * 模板目录为 classpath 下的 {@code templates/}，使用 .blade.java 后缀。
 *
 * <h3>静态资源</h3>
 * 支持在不重新打包 JAR 的情况下，通过在运行目录下放置 {@code public/} 文件夹
 * 来覆盖或新增前端资源。资源访问路径前缀为 {@code /static/**}。
 */
@Configuration
public class ViewConfig {
    private static final Logger log = LoggerFactory.getLogger(ViewConfig.class);

    /**
     * BladeEngine Bean。
     * <p>
     * 根据配置选择运行时编译或预编译模式，并注入到 {@link ResponseBuilder}。
     * 模板缓存通过 cache 模块的 {@link CacheStore} 缓存编译后的字节码。
     */
    @Bean
    public BladeEngine bladeEngine(ObjectProvider<CacheManager> cacheManagerProvider,
                                   @Value("${jaravel.view.precompiled-mode:false}") boolean precompiledMode,
                                   @Value("${jaravel.view.precompiled-package:}") String precompiledPackage,
                                   @Value("${jaravel.view.precompiled-classes-dir:}") String precompiledClassesDir,
                                   @Value("${jaravel.view.template-dir:templates}") String templateDir,
                                   @Value("${jaravel.view.suffix:.blade.java}") String suffix) {
        // 预编译模式
        if (precompiledMode) {
            try {
                if (precompiledPackage != null && !precompiledPackage.isEmpty()) {
                    log.info("[view] 使用预编译打包文件: {}", precompiledPackage);
                    return BladeEngine.fromPrecompiledPackage(precompiledPackage);
                } else if (precompiledClassesDir != null && !precompiledClassesDir.isEmpty()) {
                    log.info("[view] 使用预编译 class 目录: {}", precompiledClassesDir);
                    return BladeEngine.fromPrecompiledClasses(precompiledClassesDir);
                } else {
                    log.warn("[view] 预编译模式已启用，但未指定路径，回退到运行时编译模式");
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
            log.warn("[view] BladeEngine 初始化失败（JDK 模块系统限制），模板功能不可用");
            return null;
        }
    }

    /**
     * 将 BladeEngine 设置到 ResponseBuilder（引擎可能为 null）。
     */
    @Bean
    public Object initBladeEngine(BladeEngine engine) {
        if (engine != null) {
            ResponseBuilder.setBladeEngine(engine);
        }
        return engine != null ? engine : new Object();
    }

    /**
     * 静态资源配置（对齐原 StaticResourceConfig）。
     * <p>
     * 以 {@code @Bean} 暴露 {@link WebMvcConfigurer}，注册静态资源处理器：
     * <ul>
     *   <li>优先从文件系统 {@code ./public/} 加载</li>
     *   <li>回退到 {@code classpath:/static/}</li>
     * </ul>
     * 访问路径前缀为 {@code /static/**}。
     */
    @Bean
    public WebMvcConfigurer staticResourceConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addResourceHandlers(ResourceHandlerRegistry registry) {
                File publicDir = new File("public");
                boolean hasPublicDir = publicDir.isDirectory();

                if (hasPublicDir) {
                    registry.addResourceHandler("/static/**")
                            .addResourceLocations("file:./public/", "classpath:/static/");
                    log.info("[static] 静态资源配置: /static/** -> file:./public/ + classpath:/static/");
                } else {
                    registry.addResourceHandler("/static/**")
                            .addResourceLocations("classpath:/static/");
                    log.info("[static] 静态资源配置: /static/** -> classpath:/static/");
                }
            }
        };
    }
}
