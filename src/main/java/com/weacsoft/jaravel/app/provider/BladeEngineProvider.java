package com.weacsoft.jaravel.app.provider;

import com.weacsoft.jaravel.vendor.core.provider.ServiceProvider;
import com.weacsoft.jaravel.vendor.http.controller.response.ResponseBuilder;
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
 * asset 与 url 行为一致，模板中的 {@code @asset('css/app.css')} 与
 * {@code {{ asset('css/app.css') }}} 均按根路径拼接为 {@code /css/app.css}，不附加任何资源前缀。
 * <p>
 * 模板辅助函数 {@code route()} / {@code csrf_token()} / {@code csrf_field()} 由框架
 * （SpringBoot 自动配置 {@code SpringBootRouteAutoConfiguration}）开箱即用注册，
 * 无需在此或中间件子类中额外注册；应用只需在 Web 路由组引用 {@code "VerifyCsrfToken"} 别名即可启用校验。
 */
@Component
public class BladeEngineProvider extends ServiceProvider {

    private static final Logger log = LoggerFactory.getLogger(BladeEngineProvider.class);

    public BladeEngineProvider() {
    }

    @Override
    public void register() {
        // 创建 BladeEngine，模板目录为 classpath 下的 templates/
        // 显式传入 MemoryClassLoader 避免 jblade 内部无参构造器触发 ClassLoader 模块访问问题
        MemoryClassLoader classLoader = new MemoryClassLoader(
                new java.util.HashMap<>(), BladeEngineProvider.class.getClassLoader());
        BladeEngine engine = new BladeEngine("templates", classLoader);
        ResponseBuilder.setBladeEngine(engine);
        WireManager.setEngine(engine);

        log.info("[blade] BladeEngine 已初始化, 模板目录=templates/, 后缀=.blade.java, asset 与 url 一致(无资源前缀)");
        log.info("[blade] 模板辅助函数 route()/csrf_token()/csrf_field() 由框架自动配置开箱即用注册");
        log.info("[wire] WireManager 已初始化, 使用同一 BladeEngine 实例");
    }
}
