package com.weacsoft.jaravel.routes;

import com.weacsoft.jaravel.vendor.http.controller.response.ResponseBuilder;
import com.weacsoft.jaravel.vendor.route.Routes;
import org.springframework.stereotype.Component;

/**
 * Web 路由定义，对齐 Laravel 的 {@code routes/web.php}。
 * <p>
 * 全部使用 {@link Routes} 静态门面注册路由，无需传递 {@code Router} 实例。
 * 首页重定向到登录页，静态资源由 SpringBoot 默认静态资源服务处理。
 * <p>
 * 控制器通过字符串引用（如 {@code "PageController::captchaDemo"}），无需 {@code getBean} 获取控制器实例。
 * 闭包式路由通过 {@code Routes.currentRouter()} 获取当前 Router 后调用。
 */
@Component
public class Web {

    /**
     * 注册 Web 路由。使用 {@link Routes} 静态门面，无需传递 Router 实例。
     * <p>
     * 前提：{@code RouteServiceProvider} 中已调用 {@code Routes.setRootRouter(baseRouter)} 初始化静态门面。
     */
    public void register() {
        // 首页重定向到 index.html（闭包式路由，通过 currentRouter() 调用）
        Routes.currentRouter().get("/", request -> ResponseBuilder.redirect("/index.html"));

        // 验证码演示页面（字符串控制器引用）
        Routes.get("/captcha-demo", "PageController::captchaDemo");

        // Wire Demo 页面（初始渲染，字符串控制器引用）
        Routes.get("/wire-demo", "WireDemoController::page");

        // Wire Demo 更新端点（POST，处理 wire 请求）
        // 如需认证保护，添加 .middleware("auth:api") 即可：
        //   - 未登录时中间件检测 X-Wire-Request 头，返回 401 JSON {redirect: "/login"}
        //   - wire.js 自动跳转登录页，用户无感知
        Routes.post("/api/wire/demo", "WireDemoController::update");
    }
}
