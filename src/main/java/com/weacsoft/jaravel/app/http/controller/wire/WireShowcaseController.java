package com.weacsoft.jaravel.app.http.controller.wire;

import com.weacsoft.jaravel.vendor.http.controller.Controllers;
import com.weacsoft.jaravel.vendor.http.controller.request.Request;
import com.weacsoft.jaravel.vendor.http.controller.response.Response;
import com.weacsoft.jaravel.vendor.http.controller.response.ResponseBuilder;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Wire 演示 — 首页（仪表盘 + 记录列表 + 导航入口）
 * <p>
 * 演示 Wire 透明导航的核心概念：
 * <ul>
 *   <li>同一个 Blade 继承链（wire-layout → dashboard/records）</li>
 *   <li>多个 Controller 方法之间通过 wire-navigate 属性切换</li>
 *   <li>后端无需感知导航，只需正常渲染页面</li>
 * </ul>
 */
public class WireShowcaseController implements Controllers {

    public Response index(Request request) {
        return ResponseBuilder.view("wire/index", Map.of(
                "recordCount", 128,
                "monthCount",  36,
                "pendingCount", 8,
                "doneCount",   42
        ));
    }

    public Response records(Request request) {
        List<Map<String, Object>> records = List.of(
                mkRecord(1, "服务器部署",    "income",  5000.00, "2026-08-01", "done"),
                mkRecord(2, "域名续费",      "income",  120.00,  "2026-07-28", "done"),
                mkRecord(3, "云服务订阅",    "expense", 299.00,  "2026-07-15", "pending"),
                mkRecord(4, "数据库维护",    "expense", 800.00,  "2026-07-10", "pending"),
                mkRecord(5, "CDN 流量费用",  "expense", 450.00,  "2026-06-30", "done"),
                mkRecord(6, "SSL 证书续费",  "expense", 200.00,  "2026-06-15", "done"),
                mkRecord(7, "日志存储",      "expense", 150.00,  "2026-06-01", "pending"),
                mkRecord(8, "监控服务",      "expense", 300.00,  "2026-05-20", "done")
        );
        return ResponseBuilder.view("wire/records", Map.of("records", records));
    }

    public Response update(Request request) {
        String target = request.get("target");
        if ("refresh".equals(target)) {
            return ResponseBuilder.json(Map.of(
                    "recordCount", 128,
                    "monthCount",  36,
                    "pendingCount", 8,
                    "doneCount",   42
            ));
        }
        return ResponseBuilder.json(Map.of("ok", true));
    }

    public Response recordsUpdate(Request request) {
        return ResponseBuilder.json(Map.of("ok", true));
    }

    private Map<String, Object> mkRecord(int id, String title, String type,
                                          double amount, String date, String status) {
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("id", id);
        r.put("title", title);
        r.put("type", type);
        r.put("amount", amount);
        r.put("date", date);
        r.put("status", status);
        return r;
    }
}
