package com.weacsoft.jaravel.app.provider;

import com.weacsoft.jaravel.vendor.core.provider.ServiceProvider;
import com.weacsoft.jaravel.vendor.http.controller.response.ResponseBuilder;
import com.weacsoft.jaravel.vendor.jblade.BladeAssetHelper;
import com.weacsoft.jaravel.vendor.jblade.BladeEngine;
import com.weacsoft.jaravel.vendor.utils.memory.MemoryClassLoader;
import com.weacsoft.jaravel.vendor.wire.WireManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Blade 模板引擎初始化提供者。
 * <p>
 * 在应用启动时创建 {@link BladeEngine} 实例（模板目录 classpath:templates/），
 * 注入到 {@link ResponseBuilder} 中，使控制器可以通过 {@code ResponseBuilder.view()} 渲染模板。
 * <p>
 * 同时配置 {@link BladeAssetHelper} 的 URL 前缀为 {@code /static}，
 * 使模板中的 {@code @asset('css/app.css')} 编译为 {@code /static/css/app.css}。
 */
@Component
public class BladeEngineProvider extends ServiceProvider {

    private static final Logger log = LoggerFactory.getLogger(BladeEngineProvider.class);

    @Override
    public void register() {
        // 配置静态资源 URL 前缀
        BladeAssetHelper.setUrlPrefix("/static");

        // 创建 BladeEngine，模板目录为 classpath 下的 templates/
        // 显式传入 MemoryClassLoader 避免 jblade 内部无参构造器触发 ClassLoader 模块访问问题
        MemoryClassLoader classLoader = new MemoryClassLoader(
                new java.util.HashMap<>(), BladeEngineProvider.class.getClassLoader());
        BladeEngine engine = new BladeEngine("templates", classLoader);
        ResponseBuilder.setBladeEngine(engine);
        WireManager.setEngine(engine);

        log.info("[blade] BladeEngine 已初始化, 模板目录=templates/, 后缀=.blade.java, 资源前缀=/static");
        log.info("[wire] WireManager 已初始化, 使用同一 BladeEngine 实例");
    }
}
