package com.weacsoft.jaravel.config.wire;

import com.weacsoft.jaravel.vendor.wire.WireManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import java.util.Arrays;
import java.util.List;

/**
 * Wire 模块配置。
 * <p>
 * 显式配置 Wire 模块行为。可通过 application.yml 的 jaravel.wire.* 配置，也可在此直接编程式配置。
 * <p>
 * 配置项：
 * - jaravel.wire.auto-inject-js: 是否自动注入 wire.js（默认 true）
 * - jaravel.wire.js-path: wire.js 引用路径（默认 /static/wire.js）
 * - jaravel.wire.excluded-sections: 排除的 section 名列表（不生成 wire 标记）
 * - 设为 false 时需手动引入 wire.js，可使用 WireManager.getWireJsContent() 获取JS内容
 */
@Configuration
public class WireConfig {
    private static final Logger log = LoggerFactory.getLogger(WireConfig.class);

    public WireConfig(@Value("${jaravel.wire.auto-inject-js:true}") boolean autoInjectJs,
                      @Value("${jaravel.wire.js-path:/static/wire.js}") String jsPath,
                      Environment env) {
        WireManager.setAutoInjectJs(autoInjectJs);
        WireManager.setJsPath(jsPath);

        // 读取排除列表
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
