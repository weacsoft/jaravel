package com.weacsoft.jaravel.routes;

import com.weacsoft.jaravel.app.http.controller.WelcomeController;
import com.weacsoft.jaravel.vendor.http.response.ResponseBuilder;
import com.weacsoft.jaravel.vendor.route.Router;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Web 路由定义，对齐 Laravel 的 {@code routes/web.php}。
 * <p>
 * 由 {@code RouteServiceProvider} 调用 {@link #register(Router, ApplicationContext)}
 * 注册 Web 路由（返回视图），不加 {@code api} 前缀。
 * <p>
 * 包含：首页路由和文档页面路由（/docs/*），文档页面使用 jblade 模板引擎渲染。
 */
@Component
public class Web {

    public void register(Router router, ApplicationContext context) {
        WelcomeController welcomeController = context.getBean(WelcomeController.class);

        // Web 路由（返回视图）
        router.get("/", welcomeController::index);

        // ===== 文档页面路由（纯前端文档包） =====
        // 使用 ResponseBuilder.view() 渲染 jblade 模板
        // 模板文件位于 src/main/resources/templates/docs/ 下

        // 文档首页
        router.get("/docs", request -> ResponseBuilder.view("docs.index", Map.of(
            "title", "Jaravel Demo 文档"
        )));

        // 安装指南
        router.get("/docs/installation", request -> ResponseBuilder.view("docs.installation", Map.of(
            "title", "安装指南 - Jaravel Demo 文档"
        )));

        // 路由
        router.get("/docs/routing", request -> ResponseBuilder.view("docs.routing", Map.of(
            "title", "路由 - Jaravel Demo 文档"
        )));

        // Eloquent ORM
        router.get("/docs/eloquent", request -> ResponseBuilder.view("docs.eloquent", Map.of(
            "title", "Eloquent ORM - Jaravel Demo 文档"
        )));

        // 认证
        router.get("/docs/auth", request -> ResponseBuilder.view("docs.auth", Map.of(
            "title", "认证 - Jaravel Demo 文档"
        )));

        // 缓存
        router.get("/docs/cache", request -> ResponseBuilder.view("docs.cache", Map.of(
            "title", "缓存 - Jaravel Demo 文档"
        )));

        // 事件系统
        router.get("/docs/events", request -> ResponseBuilder.view("docs.events", Map.of(
            "title", "事件系统 - Jaravel Demo 文档"
        )));

        // Artisan CLI
        router.get("/docs/artisan", request -> ResponseBuilder.view("docs.artisan", Map.of(
            "title", "Artisan CLI - Jaravel Demo 文档"
        )));

        // 定时任务
        router.get("/docs/schedule", request -> ResponseBuilder.view("docs.schedule", Map.of(
            "title", "定时任务 - Jaravel Demo 文档"
        )));

        // 队列
        router.get("/docs/queue", request -> ResponseBuilder.view("docs.queue", Map.of(
            "title", "队列 - Jaravel Demo 文档"
        )));

        // 插件系统
        router.get("/docs/plugins", request -> ResponseBuilder.view("docs.plugins", Map.of(
            "title", "插件系统 - Jaravel Demo 文档"
        )));
    }
}
