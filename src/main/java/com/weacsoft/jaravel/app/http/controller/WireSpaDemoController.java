package com.weacsoft.jaravel.app.http.controller;

import com.weacsoft.jaravel.vendor.core.pagination.Paginator;
import com.weacsoft.jaravel.vendor.http.controller.Controllers;
import com.weacsoft.jaravel.vendor.http.controller.request.Request;
import com.weacsoft.jaravel.vendor.http.controller.response.Response;
import com.weacsoft.jaravel.vendor.wire.WireService;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Wire SPA 演示（第3点：SPA 导航 + 第7点：懒加载/分页/精准刷新组合）。
 *
 * <p>布局：左侧菜单（wire:nav-menu）+ 右侧内容区（wire:nav-content / wire:section="content"）。
 * 点击菜单只刷新右侧 content section，不整页跳转。content 根据当前 page 渲染不同子视图：
 * <ul>
 *   <li>home —— 概览卡片</li>
 *   <li>list —— 任务列表（懒加载 + 原生 Laravel 分页 + 精准刷新/CRUD，即第7点组合）</li>
 *   <li>about —— 说明</li>
 * </ul>
 *
 * <p>后端不写任何「整页刷新」特殊分支；导航、懒加载、分页、CRUD 全部走统一的
 * WireService 通道（page 用 responseWire，update 用 responseUpdate）。
 */
@Component
public class WireSpaDemoController implements Controllers {

    private static final String TEMPLATE = "wire-spa-demo";
    private static final String UPDATE_URL = "/api/wire/spa-demo";

    /** 模拟数据库（任务表）。 */
    private static final Map<Integer, Item> DB = new ConcurrentHashMap<>();
    private static final AtomicInteger SEQ = new AtomicInteger(0);

    static {
        String[] seed = {"设计登录页", "编写接口", "联调支付", "修复分页 bug",
                "优化首屏", "补充单测", "撰写文档", "压测网关",
                "梳理需求", "评审方案", "灰度发布", "复盘总结"};
        for (String name : seed) {
            int id = SEQ.incrementAndGet();
            DB.put(id, new Item(id, name, id % 3 == 0));
        }
    }

    /** 模拟数据库行。 */
    public static class Item {
        public int id;
        public String name;
        public boolean done;
        public Item(int id, String name, boolean done) {
            this.id = id; this.name = name; this.done = done;
        }
    }

    /** 从 DB 加载当前页列表，并写入 items/total/pageNum/perPage/paginatorHtml。
     *  <p>分页用独立字段 {@code pageNum}（数字），与导航字段 {@code page}（home/list/about 字符串）解耦，
     *  避免 loadList 覆盖导航状态。</p>
     */
    private static void loadList(WireService c) {
        Integer p = c.getInt("pageNum");
        Integer pp = c.getInt("perPage");
        int pageNum = (p == null || p < 1) ? 1 : p;
        int perPage = (pp == null || pp < 1) ? 5 : pp;

        List<Item> all = new ArrayList<>(DB.values());
        int total = all.size();
        int from = Math.min((pageNum - 1) * perPage, Math.max(0, total - 1));
        int to = Math.min(from + perPage, total);
        List<Item> rows = new ArrayList<>(all.subList(from, to));

        List<Map<String, Object>> items = new ArrayList<>();
        for (Item it : rows) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", it.id);
            m.put("name", it.name);
            m.put("done", it.done);
            items.add(m);
        }
        c.set("items", items);
        c.set("total", total);
        c.set("pageNum", pageNum);
        c.set("perPage", perPage);
        // 列表相关动作（分页/CRUD）始终渲染列表视图，避免 page 字段残留为 home 导致分支误判
        c.set("page", "list");

        // 第6点：原生 Laravel 分页器（pageinator.jblade 语义不变），视图名对应 layouts.mdui.pageinator
        Paginator<Map<String, Object>> paginator = new Paginator<>(items, total, perPage, pageNum);
        paginator.setPath("/wire-spa-demo");
        c.set("paginatorHtml", paginator.links("layouts.mdui.pageinator").toHtml());
    }

    /** 首屏：渲染整页（含左侧菜单框架），右侧 content 默认 home。 */
    public Response page(Request request) {
        return WireService.of(TEMPLATE, UPDATE_URL, new LinkedHashMap<>())
                .once("page", "home")        // 导航目标（字符串）
                .once("pageNum", 1)          // 列表当前页码（数字）
                .once("items", new ArrayList<>())   // 懒加载占位：首屏不取数据
                .once("total", DB.size())
                .once("perPage", 5)
                .once("paginatorHtml", "")
                .responseWire();
    }

    /** 统一更新端点：导航 / 懒加载 / 分页 / CRUD 全部走这里，不写全页刷新特例。 */
    public Response update(Request request) {
        return WireService.from(request, TEMPLATE, UPDATE_URL)
                // 第3点：SPA 导航切换右侧内容（只重渲染 content section）。导航字段 page 保持原值。
                .action("$nav", c -> {
                    String target = c.getStr("page");
                    if (target == null || target.isEmpty()) target = "home";
                    c.set("page", target);
                    if ("list".equals(target)) loadList(c);   // 进入 list 时才取数据（懒加载/导航触发）
                })
                // 第1点 / 第7点：刷新 list section（别人改了后端、或懒加载首次拉取），不改动数据
                .action("$refresh", c -> loadList(c))
                // 第6点：分页（page/perPage 来自前端拦截分页链接解析出的 params，已合并进 data）
                .action("$paginate", c -> loadList(c))
                // 第5点：新增一条（模拟 INSERT），然后重新加载权威列表
                .action("addItem", c -> {
                    int id = SEQ.incrementAndGet();
                    String name = c.getStr("name");
                    if (name == null) name = "";
                    name = name.trim();
                    DB.put(id, new Item(id, name.isEmpty() ? ("任务 " + id) : name, false));
                    loadList(c);
                })
                // 第5点：删除单条（模拟 DELETE by id）
                .action("deleteItem", c -> {
                    int id = c.getInt("id");
                    DB.remove(id);
                    loadList(c);
                })
                // 第5点：更新单条（模拟 UPDATE by id：改名 + 勾选态）
                .action("updateItem", c -> {
                    int id = c.getInt("id");
                    Item row = DB.get(id);
                    if (row != null) {
                        String nm = c.getStr("name");
                        if (nm != null) row.name = nm;
                        String done = c.getStr("done");
                        row.done = "1".equals(done) || "true".equals(done);
                    }
                    loadList(c);
                })
                .responseUpdate();
    }
}
