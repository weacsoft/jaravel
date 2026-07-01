package com.weacsoft.jaravel.app.http.controller;

import com.weacsoft.jaravel.vendor.controller.Controllers;
import com.weacsoft.jaravel.vendor.cache.Cache;
import com.weacsoft.jaravel.vendor.http.request.Request;
import com.weacsoft.jaravel.vendor.http.response.Response;
import com.weacsoft.jaravel.vendor.http.response.ResponseBuilder;
import org.springframework.stereotype.Controller;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 欢迎控制器，演示 Router+Request+Response 设计模式。
 * <p>
 * 控制器方法签名为 {@code public Response method(Request request)}，
 * 通过方法引用注册到 {@code Router}，返回 {@link Response} 对象。
 */
@Controller
public class WelcomeController implements Controllers {

    public Response hello(Request request) {
        Map<String, Object> data = new HashMap<>();
        data.put("message", "Hello, Jaravel!");
        data.put("version", "1.0.0");
        return ResponseBuilder.json(data);
    }

    public Response index(Request request) {
        Map<String, Object> data = new HashMap<>();
        data.put("title", "Welcome to Jaravel");
        data.put("version", "1.0.0");
        data.put("features", new String[]{
            "Elegant Routing System",
            "Powerful Request & Response Handling",
            "Middleware Support",
            "Blade Template Engine",
            "Database ORM Support",
            "Migration Support"
        });
        return ResponseBuilder.view("welcome", data);
    }

    /**
     * Request 格式测试，返回请求的全部输入、查询参数、请求头与文件信息。
     */
    public Response requestTest(Request request) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("all_inputs", request.all());
        result.put("all_queries", request.query());
        result.put("headers", request.header());
        result.put("content_type", request.header("Content-Type"));
        result.put("has_file", request.hasFile("file"));
        if (request.hasFile("file")) {
            result.put("file_name", request.file("file").getOriginalFilename());
            result.put("file_size", request.file("file").getSize());
        }
        return ResponseBuilder.json(result);
    }

    /**
     * Cache 演示：put / get / has / increment / remember。
     * 对齐 Laravel {@code Cache::put()} / {@code Cache::get()} / {@code Cache::remember()}。
     */
    public Response cacheDemo(Request request) {
        Map<String, Object> result = new LinkedHashMap<>();

        // put + get
        Cache.put("demo:key", "hello from cache", 60);
        result.put("cache_get", Cache.get("demo:key"));

        // has
        result.put("cache_has", Cache.has("demo:key"));

        // increment
        Cache.forget("demo:counter");
        Cache.increment("demo:counter");
        Cache.increment("demo:counter");
        Cache.increment("demo:counter", 5);
        result.put("cache_increment", Cache.get("demo:counter"));

        // remember (带 TTL 的缓存闭包)
        Object remembered = Cache.remember("demo:computed", 60, () -> {
            return "computed_at_" + System.currentTimeMillis();
        });
        result.put("cache_remember", remembered);

        // 第二次 remember（命中缓存，不会重新计算）
        Object remembered2 = Cache.remember("demo:computed", 60, () -> {
            return "computed_at_" + System.currentTimeMillis();
        });
        result.put("cache_remember_hit", remembered2);
        result.put("cache_remember_same", remembered.equals(remembered2));

        // forget
        Cache.forget("demo:key");
        result.put("cache_after_forget", Cache.has("demo:key"));

        result.put("message", "Cache 功能演示完成");
        return ResponseBuilder.json(result);
    }

    /**
     * View 演示：渲染 Blade 模板。
     * 对齐 Laravel {@code view('welcome', $data)}。
     */
    public Response viewDemo(Request request) {
        Map<String, Object> data = new HashMap<>();
        data.put("title", "Jaravel Blade Template Engine");
        data.put("version", "1.0.0");
        data.put("features", new String[]{
            "Elegant Routing System",
            "Multi-Guard Authentication (JWT + Session)",
            "Multi-Database Support",
            "Middleware Pipeline (Onion Model)",
            "Event/Queue System with Retry",
            "Cache (Array + File)",
            "Blade Template Engine",
            "Migration System (MySQL/SQLite/SQL Server)",
            "Eloquent ORM (Merged Model)"
        });
        return ResponseBuilder.view("welcome", data);
    }
}
