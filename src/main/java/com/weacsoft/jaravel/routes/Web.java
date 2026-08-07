package com.weacsoft.jaravel.routes;

import com.weacsoft.jaravel.vendor.http.controller.response.ResponseBuilder;
import com.weacsoft.jaravel.vendor.route.Route;

/**
 * Web 路由定义，对齐 Laravel 的 {@code routes/web.php}。
 */
public class Web {

    public static void register() {
        // 首页重定向
        Route.currentRouter().get("/", request -> ResponseBuilder.redirect("/index.html"));

        // 验证码演示页面
        Route.get("/captcha-demo", "PageController::captchaDemo").name("captcha.demo");

        // jblade 功能演示页面（三层继承 / @parent / $loop / @route 路由别名 / 动态函数）
        Route.get("/blade-demo", "PageController::bladeDemo").name("blade.demo");

        // ===== Wire 演示（所有控制器集中在 wire 包下，模板集中在 templates/wire/ 下）=====

        // 主入口：仪表盘（含统计数据 + 快速导航）
        Route.get("/wire",          "WireShowcaseController::index").name("wire.index");
        Route.post("/api/wire",     "WireShowcaseController::update");

        // 记录列表（与仪表盘共享 wire-layout，展示多 Controller 间切换）
        Route.get("/wire/records",      "WireShowcaseController::records").name("wire.records");
        Route.post("/api/wire/records", "WireShowcaseController::recordsUpdate");

        // SPA 导航演示（左侧菜单切换三个页面）
        Route.get("/wire/spa",          "WireSpaController::page").name("wire.spa");
        Route.post("/api/wire/spa",     "WireSpaController::update");

        // CRUD 任务列表演示（真实 SQLite 数据库 + 精准刷新）
        Route.get("/wire/tasks",          "WireListController::page").name("wire.tasks");
        Route.post("/api/wire/tasks",     "WireListController::update");

        // 命名组件演示（Toast + Confirm）
        Route.get("/wire/components",          "WireComponentController::page").name("wire.components");
        Route.post("/api/wire/components",     "WireComponentController::update");

        // 数据库文件存储演示
        Route.get("/demo/storage", "StorageDemoController::index").name("storage.demo");
        Route.post("/demo/storage/upload", "StorageDemoController::upload");
        Route.get("/demo/storage/download", "StorageDemoController::download");
        Route.get("/demo/storage/view", "StorageDemoController::view");
        Route.get("/demo/storage/delete", "StorageDemoController::delete");
    }
}
