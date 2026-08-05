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
import java.util.List;
import java.util.Map;

/**
 * PJAX 演示 —— 任务列表页控制器（{@code GET /list}，支持 {@code ?page=N} 分页）。
 * <p>
 * 同样<b>没有一行 PJAX 相关代码</b>：分页参数用最普通的 {@code request.query("page")} 读取，
 * 分页链接在模板里也是普通的 {@code <a href="/list?page=2">}。
 * 翻页时框架自动判定只有 {@code content} 与 {@code title} 区域变化，
 * 侧栏、探针区、脚本区指纹不变，前端完全不触碰它们的 DOM。
 */
@Component
public class PjaxListController implements Controllers {

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

    /** 每页条数 */
    private static final int PER_PAGE = 5;

    /**
     * 列表页：分页展示任务。
     */
    public Response index(Request request) {
        int total = PjaxDemoTaskRepository.total();
        int lastPage = Math.max(1, (total + PER_PAGE - 1) / PER_PAGE);
        int pageNum = parsePage(request.query("page"), lastPage);

        List<Map<String, Object>> items = PjaxDemoTaskRepository.page(pageNum, PER_PAGE);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("items", items);
        data.put("total", total);
        data.put("perPage", PER_PAGE);
        data.put("pageNum", pageNum);
        data.put("lastPage", lastPage);
        data.put("renderedAt", LocalTime.now().format(TIME));
        return ResponseBuilder.view("pjax.list", data);
    }

    /**
     * 解析页码，越界回退到合法范围。
     */
    private static int parsePage(String raw, int lastPage) {
        int pageNum = 1;
        if (raw != null && !raw.isEmpty()) {
            try {
                pageNum = Integer.parseInt(raw.trim());
            } catch (NumberFormatException ignored) {
                pageNum = 1;
            }
        }
        if (pageNum < 1) {
            pageNum = 1;
        }
        if (pageNum > lastPage) {
            pageNum = lastPage;
        }
        return pageNum;
    }
}
