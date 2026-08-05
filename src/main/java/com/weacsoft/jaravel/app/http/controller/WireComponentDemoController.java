package com.weacsoft.jaravel.app.http.controller;

import com.weacsoft.jaravel.vendor.http.controller.Controllers;
import com.weacsoft.jaravel.vendor.http.controller.request.Request;
import com.weacsoft.jaravel.vendor.http.controller.response.Response;
import com.weacsoft.jaravel.vendor.http.controller.response.ResponseBuilder;
import com.weacsoft.jaravel.vendor.wire.WireResponse;
import com.weacsoft.jaravel.vendor.wire.WireService;
import com.weacsoft.jaravel.vendor.wire.component.WireComponents;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Wire 命名组件（toast / confirm 等临时事务型片段）演示控制器。
 * <p>
 * 演示两种下发路径：
 * <ul>
 *   <li><b>首屏</b>：控制器里 {@code WireComponents.push(name, params)}，由 {@code WireOutlet} 中间件
 *       自动注入 outlet 容器 + bootstrap，前端 {@code wire-component.js} 挂载；</li>
 *   <li><b>Wire 更新</b>：action 里 {@code c.responseComponent(name, params)}，随 {@code effects.components}
 *       下发，wire.js 无感挂载。</li>
 * </ul>
 * 无论哪条路径，每个实例都有独立生命周期（onCreate / onStart / onStop / onDestroy），
 * 模板内调用 {@code wire.stop()} 即表示“展示完成，移除我”，多个同名组件互相隔离。
 */
@Component
public class WireComponentDemoController implements Controllers {

    private static final String TEMPLATE = "wire-component-demo";
    private static final String UPDATE_URL = "/api/wire-component-demo";

    /**
     * 主演示页（Wire 页面）：首屏自动挂载一条欢迎 toast，并提供按钮触发 toast / confirm / 多实例。
     */
    public Response page(Request request) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("appName", "jaravel");

        // 首屏欢迎 toast：走 WireOutlet 首屏 bootstrap 路径（零配置自动注入 outlet + 运行时）
        WireComponents.push("toast", Map.of(
                "level", "info",
                "icon", "👋",
                "message", "欢迎来到 Wire 命名组件演示（首屏自动挂载）",
                "ttl", 4000
        ));
        return WireResponse.wire(TEMPLATE, data, UPDATE_URL);
    }

    /**
     * Wire 更新端点：toast / confirm / multi 三个 action 各自下发命名组件，随 effects.components 无感返回。
     */
    public Response update(Request request) {
        return WireService.from(request, TEMPLATE, UPDATE_URL)
                .action("toast", c -> c.responseComponent("toast", Map.of(
                        "level", "success", "icon", "✓",
                        "message", "这是一次 Wire 更新响应下发的 toast", "ttl", 3500)))
                .action("confirm", c -> c.responseComponent("confirm", Map.of(
                        "title", "确认操作", "message", "确定要执行该操作吗？")))
                .action("multi", c -> {
                    for (int i = 1; i <= 3; i++) {
                        c.responseComponent("toast", Map.of(
                                "level", "info", "icon", "#" + i,
                                "message", "第 " + i + " 条 toast —— 每个实例独立生命周期、互不影响", "ttl", 4000));
                    }
                })
                .responseUpdate();
    }

    /**
     * 自定义加载位置演示：普通页面，用 {!! wire_outlet() !!} 在指定位置显式放置 outlet。
     */
    public Response plain(Request request) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("appName", "jaravel");

        // 这条 toast 会被挂载到页面中用 {!! wire_outlet() !!} 指定的自定义位置（而非默认 body 末尾）
        WireComponents.push("toast", Map.of(
                "level", "warning", "icon", "📍",
                "message", "这条 toast 挂载在你用 {!! wire_outlet() !!} 指定的自定义位置", "ttl", 6000));
        return ResponseBuilder.view("wire-component-plain", data);
    }
}
