package com.weacsoft.jaravel.routes;

import com.weacsoft.jaravel.vendor.http.controller.response.ResponseBuilder;
import com.weacsoft.jaravel.vendor.route.Router;
import org.springframework.stereotype.Component;

/**
 * Web 路由定义，对齐 Laravel 的 {@code routes/web.php}。
 * <p>
 * 首页重定向到登录页，静态资源由 SpringBoot 默认静态资源服务处理。
 * <p>
 * 控制器通过字符串引用（如 {@code "PageController::captchaDemo"}），无需 {@code getBean} 获取控制器实例。
 */
@Component
public class Web {

    public void register(Router router) {
        // 首页重定向到 index.html
        router.get("/", request -> ResponseBuilder.redirect("/index.html"));

        // 验证码演示页面（字符串控制器引用）
        router.get("/captcha-demo", "PageController::captchaDemo");

        // Wire Demo 页面（初始渲染，字符串控制器引用）
        router.get("/wire-demo", "WireDemoController::page");

        // Wire Demo 更新端点（POST，处理 wire 请求）
        // 如需认证保护，添加 .middleware("auth:api") 即可：
        //   - 未登录时中间件检测 X-Wire-Request 头，返回 401 JSON {redirect: "/login"}
        //   - wire.js 自动跳转登录页，用户无感知
        router.post("/api/wire/demo", "WireDemoController::update");
    }
}
