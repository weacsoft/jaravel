package com.weacsoft.jaravel.app.http.controller;

import com.weacsoft.jaravel.app.event.OrderCreatedEvent;
import com.weacsoft.jaravel.app.event.UserRegisteredEvent;
import com.weacsoft.jaravel.config.AppConfig;
import com.weacsoft.jaravel.vendor.event.Dispatcher;
import com.weacsoft.jaravel.vendor.http.controller.Controllers;
import com.weacsoft.jaravel.vendor.http.controller.request.Request;
import com.weacsoft.jaravel.vendor.http.controller.response.Response;
import com.weacsoft.jaravel.vendor.http.controller.response.ResponseBuilder;
import org.springframework.stereotype.Controller;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 事件与缓存演示控制器，展示多 cache store 和多 queue 分发。
 * <p>
 * <b>多 Cache 演示</b>：
 * <ul>
 *   <li>{@code GET /api/demo/cache} — 演示使用不同 cache store（array / file / database）</li>
 * </ul>
 * <b>多 Queue 演示</b>：
 * <ul>
 *   <li>{@code GET /api/demo/event/user} — 分发用户注册事件（同步 + emails 队列）</li>
 *   <li>{@code GET /api/demo/event/order} — 分发订单创建事件（payments + invoices 队列）</li>
 * </ul>
 * <b>AppConfig.app() 演示</b>：
 * <ul>
 *   <li>{@code GET /api/demo/app} — 展示通过 AppConfig.app() 链式访问各类服务</li>
 * </ul>
 */
@Controller
public class EventCacheDemoController implements Controllers {

    /**
     * 演示多 cache store：同一应用中使用 array / file / database 三种缓存。
     * <p>
     * 使用 {@code AppConfig.app().cache()} 获取 CacheManager，替代 {@code Cache::} 静态门面。
     */
    public Response demoMultiCache(Request request) {
        Map<String, Object> result = new LinkedHashMap<>();

        // 使用 typed 访问器 cache() 获取 CacheManager
        var cache = AppConfig.app().cache();
        var defaultStore = cache.store();

        // 1. 使用默认 store（array）
        defaultStore.put("demo:default", "hello-from-array", 60);
        result.put("default-store", defaultStore.get("demo:default"));

        // 2. 使用 file store
        try {
            cache.store("file").put("demo:file", "hello-from-file", 60);
            result.put("file-store", cache.store("file").get("demo:file"));
        } catch (Exception e) {
            result.put("file-store", "file store 不可用: " + e.getMessage());
        }

        // 3. 使用 database store（需先执行 artisan cache:table 建表）
        try {
            cache.store("database").put("demo:db", "hello-from-database", 60);
            result.put("database-store", cache.store("database").get("demo:db"));
        } catch (Exception e) {
            result.put("database-store", "database store 不可用（请先执行 artisan cache:table）: " + e.getMessage());
        }

        result.put("config-note", "各模块通过独立配置项指定 cache store：wechat=file, model-cache=database, jwt=array");

        return ResponseBuilder.json(result);
    }

    /**
     * 演示用户注册事件分发：
     * <p>
     * 使用 {@code AppConfig.app().event()} 获取 Dispatcher，替代 {@code Facade.resolve(Dispatcher.class)}。
     */
    public Response demoUserEvent(Request request) {
        // 使用 typed 访问器 event() 获取事件分发器
        Dispatcher dispatcher = AppConfig.app().event();
        dispatcher.dispatch(new UserRegisteredEvent(1L, "demo-user"));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("event", "UserRegisteredEvent");
        result.put("listeners", new String[]{
                "RecordUserRegistrationListener (sync)",
                "SendWelcomeEmailListener (queue=emails)"
        });
        result.put("message", "事件已分发，查看日志输出");
        return ResponseBuilder.json(result);
    }

    /**
     * 演示订单创建事件分发：
     * <p>
     * 使用 {@code AppConfig.app().event()} 获取 Dispatcher。
     */
    public Response demoOrderEvent(Request request) {
        Dispatcher dispatcher = AppConfig.app().event();
        dispatcher.dispatch(new OrderCreatedEvent(1001L, 1L, 99.99));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("event", "OrderCreatedEvent");
        result.put("listeners", new String[]{
                "ProcessOrderPaymentListener (queue=payments)",
                "GenerateInvoiceListener (queue=invoices, delay=5s)"
        });
        result.put("message", "事件已分发到不同队列，查看日志输出");
        return ResponseBuilder.json(result);
    }

    /**
     * 演示 AppConfig.app() 链式服务访问，替代 Facade 静态代理。
     * <p>
     * 展示三种访问方式：
     * <ol>
     *   <li>typed 访问器：{@code AppConfig.app().auth()} — 免强转，推荐</li>
     *   <li>make(String) 别名：{@code AppConfig.app().make("auth")} — 自动注册的别名</li>
     *   <li>make(Class) 通用：{@code AppConfig.app().make(AuthManager.class)} — 任何模块可用</li>
     * </ol>
     */
    public Response demoAppContainer(Request request) {
        Map<String, Object> result = new LinkedHashMap<>();

        // 方式一：typed 访问器（免强转，推荐）
        var auth = AppConfig.app().auth();
        result.put("typed-auth-guard", auth.getDefaultGuard());
        result.put("typed-auth-has-guards", auth.hasGuards());

        // 方式二：make(String) 别名（自动注册的别名，对齐 Laravel app('auth')）
        var authByAlias = AppConfig.app().<com.weacsoft.jaravel.vendor.auth.AuthManager>make("auth");
        result.put("alias-auth-same-instance", auth == authByAlias);

        // 方式三：make(Class) 通用方式
        var config = AppConfig.app().make(com.weacsoft.jaravel.vendor.core.config.ConfigRepository.class);
        result.put("make-class-config-name", config.string("app.name", "Jaravel"));

        // 自定义服务注册演示（singleton + make）
        AppConfig.app().singleton("demo:greeting", () -> "Hello from AppConfig.app()!");
        result.put("custom-service", AppConfig.app().make("demo:greeting"));
        result.put("custom-bound", AppConfig.app().bound("demo:greeting"));

        return ResponseBuilder.json(result);
    }
}
