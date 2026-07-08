package com.weacsoft.jaravel.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.File;

/**
 * 静态资源配置。
 * <p>
 * 支持从文件系统 {@code ./public/} 目录优先加载静态资源（CSS/JS/字体/图片等），
 * 回退到 ClassPath {@code classpath:/static/}（JAR 内置资源）。
 * <p>
 * 这样可以在不重新打包 JAR 的情况下，通过在运行目录下放置 {@code public/} 文件夹
 * 来覆盖或新增前端资源，实现前端独立更新。
 * <p>
 * 资源访问路径前缀为 {@code /static/**}，与 {@code BladeAssetHelper} 的 URL 前缀一致。
 */
@Configuration
public class StaticResourceConfig implements WebMvcConfigurer {

    private static final Logger log = LoggerFactory.getLogger(StaticResourceConfig.class);

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 检查文件系统 ./public/ 目录是否存在
        File publicDir = new File("public");
        boolean hasPublicDir = publicDir.isDirectory();

        if (hasPublicDir) {
            // 优先从文件系统 ./public/ 加载，回退到 classpath:/static/
            registry.addResourceHandler("/static/**")
                    .addResourceLocations(
                            "file:./public/",
                            "classpath:/static/"
                    );
            log.info("[static] 静态资源配置: /static/** -> file:./public/ (优先) + classpath:/static/ (回退)");
        } else {
            // 仅使用 classpath:/static/
            registry.addResourceHandler("/static/**")
                    .addResourceLocations("classpath:/static/");
            log.info("[static] 静态资源配置: /static/** -> classpath:/static/ (未检测到 ./public/ 目录)");
        }
    }
}
