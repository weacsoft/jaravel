package com.weacsoft.jaravel.app.http.controller;

import com.weacsoft.jaravel.app.model.Task;
import com.weacsoft.jaravel.vendor.core.pagination.Paginator;
import com.weacsoft.jaravel.vendor.http.controller.Controllers;
import com.weacsoft.jaravel.vendor.http.controller.request.Request;
import com.weacsoft.jaravel.vendor.http.controller.response.Response;
import com.weacsoft.jaravel.vendor.wire.WireService;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Wire 列表演示：基于 SQLite 真实数据库（使用 database 模块的 BaseModel）。
 *
 * <p>演示要点：
 * <ul>
 *   <li>CRUD：数据来自 SQLite tasks 表，增删改后重新加载权威列表</li>
 *   <li>分页：用 BaseModel 内置 {@code paginate()} + core 的 {@link Paginator}</li>
 *   <li>精准刷新：{@code Wire.refresh(['list'])} 只刷 list section，后端走统一 update 通道</li>
 *   <li>列表交互：每行的 {@code data-wire-key} 让前端做最小化 diff，保留输入框/勾选状态</li>
 * </ul>
 */
@Component
public class WireListDemoController implements Controllers {

    private static final String TEMPLATE = "wire-list-demo";
    private static final String UPDATE_URL = "/api/wire/list-demo";

    /**
     * 初始页面。首屏直接渲染真实数据。
     */
    public Response page(Request request) {
        WireService c = WireService.of(TEMPLATE, UPDATE_URL, new LinkedHashMap<>())
                .once("title", "Wire 列表演示 - CRUD + 分页 + 精准刷新（SQLite）")
                .once("page", 1)
                .once("perPage", 5)
                .once("items", new ArrayList<>())
                .once("total", 0)
                .once("paginatorHtml", "");
        ensureSeedData();
        loadList(c);
        return c.responseWire();
    }

    /**
     * 统一更新端点：所有交互（增删改、刷新、分页）都走这里。
     */
    public Response update(Request request) {
        return WireService.from(request, TEMPLATE, UPDATE_URL)
                .once("title", "Wire 列表演示 - CRUD + 分页 + 精准刷新（SQLite）")
                .once("page", 1)
                .once("perPage", 5)
                .once("items", new ArrayList<>())
                .once("total", 0)
                .once("paginatorHtml", "")
                // —— action 处理器 ——

                // 新增一条（INSERT）
                .action("addItem", c -> {
                    String raw = c.getStr("name");
                    if (raw == null || raw.trim().isEmpty()) {
                        loadList(c);
                        return;
                    }
                    Task task = new Task();
                    task.setName(raw.trim());
                    task.setDone(false);
                    task.save();
                    loadList(c);
                })

                // 删除单条（DELETE by id）
                .action("deleteItem", c -> {
                    long id = c.getLong("id");
                    System.out.println("[WireListDemo] deleteItem id=" + id);
                    Task.self().newQuery().where("id", id).delete();
                    loadList(c);
                })

                // 更新单条（UPDATE by id：改名 + 勾选态）
                .action("updateItem", c -> {
                    long id = c.getLong("id");
                    String name = c.getStr("name");
                    String doneStr = c.getStr("done");
                    System.out.println("[WireListDemo] updateItem id=" + id + " name=" + name + " done=" + doneStr);
                    Task row = Task.findById(id);
                    if (row != null) {
                        if (name != null && !name.trim().isEmpty()) {
                            row.setName(name.trim());
                        }
                        if (doneStr != null && !doneStr.isEmpty()) {
                            row.setDone(doneStr.equals("1") || doneStr.equals("true"));
                        }
                        row.save();
                        System.out.println("[WireListDemo] updateItem saved: id=" + row.getId() + " name=" + row.getName() + " done=" + row.getDone());
                    } else {
                        System.out.println("[WireListDemo] updateItem: row not found for id=" + id);
                    }
                    loadList(c);
                })

                // 刷新 list section
                .action("$refresh", c -> loadList(c))

                // 输入框实时同步
                .action("$sync", c -> loadList(c))

                // 分页
                .action("$paginate", c -> loadList(c))
                .responseUpdate();
    }

    /**
     * 加载「当前页」的权威数据，并渲染分页器 HTML。
     * 使用 BaseModel 内置 paginate() 一步完成分页查询 + 总数统计。
     */
    private static void loadList(WireService c) {
        int page = c.getInt("pageNum");
        if (page < 1) page = c.getInt("page");
        int perPage = c.getInt("perPage");
        if (page < 1) page = 1;
        if (perPage < 1) perPage = 5;

        // 使用 BaseModel 内置 paginate，一步拿到分页数据 + 总数
        Paginator<Task> taskPaginator = Task.self().paginate(page, perPage);
        List<Task> tasks = taskPaginator.items();
        long total = taskPaginator.total();

        // 转成 Map 便于模板通过 $item['name'] 访问
        List<Map<String, Object>> rows = tasks.stream().map(t -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", t.getId());
            m.put("name", t.getName());
            m.put("done", Boolean.TRUE.equals(t.getDone()));
            return m;
        }).collect(Collectors.toList());

        c.set("items", rows);
        c.set("total", (int) total);
        c.set("pageNum", page);
        c.set("perPage", perPage);

        // 用 Map 列表构建分页器（用于渲染分页 HTML）
        Paginator<Map<String, Object>> paginator = new Paginator<>(rows, (int) total, perPage, page);
        paginator.setPath("/wire-list-demo");
        c.set("paginatorHtml", paginator.links("layouts.mdui.pageinator").toHtml());
    }

    /**
     * 首次访问时播种示例数据（仅在 tasks 表为空时执行）。
     */
    private static void ensureSeedData() {
        if (Task.count() > 0) return;
        String[] names = {"任务 A", "任务 B", "任务 C", "任务 D", "任务 E",
                "任务 F", "任务 G", "任务 H", "任务 I", "任务 J", "任务 K", "任务 L"};
        for (int i = 0; i < names.length; i++) {
            Task t = new Task();
            t.setName(names[i]);
            t.setDone(i % 3 == 2); // 每第三个任务标记为已完成
            t.save();
        }
        System.out.println("[WireListDemo] Seeded " + names.length + " tasks");
    }
}
