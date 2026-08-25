package com.weacsoft.jaravel.config;

import com.weacsoft.jaravel.vendor.cache.CacheManager;
import com.weacsoft.jaravel.vendor.cache.CacheStore;
import com.weacsoft.jaravel.vendor.core.view.View;
import com.weacsoft.jaravel.vendor.jblade.BladeEngine;
import com.weacsoft.jaravel.vendor.jblade.PrecompiledTemplateLoader;
import com.weacsoft.jaravel.vendor.jblade.view.BladeView;
import com.weacsoft.jaravel.vendor.springboot.jblade.RegisterView;
import com.weacsoft.jaravel.vendor.utils.memory.MemoryClassLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 视图与静态资源配置（对齐 Laravel 的 Blade 模板引擎 + 静态资源）。
 * <p>
 * <b>新设计（对齐 auth/cache 等模块）：通过 {@link RegisterView} 声明式注册 {@link View} 实现。</b>
 * 框架只依赖 {@code View} 抽象，控制器用 {@code ResponseBuilder.view(...)} 渲染时，
 * 由 {@code ViewFacade} 自动取出当前激活的 View 实现，<b>无需手动 setBladeEngine</b>。
 * </p>
 *
 * <h3>三层优先级（声明 → 配置 → 默认）</h3>
 * <ol>
 *   <li><b>声明</b>：下方 {@code @RegisterView} 标注的 {@code @Bean} 提供 Blade 实现；</li>
 *   <li><b>配置</b>：{@code jaravel.view.default=xxx} 指定默认激活哪一个实现；</li>
 *   <li><b>默认</b>：未声明任何实现时，jblade 的 {@code ViewAutoConfiguration} 兜底注册 Blade。</li>
 * </ol>
 *
 * <h3>BladeEngine 运行模式</h3>
 * <ul>
 *   <li><b>预编译包模式</b>（默认，仅需 JRE）：从 classpath 资源加载预编译的 .jblade.zip，模板编译结果已打包进 JAR。</li>
 *   <li><b>运行时编译模式</b>：从 classpath 读取模板源码，运行时编译（需要 JDK，仅开发环境）。</li>
 * </ul>
 *
 * <h3>构建说明</h3>
 * <p>
 * Maven 构建时会自动预编译所有模板并打包为 templates.jblade.zip：
 * <pre>
 *   mvn package -P precompile-templates
 * </pre>
 * 该 profile 会执行 jblade 预编译器，生成预编译包并放入 classpath。
 * </p>
 */
@Configuration
public class ViewConfig {
    private static final Logger log = LoggerFactory.getLogger(ViewConfig.class);
    private static final String PRECOMPILED_ZIP = "templates.jblade.zip";

    /**
     * 预编译包模式（默认，仅需 JRE）：从 classpath 加载预编译的 .jblade.zip
     */
    @RegisterView(name = "blade", defaultView = true)
    @Bean
    public View bladeView(ObjectProvider<CacheManager> cacheManagerProvider,
                          @Value("${jaravel.view.template-dir:templates}") String templateDir,
                          @Value("${jaravel.view.suffix:.blade.java}") String suffix,
                          @Value("${jaravel.view.asset-url-prefix:/static}") String assetUrlPrefix) {
        CacheStore cacheStore = null;
        CacheManager cm = cacheManagerProvider.getIfAvailable();
        if (cm != null) {
            try {
                cacheStore = cm.store();
            } catch (Exception e) {
                log.warn("[view] 缓存模块未正确配置，编译缓存回退为内存");
            }
        }

        // 尝试从 classpath 加载预编译包（使用 ClassPathResource 确保 JAR 内资源正确读取）
        try {
            ClassPathResource cpr = new ClassPathResource(PRECOMPILED_ZIP);
            if (cpr.exists()) {
                log.info("[view] 使用预编译模板包: {}", PRECOMPILED_ZIP);
                byte[] zipBytes = cpr.getInputStream().readAllBytes();
                log.info("[view] 读取到 {} 字节预编译模板数据", zipBytes.length);
                PrecompiledTemplateLoader.PrecompiledBundle bundle =
                        PrecompiledTemplateLoader.loadBundleFromPackage(
                                new ByteArrayInputStream(zipBytes));
                MemoryClassLoader loader = new MemoryClassLoader(new ConcurrentHashMap<>(), BladeEngine.class.getClassLoader());
                BladeEngine engine = new BladeEngine(templateDir, suffix, cacheStore, loader);
                engine.populatePrecompiledBundle(bundle);
                return BladeView.precompiledPackage("blade", templateDir, suffix, engine, assetUrlPrefix);
            } else {
                log.warn("[view] 预编译包不存在: {}", PRECOMPILED_ZIP);
            }
        } catch (Exception e) {
            log.warn("[view] 预编译包加载失败，回退到运行时编译: {}", e.getMessage(), e);
        }

        // 运行时编译模式（需要 JDK）
        log.info("[view] 使用运行时编译模式: templateDir={}, suffix={}（需要 JDK）", templateDir, suffix);
        return BladeView.runtime("blade", templateDir, suffix, cacheStore, assetUrlPrefix);
    }

    /**
     * 静态资源配置（对齐原 StaticResourceConfig）。
     * <p>
     * 以 {@code @Bean} 暴露 {@link WebMvcConfigurer}，注册静态资源处理器：
     * <ul>
     *   <li>优先从文件系统 {@code ./public/} 加载</li>
     *   <li>回退到 {@code classpath:/static/}</li>
     * </ul>
     * 访问路径前缀为 {@code /static/**}。本 Bean 由 Spring MVC 容器自动消费（非业务注入）。
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
