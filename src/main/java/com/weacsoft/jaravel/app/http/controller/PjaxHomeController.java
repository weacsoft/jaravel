package com.weacsoft.jaravel.app.http.controller;

import com.weacsoft.jaravel.app.service.PjaxDemoTaskRepository;
import com.weacsoft.jaravel.vendor.http.controller.Controllers;
import com.weacsoft.jaravel.vendor.http.controller.request.Request;
import com.weacsoft.jaravel.vendor.http.controller.response.Response;
import com.weacsoft.jaravel.vendor.http.controller.response.ResponseBuilder;
import org.springframework.stereotype.Component;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * PJAX 演示 —— 概览页控制器（{@code GET /home}）。
 * <p>
 * <b>本类没有一行 PJAX 相关代码</b>：既不判断请求头，也不返回片段，
 * 就是最普通的「取数据 → {@code ResponseBuilder.view(...)}」写法。
 * <p>
 * 无感切换由框架在下游自动完成：
 * <ol>
 *   <li>全局 {@code PjaxMiddleware} 把请求上下文写入 ThreadLocal；</li>
 *   <li>{@code ResponseBuilder.view} 检测到上下文后交给 {@code PjaxViewRenderer}；</li>
 *   <li>首次直接访问 → 返回带区域锚点的完整页面；
 *       从已加载页面点链接过来 → 只返回发生变化的区域。</li>
 * </ol>
 * 因此把本类改回普通页面（移除中间件）也完全不需要改动这里的代码。
 *
 * @see PjaxListController 列表页（独立路由 / 独立控制器 / 独立模板）
 */
@Component
public class PjaxHomeController implements Controllers {

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

    /**
     * 概览页：统计信息 + 跳转入口。
     */
    public Response index(Request request) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("intro", "这是一个独立页面：独立路由 /home、独立控制器 PjaxHomeController、"
                + "独立模板 templates/pjax/home.blade.java。与 /list 之间的切换不整页刷新，"
                + "但两个页面之间没有任何耦合。");
        data.put("total", PjaxDemoTaskRepository.total());
        data.put("done", PjaxDemoTaskRepository.doneCount());
        data.put("pending", PjaxDemoTaskRepository.total() - PjaxDemoTaskRepository.doneCount());
        data.put("renderedAt", LocalTime.now().format(TIME));
        return ResponseBuilder.view("pjax.home", data);
    }
}
