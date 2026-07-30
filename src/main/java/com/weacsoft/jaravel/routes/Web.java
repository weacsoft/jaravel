package com.weacsoft.jaravel.routes;

import com.weacsoft.jaravel.vendor.http.controller.response.ResponseBuilder;
import com.weacsoft.jaravel.vendor.route.Route;

/**
 * Web 路由定义，对齐 Laravel 的 {@code routes/web.php}。
 * <p>
 * 纯静态类，不注册为 Spring Bean。由 {@code RouteServiceProvider} 通过
 * {@code Route.group(Map.of(Route.Group.MIDDLEWARE, new String[]{}), Web::register)}
 * 以方法引用形式调用，对齐 Laravel {@code Route::middleware('web')->group(base_path('routes/web.php'))}。
 * <p>
 * 全部使用 {@link Route} 静态门面注册路由，无需传递 {@code Router} 实例。
 * 首页重定向到登录页，静态资源由 SpringBoot 默认静态资源服务处理。
 * <p>
 * 控制器通过字符串引用（如 {@code "PageController::captchaDemo"}），无需 {@code getBean} 获取控制器实例。
 * 闭包式路由通过 {@code Route.currentRouter()} 获取当前 Router 后调用。
 */
public class Web {

    /**
     * 注册 Web 路由。使用 {@link Route} 静态门面，无需传递 Router 实例。
     * <p>
     * 由 {@code RouteServiceProvider} 以方法引用 {@code Web::register} 调用，
     * 外层已通过 {@code Route.group(Map.of(Route.Group.MIDDLEWARE, new String[]{}), Web::register)}
     * 提供 Web 组的中间件数组（对齐 Laravel {@code Route::middleware('web')->group(...)}）。
     */
    public static void register() {
        // 首页重定向到 index.html（闭包式路由，通过 currentRouter() 调用）
        Route.currentRouter().get("/", request -> ResponseBuilder.redirect("/index.html"));

        // 验证码演示页面（字符串控制器引用），命名路由供模板 @route('captcha.demo') 引用
        Route.get("/captcha-demo", "PageController::captchaDemo").name("captcha.demo");

        // jblade 功能演示页面（三层继承 / @parent / $loop / @route 路由别名 / 动态函数）
        Route.get("/blade-demo", "PageController::bladeDemo").name("blade.demo");

        // Wire Demo 页面（初始渲染，字符串控制器引用），命名路由供模板 @route('wire.demo') 引用
        Route.get("/wire-demo", "WireDemoController::page").name("wire.demo");

        // Wire Demo 更新端点（POST，处理 wire 请求）
        // 如需认证保护，添加 .middleware("auth:api") 即可：
        //   - 未登录时中间件检测 X-Wire-Request 头，返回 401 JSON {redirect: "/login"}
        //   - wire.js 自动跳转登录页，用户无感知
        Route.post("/api/wire/demo", "WireDemoController::update");
    }
}
