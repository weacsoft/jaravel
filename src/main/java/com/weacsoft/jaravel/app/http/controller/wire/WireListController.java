package com.weacsoft.jaravel.app.http.controller.wire;

import com.weacsoft.jaravel.app.model.Task;
import com.weacsoft.jaravel.vendor.http.controller.Controllers;
import com.weacsoft.jaravel.vendor.http.controller.request.Request;
import com.weacsoft.jaravel.vendor.http.controller.response.Response;
import com.weacsoft.jaravel.vendor.http.controller.response.ResponseBuilder;
import com.weacsoft.jaravel.vendor.wire.WireRequest;
import com.weacsoft.jaravel.vendor.wire.WireResponse;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Wire CRUD 演示 — 基于 SQLite 真实数据库的任务列表。
 * <p>
 * 演示要点：增删改由 {@code wire:click} / {@code wire:submit} 触发，
 * 服务端只回传 {@code summary} 与 {@code list} 两个 section，页面其余部分（含新增输入框）保持原状。
 * <p>
 * 前后端契约：请求体为 {@code wire_body=<json>}（snapshot/action/params/sections），
 * 响应必须是 {@code {sections:{...}, snapshot, effects}} —— 由 {@link WireResponse#update} 统一构造。
 */
public class WireListController implements Controllers {

    /** 本页参与局部刷新的 section。 */
    private static final List<String> SECTIONS = List.of("summary", "list");

    private static final String TEMPLATE = "wire/task-list";

    public Response page(Request request) {
        return ResponseBuilder.view(TEMPLATE, listData());
    }

    public Response update(Request request) {
        WireRequest wr = WireRequest.from(request);
        String action = wr.getAction();
        Map<String, Object> params = wr.getParams();

        if ("add".equals(action)) {
            String name = text(params.get("name"));
            if (!name.isBlank()) {
                Task t = new Task();
                t.setName(name);
                t.setDone(false);
                t.save();
            }
        } else if ("update".equals(action)) {
            Integer id = intOf(params.get("id"));
            String newName = text(params.get("name"));
            if (id != null && !newName.isBlank()) {
                Task t = Task.findById(id);
                if (t != null) {
                    t.setName(newName);
                    t.save();
                }
            }
        } else if ("toggle".equals(action)) {
            Integer id = intOf(params.get("id"));
            if (id != null) {
                Task t = Task.findById(id);
                if (t != null) {
                    t.setDone(!Boolean.TRUE.equals(t.getDone()));
                    t.save();
                }
            }
        } else if ("delete".equals(action)) {
            Integer id = intOf(params.get("id"));
            if (id != null) {
                Task.self().newQuery().where("id", id).delete();
            }
        }

        // 只回传变化的 section，前端 wire.js 定位 [wire:section] 后替换其内容
        return WireResponse.update(TEMPLATE, listData(), SECTIONS);
    }

    /** 从数据库读取全量任务并整理成模板数据。 */
    private Map<String, Object> listData() {
        List<Task> tasks = Task.self().findAll().toObjectList();
        List<Map<String, Object>> taskList = new ArrayList<>();
        for (Task t : tasks) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", t.getId());
            m.put("name", t.getName());
            m.put("done", t.getDone());
            taskList.add(m);
        }
        long doneCount = taskList.stream().filter(x -> Boolean.TRUE.equals(x.get("done"))).count();

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("tasks", taskList);
        data.put("total", taskList.size());
        data.put("doneCount", doneCount);
        return data;
    }

    /** wire params 的值可能是字符串或数字，统一转成 trim 后的文本。 */
    private static String text(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    /** wire:param-id 传来的通常是字符串，宽松解析为整数；非法值返回 null 而不是抛异常。 */
    private static Integer intOf(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.valueOf(String.valueOf(value).trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
