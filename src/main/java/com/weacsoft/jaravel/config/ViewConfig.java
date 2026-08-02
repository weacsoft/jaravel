package com.weacsoft.jaravel.config;

import com.weacsoft.jaravel.vendor.cache.CacheManager;
import com.weacsoft.jaravel.vendor.cache.CacheStore;
import com.weacsoft.jaravel.vendor.core.view.View;
import com.weacsoft.jaravel.vendor.jblade.view.BladeView;
import com.weacsoft.jaravel.vendor.jblade.view.RegisterView;
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
 *   <li><b>运行时编译模式</b>（默认）：从 classpath 读取模板源码，运行时编译（需要 JDK）。</li>
 *   <li><b>预编译模式</b>：从预编译的打包文件或 class 目录加载（仅需 JRE）。</li>
 * </ul>
 *
 * <h3>静态资源</h3>
 * 支持在不重新打包 JAR 的情况下，通过在运行目录下放置 {@code public/} 文件夹
 * 来覆盖或新增前端资源。资源访问路径前缀为 {@code /static/**}。
 */
@Configuration
public class ViewConfig {
    private static final Logger log = LoggerFactory.getLogger(ViewConfig.class);

    /**
     * 声明式注册 Blade 视图实现（对齐 {@code @RegisterGuardDriver} / {@code @RegisterCacheStore}）。
     * <p>
     * 三种写法示例（仅启用其一即可），其余以注释保留方便理解：
     * </p>
     *
     * <h4>写法一：运行时编译模式（默认，需 JDK）</h4>
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
        log.info("[view] 注册 Blade 视图(运行时编译): templateDir={}, suffix={}", templateDir, suffix);
        return BladeView.runtime("blade", templateDir, suffix, cacheStore, assetUrlPrefix);
    }

    // ========================================================================
    // 备选写法（注释态，方便理解与使用）
    // ========================================================================
    //
    // 写法二：预编译打包模式（仅需 JRE，模板已编译进 jar 包）
    // @RegisterView(name = "blade-precompiled", defaultView = true)
    // @Bean
    // public View bladePrecompiledPackageView(
    //         @Value("${jaravel.view.precompiled-package:com.weacsoft.jaravel.templates}") String pkg,
    //         @Value("${jaravel.view.asset-url-prefix:/static}") String assetUrlPrefix) {
    //     return BladeView.precompiledPackage("blade-precompiled", pkg, assetUrlPrefix);
    // }
    //
    // 写法三：预编译 class 目录模式（仅需 JRE，从外部目录加载编译产物）
    // @RegisterView(name = "blade-precompiled-dir", defaultView = true)
    // @Bean
    // public View bladePrecompiledDirView(
    //         @Value("${jaravel.view.precompiled-classes-dir:}") String classesDir,
    //         @Value("${jaravel.view.asset-url-prefix:/static}") String assetUrlPrefix) {
    //     return BladeView.precompiledClasses("blade-precompiled-dir", classesDir, assetUrlPrefix);
    // }
    //
    // 写法四：自定义 View 实现（完全替换模板引擎）
    // @RegisterView(name = "myview", defaultView = true)
    // @Bean
    // public View myView() {
    //     return new MyCustomView(); // 实现 com.weacsoft.jaravel.vendor.core.view.View
    // }
    //
    // 写法五：完全不写任何声明 —— 由 jblade 的 ViewAutoConfiguration 兜底注册 Blade（运行时编译）。
    //   此时 application.yml 中无需任何 jaravel.view.* 配置即可渲染模板。

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
