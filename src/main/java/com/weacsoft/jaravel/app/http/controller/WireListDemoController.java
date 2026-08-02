package com.weacsoft.jaravel.app.http.controller;

import com.weacsoft.jaravel.vendor.core.pagination.Paginator;
import com.weacsoft.jaravel.vendor.http.controller.Controllers;
import com.weacsoft.jaravel.vendor.http.controller.request.Request;
import com.weacsoft.jaravel.vendor.http.controller.response.Response;
import com.weacsoft.jaravel.vendor.wire.WireService;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Wire 列表演示：静态数组模拟数据库（无需真实 DB）。
 *
 * <p>演示要点（对应你提出的 1/2/4/5/6/7 点）：
 * <ul>
 *   <li>第5点 查库 + CRUD：数据来自 {@code DB} 静态 Map，增删改后重新加载权威列表</li>
 *   <li>第6点 分页：用 core 的 {@link Paginator}，模板里用原生 Laravel 分页 {@code $paginator->links('pageinator')}</li>
 *   <li>第1点 精准刷新：{@code Wire.refresh(['list'])} 只刷 list section，后端走统一 update 通道，无全页刷新特例</li>
 *   <li>第4点 懒加载：page() 不预加载列表（只给空壳），由前端 load 后发 $refresh 拉取</li>
 *   <li>第2点 列表交互：每行的 {@code data-wire-key} 让前端做最小化 diff，保留输入框/勾选状态</li>
 * </ul>
 *
 * <p>后端设计原则：不区分「全页刷新 / 局部刷新 / 分页 / 懒加载」，
 * 它们全部是「执行 action → 重渲染请求的 section」。
 */
@Component
public class WireListDemoController implements Controllers {

    private static final String TEMPLATE = "wire-list-demo";
    private static final String UPDATE_URL = "/api/wire/list-demo";

    /** 静态数组，模拟数据库表（id -> 行）。 */
    private static final Map<Integer, Item> DB = new LinkedHashMap<>();
    private static final AtomicInteger SEQ = new AtomicInteger(0);

    static {
        DB.put(SEQ.incrementAndGet(), new Item(SEQ.get(), "任务 A", false));
        DB.put(SEQ.incrementAndGet(), new Item(SEQ.get(), "任务 B", true));
        DB.put(SEQ.incrementAndGet(), new Item(SEQ.get(), "任务 C", false));
        DB.put(SEQ.incrementAndGet(), new Item(SEQ.get(), "任务 D", false));
        DB.put(SEQ.incrementAndGet(), new Item(SEQ.get(), "任务 E", true));
        DB.put(SEQ.incrementAndGet(), new Item(SEQ.get(), "任务 F", false));
        DB.put(SEQ.incrementAndGet(), new Item(SEQ.get(), "任务 G", false));
        DB.put(SEQ.incrementAndGet(), new Item(SEQ.get(), "任务 H", true));
        DB.put(SEQ.incrementAndGet(), new Item(SEQ.get(), "任务 I", false));
        DB.put(SEQ.incrementAndGet(), new Item(SEQ.get(), "任务 J", false));
        DB.put(SEQ.incrementAndGet(), new Item(SEQ.get(), "任务 K", false));
        DB.put(SEQ.incrementAndGet(), new Item(SEQ.get(), "任务 L", true));
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

    /**
     * 初始页面。列表初始为空壳（懒加载演示），分页器也留空，
     * 真正数据在 DOMContentLoaded 后由前端发 $refresh 拉取（见模板末尾脚本）。
     */
    public Response page(Request request) {
        return WireService.of(TEMPLATE, UPDATE_URL, new LinkedHashMap<>())
                .once("title", "Wire 列表演示 - CRUD + 分页 + 精准刷新")
                .once("page", 1)
                .once("perPage", 5)
                .once("items", new ArrayList<>())   // 空壳，等前端懒加载
                .once("total", DB.size())
                .once("paginatorHtml", "")
                .responseWire();
    }

    /**
     * 统一更新端点：所有交互（增删改、刷新、分页）都走这里。
     * 不写任何「全页刷新」特例。
     */
    public Response update(Request request) {
        // 从请求解析 snapshot + action + params(含 page/perPage 等) + sections
        return WireService.from(request, TEMPLATE, UPDATE_URL)
                .once("title", "Wire 列表演示 - CRUD + 分页 + 精准刷新")
                .once("page", 1)
                .once("perPage", 5)
                .once("items", new ArrayList<>())
                .once("paginatorHtml", "")
                // —— action 处理器 ——

                // 第5点：新增一条（模拟 INSERT），然后重新加载权威列表
                .action("addItem", c -> {
                    int id = SEQ.incrementAndGet();
                    c.set("name", c.getStr("name").trim());
                    DB.put(id, new Item(id, c.getStr("name").isEmpty() ? ("任务 " + id) : c.getStr("name"), false));
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
                        row.name = c.getStr("name");
                        row.done = c.getStr("done").equals("1") || c.getStr("done").equals("true");
                        DB.put(id, row);
                    }
                    loadList(c);
                })

                // 第1点 / 第7点：刷新 list section（别人改了后端、或懒加载首次拉取），不改动数据
                .action("$refresh", c -> loadList(c))

                // 第6点：分页（page/perPage 来自前端拦截分页链接解析出的 params，已合并进 data）
                .action("$paginate", c -> loadList(c))
                .responseUpdate();
    }

    /**
     * 加载「当前页」的权威数据，并渲染分页器 HTML。
     * 这一步把查库结果塞回 data，前端只会收到 list section 的更新。
     */
    private static void loadList(WireService c) {
        int page = c.getInt("pageNum");
        int perPage = c.getInt("perPage");
        if (page < 1) page = 1;
        if (perPage < 1) perPage = 5;

        List<Item> all = new ArrayList<>(DB.values());
        int total = all.size();
        int from = Math.min((page - 1) * perPage, Math.max(0, total - 1));
        int to = Math.min(from + perPage, total);
        List<Item> pageItems = new ArrayList<>(all.subList(from, to));

        // 转成 Map 便于模板通过 $item['name'] 访问（jblade 输出对象字段更稳妥）
        List<Map<String, Object>> rows = pageItems.stream().map(it -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", it.id);
            m.put("name", it.name);
            m.put("done", it.done);
            return m;
        }).collect(Collectors.toList());

        c.set("items", rows);
        c.set("total", total);
        c.set("pageNum", page);
        c.set("perPage", perPage);

        // 第6点：用 core Paginator 生成原生 Laravel 风格分页（视图 pageinator.jblade 完全不动，
        // 对应 jblade 视图名 layouts.mdui.pageinator，与 PHP 版契约一致）
        Paginator<Map<String, Object>> paginator = new Paginator<>(rows, total, perPage, page);
        paginator.setPath("/wire-list-demo");
        // 把分页器 HTML 作为 Htmlable 渲染结果存入 data，模板里用 {{ $paginatorHtml }} 输出
        c.set("paginatorHtml", paginator.links("layouts.mdui.pageinator").toHtml());
    }
}
