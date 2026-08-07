package com.weacsoft.jaravel.app.http.controller.wire;

import com.weacsoft.jaravel.vendor.http.controller.Controllers;
import com.weacsoft.jaravel.vendor.http.controller.request.Request;
import com.weacsoft.jaravel.vendor.http.controller.response.Response;
import com.weacsoft.jaravel.vendor.http.controller.response.ResponseBuilder;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Wire SPA 演示 — 左侧菜单 + 右侧内容切换
 * <p>
 * 演示：同一个 wire-layout 布局下，三个不同的 Blade 模板通过 sidebar 导航切换，
 * 只有 content 和 sidebar 区域 diff，顶栏保持不变。
 */
public class WireSpaController implements Controllers {

    public Response page(Request request) {
        String action = request.get("action");
        if ("tasks".equals(action)) {
            return taskList(request);
        } else if ("about".equals(action)) {
            return about(request);
        }
        return overview(request);
    }

    private Response overview(Request request) {
        return ResponseBuilder.view("wire/spa-overview", Map.of("currentPage", "overview"));
    }

    private Response taskList(Request request) {
        List<Map<String, Object>> tasks = List.of(
                mkTask(1, "完成 Wire 导航演示",    true),
                mkTask(2, "整理控制器结构",        true),
                mkTask(3, "编写测试用例",          false),
                mkTask(4, "性能优化",              false),
                mkTask(5, "部署到生产环境",        false)
        );
        return ResponseBuilder.view("wire/spa-task-list", Map.of(
                "currentPage", "tasks",
                "tasks", tasks
        ));
    }

    private Response about(Request request) {
        return ResponseBuilder.view("wire/spa-about", Map.of("currentPage", "about"));
    }

    public Response update(Request request) {
        String action = request.get("action");
        if ("add_task".equals(action)) {
            return ResponseBuilder.json(Map.of("ok", true, "message", "任务已添加"));
        }
        if ("toggle_task".equals(action)) {
            return ResponseBuilder.json(Map.of("ok", true));
        }
        return ResponseBuilder.json(Map.of("ok", true));
    }

    private Map<String, Object> mkTask(int id, String name, boolean done) {
        Map<String, Object> t = new LinkedHashMap<>();
        t.put("id", id);
        t.put("name", name);
        t.put("done", done);
        return t;
    }
}
