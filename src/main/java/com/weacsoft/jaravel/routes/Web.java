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

        // Wire 列表演示：静态数组模拟 DB 的 CRUD + 分页 + 精准刷新（对应问题 1/2/4/5/6/7）
        Route.get("/wire-list-demo", "WireListDemoController::page").name("wire.list.demo");
        Route.post("/api/wire/list-demo", "WireListDemoController::update");

        // Wire SPA 演示：左侧菜单 + 右侧内容（第3点导航）+ 懒加载/分页/CRUD 组合（第7点）
        Route.get("/wire-spa-demo", "WireSpaDemoController::page").name("wire.spa.demo");
        Route.post("/api/wire/spa-demo", "WireSpaDemoController::update");

        // ===== Wire 命名组件（toast / confirm）演示 =====
        // WireOutlet 中间件已挂在 Web 组末尾，自动补齐 outlet 容器 + 首屏 bootstrap + 前端运行时；
        // 组件在 jaravel.wire.components 中注册（toast / confirm）。
        Route.get("/wire-component-demo", "WireComponentDemoController::page").name("wire.component.demo");
        Route.post("/api/wire-component-demo", "WireComponentDemoController::update");
        Route.get("/wire-component-plain", "WireComponentDemoController::plain").name("wire.component.plain");

        // ===== PJAX 无感切换演示：两个完全独立的页面 =====
        // 各自独立路径 + 独立控制器类 + 独立 blade 模板文件，彼此零耦合。
        // 控制器里没有任何 PJAX 代码，也没有为切换新增端点：
        // 首次直接访问返回完整页面，从已加载页面点链接过来只替换变化区域，
        // 全部由全局 PjaxMiddleware + ResponseBuilder 自动完成。
        Route.get("/home", "PjaxHomeController::index").name("pjax.home");
        Route.get("/list", "PjaxListController::index").name("pjax.list");

        // 数据库文件存储演示（driver: database）：上传/预览/下载/删除
        Route.get("/demo/storage", "StorageDemoController::index").name("storage.demo");
        Route.post("/demo/storage/upload", "StorageDemoController::upload");
        Route.get("/demo/storage/download", "StorageDemoController::download");
        Route.get("/demo/storage/view", "StorageDemoController::view");
        Route.get("/demo/storage/delete", "StorageDemoController::delete");
    }
}
