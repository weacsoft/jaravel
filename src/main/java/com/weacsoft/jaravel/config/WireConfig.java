package com.weacsoft.jaravel.config;

import com.weacsoft.jaravel.vendor.wire.WireManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import java.util.Arrays;
import java.util.List;

/**
 * Wire 模块配置（对齐 Laravel Livewire 风格的部分更新）。
 * <p>
 * 通过构造器注入配置 WireManager 的行为，配置项来自 {@code jaravel.wire.*}：
 * <ul>
 *   <li>{@code jaravel.wire.auto-inject-js}：是否自动注入 wire.js（默认 true）</li>
 *   <li>{@code jaravel.wire.js-path}：wire.js 引用路径（默认 /static/wire.js）</li>
 *   <li>{@code jaravel.wire.excluded-sections}：排除的 section 名列表</li>
 * </ul>
 */
@Configuration
public class WireConfig {
    private static final Logger log = LoggerFactory.getLogger(WireConfig.class);

    public WireConfig(@Value("${jaravel.wire.auto-inject-js:true}") boolean autoInjectJs,
                      @Value("${jaravel.wire.js-path:/static/wire.js}") String jsPath,
                      Environment env) {
        WireManager.setAutoInjectJs(autoInjectJs);
        WireManager.setJsPath(jsPath);

        String excludedStr = env.getProperty("jaravel.wire.excluded-sections", "");
        if (excludedStr != null && !excludedStr.isEmpty()) {
            List<String> excluded = Arrays.asList(excludedStr.split(","));
            WireManager.addExcludedSections(excluded.toArray(new String[0]));
            log.info("Wire 模块已配置: autoInjectJs={}, jsPath={}, excludedSections={}", autoInjectJs, jsPath, excluded);
        } else {
            log.info("Wire 模块已配置: autoInjectJs={}, jsPath={}", autoInjectJs, jsPath);
        }
    }
}
