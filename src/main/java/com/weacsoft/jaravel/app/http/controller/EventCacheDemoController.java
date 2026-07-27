package com.weacsoft.jaravel.app.http.controller;

import com.weacsoft.jaravel.app.event.OrderCreatedEvent;
import com.weacsoft.jaravel.app.event.UserRegisteredEvent;
import com.weacsoft.jaravel.vendor.cache.Cache;
import com.weacsoft.jaravel.vendor.core.Facade;
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
 */
@Controller
public class EventCacheDemoController implements Controllers {

    /**
     * 演示多 cache store：同一应用中使用 array / file / database 三种缓存。
     * <p>
     * 不同模块可以使用不同的 cache store：
     * <ul>
     *   <li>默认 store（array）：用于临时数据、高频读写</li>
     *   <li>file store：用于文件型缓存，跨重启持久化</li>
     *   <li>database store：用于数据库缓存，需先执行 {@code artisan cache:table}</li>
     * </ul>
     */
    public Response demoMultiCache(Request request) {
        Map<String, Object> result = new LinkedHashMap<>();

        // 1. 使用默认 store（array）
        Cache.put("demo:default", "hello-from-array", 60);
        result.put("default-store", Cache.get("demo:default"));

        // 2. 使用 file store
        try {
            Cache.store("file").put("demo:file", "hello-from-file", 60);
            result.put("file-store", Cache.store("file").get("demo:file"));
        } catch (Exception e) {
            result.put("file-store", "file store 不可用: " + e.getMessage());
        }

        // 3. 使用 database store（需先执行 artisan cache:table 建表）
        try {
            Cache.store("database").put("demo:db", "hello-from-database", 60);
            result.put("database-store", Cache.store("database").get("demo:db"));
        } catch (Exception e) {
            result.put("database-store", "database store 不可用（请先执行 artisan cache:table）: " + e.getMessage());
        }

        // 4. 模块级 cache 配置说明
        result.put("config-note", "各模块通过独立配置项指定 cache store：wechat=file, model-cache=database, jwt=array");

        return ResponseBuilder.json(result);
    }

    /**
     * 演示用户注册事件分发：
     * <ul>
     *   <li>{@code RecordUserRegistrationListener} — 同步执行（不实现 ShouldQueue）</li>
     *   <li>{@code SendWelcomeEmailListener} — 异步执行到 {@code emails} 队列</li>
     * </ul>
     */
    public Response demoUserEvent(Request request) {
        Dispatcher dispatcher = Facade.resolve(Dispatcher.class);
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
     * <ul>
     *   <li>{@code ProcessOrderPaymentListener} — 异步执行到 {@code payments} 队列</li>
     *   <li>{@code GenerateInvoiceListener} — 异步执行到 {@code invoices} 队列（延迟 5 秒）</li>
     * </ul>
     * 两个监听器路由到不同队列，互不阻塞，可并行执行。
     */
    public Response demoOrderEvent(Request request) {
        Dispatcher dispatcher = Facade.resolve(Dispatcher.class);
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
}
