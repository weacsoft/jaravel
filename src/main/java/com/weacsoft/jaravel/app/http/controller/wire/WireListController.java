package com.weacsoft.jaravel.app.http.controller.wire;

import com.weacsoft.jaravel.app.model.Task;
import com.weacsoft.jaravel.vendor.http.controller.Controllers;
import com.weacsoft.jaravel.vendor.http.controller.request.Request;
import com.weacsoft.jaravel.vendor.http.controller.response.Response;
import com.weacsoft.jaravel.vendor.http.controller.response.ResponseBuilder;
import com.weacsoft.jaravel.vendor.wire.WireManager;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Wire CRUD 演示 — 基于 SQLite 真实数据库的任务列表
 * <p>
 * 演示：增删改操作后精准刷新列表（只更新 list section），
 * 输入框状态在刷新后保留（通过 data-wire-key 实现最小化 diff）。
 */
public class WireListController implements Controllers {

    public Response page(Request request) {
        List<Task> tasks = Task.self().findAll().toObjectList();
        List<Map<String, Object>> taskList = new ArrayList<>();
        for (Task t : tasks) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", t.getId());
            m.put("name", t.getName());
            m.put("done", t.getDone());
            taskList.add(m);
        }
        long doneCount = taskList.stream().filter(t -> Boolean.TRUE.equals(t.get("done"))).count();
        return ResponseBuilder.view("wire/task-list", Map.of(
                "tasks", taskList,
                "total", taskList.size(),
                "doneCount", doneCount
        ));
    }

    public Response update(Request request) {
        String action = request.get("action");

        if ("add".equals(action)) {
            String name = request.get("name");
            if (name != null && !name.isBlank()) {
                Task t = new Task();
                t.setName(name.trim());
                t.setDone(false);
                t.save();
            }
        } else if ("update".equals(action)) {
            Integer id = request.get("id", Integer.class);
            String newName = request.get("name");
            if (id != null && newName != null && !newName.isBlank()) {
                Task t = Task.findById(id);
                if (t != null) {
                    t.setName(newName.trim());
                    t.save();
                }
            }
        } else if ("toggle".equals(action)) {
            Integer id = request.get("id", Integer.class);
            if (id != null) {
                Task t = Task.findById(id);
                if (t != null) {
                    t.setDone(!t.getDone());
                    t.save();
                }
            }
        } else if ("delete".equals(action)) {
            Integer id = request.get("id", Integer.class);
            if (id != null) {
                Task.self().newQuery().where("id", id).delete();
            }
        }

        // 重新加载列表数据
        List<Task> tasks = Task.self().findAll().toObjectList();
        List<Map<String, Object>> taskList = new ArrayList<>();
        for (Task t : tasks) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", t.getId());
            m.put("name", t.getName());
            m.put("done", t.getDone());
            taskList.add(m);
        }

        // 只返回 list section 的 diff
        long doneCount = taskList.stream().filter(x -> Boolean.TRUE.equals(x.get("done"))).count();
        String html = WireManager.renderSection("wire/task-list", "list", Map.of(
                "tasks", taskList,
                "total", taskList.size(),
                "doneCount", doneCount
        ));
        return ResponseBuilder.json(Map.of("list", html));
    }
}
