package com.weacsoft.jaravel.app.http.controller;

import com.weacsoft.jaravel.vendor.http.controller.request.Request;
import com.weacsoft.jaravel.vendor.http.controller.response.Response;
import com.weacsoft.jaravel.vendor.wire.WireService;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * Wire 演示控制器。
 * <p>
 * 使用 {@link WireService} 流式 API，零 if/switch。
 */
@Component
public class WireDemoController {

    private static final String TEMPLATE = "wire-demo";
    private static final String UPDATE_URL = "/api/wire/demo";

    /**
     * 初始页面渲染。
     */
    public Response page(Request request) {
        return WireService.of(TEMPLATE, UPDATE_URL, new java.util.LinkedHashMap<>())
                .once("title", "Wire Demo - 部分更新演示")
                .once("count", 0)
                .once("message", "")
                .once("items", Arrays.asList("苹果", "香蕉", "橙子"))
                .once("appName", "jaravel")
                .responseWire();
    }

    /**
     * Wire 更新端点。
     * <p>
     * 从请求解析 → 填默认值 → 注册 action → 返回响应，全程链式。
     */
    public Response update(Request request) {
        return WireService.from(request, TEMPLATE, UPDATE_URL)
                // 默认值（snapshot 为空或字段缺失时使用）
                .once("title", "Wire Demo - 部分更新演示")
                .once("count", 0)
                .once("message", "")
                .once("items", new java.util.ArrayList<>(Arrays.asList("苹果", "香蕉", "橙子")))
                .once("appName", "jaravel")
                // action 处理器
                .action("increment", c -> c.set("count", c.getInt("count") + 1))
                .action("decrement", c -> c.set("count", c.getInt("count") - 1))
                .action("reset", c -> { c.set("count", 0); c.set("message", ""); })
                .action("addItem", c -> {
                    var items = c.getList("items");
                    items.add("项目 " + (items.size() + 1));
                })
                .action("removeItem", c -> {
                    var items = c.getList("items");
                    if (!items.isEmpty()) items.remove(items.size() - 1);
                })
                // 生成响应（自动分派 action + 渲染 section）
                .responseUpdate();
    }
}
