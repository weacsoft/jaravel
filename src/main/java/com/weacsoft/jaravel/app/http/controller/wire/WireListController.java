package com.weacsoft.jaravel.app.http.controller.wire;

import com.weacsoft.jaravel.app.model.Task;
import com.weacsoft.jaravel.vendor.http.controller.request.Request;
import com.weacsoft.jaravel.vendor.wire.WireController;
import com.weacsoft.jaravel.vendor.wire.WireLocked;
import com.weacsoft.jaravel.vendor.wire.WireView;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Wire CRUD 演示 — 基于 SQLite 真实数据库的任务列表。
 * <p>
 * 演示要点：增删改由 {@code wire:click} / {@code wire:submit} 触发，
 * 服务端只回传 {@code summary} 与 {@code list} 两个 section，页面其余部分（含新增输入框）保持原状。
 */
public class WireListController extends WireController {

    /** 列表数据。标记 @WireLocked:不进入快照(避免把整张表序列化进 wire 快照),
     * 每次 wire 更新时由 refresh() 从 DB 重新查询。 */
    @WireLocked
    public List<Map<String, Object>> tasks;

    @Override
    protected WireView render() {
        return wireView("wire/task-list");
    }

    @Override
    protected String getUpdateRouteName() { return "wire.tasks"; }

    @Override
    protected void mount(Request request) {
        tasks = loadTasks();
    }

    @Override
    protected void refresh(Map<String, Object> params) {
        tasks = loadTasks();
    }

    /** 添加任务：从快照中取 name 字段（wire:model 双向绑定） */
    public void add() {
        String name = String.valueOf(getParam("name", "")).trim();
        if (!name.isBlank()) {
            Task t = new Task();
            t.setName(name);
            t.setDone(false);
            t.save();
        }
    }

    /** 更新任务名 */
    public void update() {
        Integer id = intOf(getParam("id"));
        String name = String.valueOf(getParam("name", "")).trim();
        if (id != null && !name.isBlank()) {
            Task t = Task.findById(id);
            if (t != null) {
                t.setName(name);
                t.save();
            }
        }
    }

    /** 切换完成状态 */
    public void toggle() {
        Integer id = intOf(getParam("id"));
        if (id != null) {
            Task t = Task.findById(id);
            if (t != null) {
                t.setDone(!Boolean.TRUE.equals(t.getDone()));
                t.save();
            }
        }
    }

    /** 删除任务 */
    public void delete() {
        Integer id = intOf(getParam("id"));
        if (id != null) {
            Task.self().newQuery().where("id", id).delete();
        }
    }

    private List<Map<String, Object>> loadTasks() {
        List<Task> taskList = Task.self().findAll().toObjectList();
        List<Map<String, Object>> result = new ArrayList<>();
        for (Task t : taskList) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", t.getId());
            m.put("name", t.getName());
            m.put("done", t.getDone());
            result.add(m);
        }
        return result;
    }

    private Object getParam(String key) {
        return currentRequest != null ? currentRequest.get(key) : null;
    }

    private Object getParam(String key, Object defaultVal) {
        Object v = getParam(key);
        return v == null ? defaultVal : v;
    }

    private Integer intOf(Object value) {
        if (value == null) return null;
        if (value instanceof Number number) return number.intValue();
        try {
            return Integer.valueOf(String.valueOf(value).trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
