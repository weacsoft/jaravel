package com.weacsoft.jaravel.app.provider;

import com.weacsoft.jaravel.vendor.core.provider.ServiceProvider;
import com.weacsoft.jaravel.app.http.middleware.AppVerifyCsrfToken;
import com.weacsoft.jaravel.vendor.http.controller.request.Request;
import com.weacsoft.jaravel.vendor.http.controller.request.RequestFactory;
import com.weacsoft.jaravel.vendor.http.controller.response.ResponseBuilder;
import com.weacsoft.jaravel.vendor.jblade.BladeEngine;
import com.weacsoft.jaravel.vendor.jblade.BladeFunctions;
import com.weacsoft.jaravel.vendor.route.RouteDefinition;
import com.weacsoft.jaravel.vendor.route.Router;
import com.weacsoft.jaravel.vendor.utils.memory.MemoryClassLoader;
import com.weacsoft.jaravel.vendor.wire.WireManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Blade 模板引擎初始化提供者。
 * <p>
 * 在应用启动时创建 {@link BladeEngine} 实例（模板目录 classpath:templates/），
 * 注入到 {@link ResponseBuilder} 中，使控制器可以通过 {@code ResponseBuilder.view()} 渲染模板。
 * <p>
 * asset 与 url 行为一致，模板中的 {@code @asset('css/app.css')} 与
 * {@code {{ asset('css/app.css') }}} 均按根路径拼接为 {@code /css/app.css}，不附加任何资源前缀。
 * <p>
 * <b>动态函数注册（不修改 jblade 源码的外部扩展点）</b>：
 * 通过 {@link BladeFunctions#register} 注册 {@code route} 函数，
 * 将模板中的 {@code @route('login')} / {@code {{ route('users.show', ...) }}}
 * 解析为 http 模块中以 {@link RouteDefinition#name(String)} 命名的路由别名对应的 URI，
 * 语义对齐 Laravel 的 {@code route()} 辅助函数。
 * 同时注册 {@code csrf_token} 函数：从当前请求的 HttpSession 读取
 * {@code VerifyCsrfToken} 维护的 token（无则生成并写回），使模板中的
 * {@code {{ csrf_field() }}} / {@code @csrf} 输出非空隐藏域
 * （{@code <input type="hidden" name="_token" value="...">}），
 * 且与 Web 路由组挂接的 {@code VerifyCsrfToken} 中间件校验同源。
 */
@Component
public class BladeEngineProvider extends ServiceProvider {

    private static final Logger log = LoggerFactory.getLogger(BladeEngineProvider.class);

    private final Router router;

    public BladeEngineProvider(Router router) {
        this.router = router;
    }

    @Override
    public void register() {
        // ===== 动态函数：route(name [, params]) —— http 模块路由别名 -> URI =====
        // 不修改 jblade 核心源码，通过 BladeFunctions 外部注册；
        // BladeTemplate.route()/routeAny() 运行时优先查找此注册表。
        BladeFunctions.register("route", args -> {
            String name = String.valueOf(args[0]);
            Object params = args.length > 1 ? args[1] : null;
            return resolveRoute(name, params);
        });

        // ===== 动态函数：csrf_token() —— 对齐 Laravel csrf_token() =====
        // 复用 AppVerifyCsrfToken 读取当前请求 session 中的 token（无则生成并写回），
        // 保证 csrf_field() 渲染出的隐藏域 value 非空，且与 Web 路由组挂接的
        // VerifyCsrfToken 中间件校验用的 token 同源一致。
        BladeFunctions.register("csrf_token", args -> {
            try {
                Request req = RequestFactory.getCurrentRequest();
                if (req != null) {
                    return AppVerifyCsrfToken.currentToken(req);
                }
            } catch (Exception ignored) {
                // 非 Web 请求上下文（如离线模板渲染）回退到空串，由调用方决定
            }
            return "";
        });

        // 创建 BladeEngine，模板目录为 classpath 下的 templates/
        // 显式传入 MemoryClassLoader 避免 jblade 内部无参构造器触发 ClassLoader 模块访问问题
        MemoryClassLoader classLoader = new MemoryClassLoader(
                new java.util.HashMap<>(), BladeEngineProvider.class.getClassLoader());
        BladeEngine engine = new BladeEngine("templates", classLoader);
        ResponseBuilder.setBladeEngine(engine);
        WireManager.setEngine(engine);

        log.info("[blade] BladeEngine 已初始化, 模板目录=templates/, 后缀=.blade.java, asset 与 url 一致(无资源前缀)");
        log.info("[blade] 动态函数 route() 已注册, 按 http 模块路由别名解析 URI");
        log.info("[blade] 动态函数 csrf_token() 已注册, 从 HttpSession 读取/生成 token, 与 VerifyCsrfToken 中间件同源");
        log.info("[wire] WireManager 已初始化, 使用同一 BladeEngine 实例");
    }

    /**
     * 按路由别名解析 URI（对齐 Laravel route() 语义）。
     *
     * @param name   路由别名（{@code Route.get(...).name("login")} 注册的完整名称）
     * @param params 路由参数：Map 时按参数名替换 {@code {key}} / {@code {key?}}；
     *               单值时替换第一个占位符；null 时不替换
     * @return 解析后的 URI；未找到别名时退化为 {@code /name}（点转斜杠）并输出警告
     */
    private String resolveRoute(String name, Object params) {
        // getFullName() 返回的完整名称带前导点（normalizeName 语义），匹配前统一去除
        String target = name.startsWith(".") ? name.substring(1) : name;
        for (RouteDefinition def : router.getAllRoutes()) {
            String fullName = def.getFullName();
            if (fullName == null) {
                continue;
            }
            String candidate = fullName.startsWith(".") ? fullName.substring(1) : fullName;
            if (target.equals(candidate)) {
                String uri = def.getFullUri();
                if (!uri.startsWith("/")) {
                    uri = "/" + uri;
                }
                if (params instanceof Map) {
                    for (Map.Entry<?, ?> e : ((Map<?, ?>) params).entrySet()) {
                        String key = String.valueOf(e.getKey());
                        String value = String.valueOf(e.getValue());
                        uri = uri.replace("{" + key + "?}", value).replace("{" + key + "}", value);
                    }
                } else if (params != null) {
                    uri = uri.replaceFirst("\\{[^/{}]+\\}", String.valueOf(params));
                }
                return uri;
            }
        }
        log.warn("[blade] route('{}') 未找到对应路由别名, 退化为路径映射", name);
        return "/" + name.replace('.', '/');
    }
}
