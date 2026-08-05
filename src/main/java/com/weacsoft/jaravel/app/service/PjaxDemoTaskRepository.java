package com.weacsoft.jaravel.app.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * PJAX 演示用的内存任务表（不依赖数据库，纯静态数据）。
 * <p>
 * 供 {@code PjaxHomeController} 与 {@code PjaxListController} 共享，
 * 两个控制器彼此独立，只共享数据源。
 */
public final class PjaxDemoTaskRepository {

    /** 任务名称种子 */
    private static final String[] NAMES = {
            "设计登录页", "编写用户接口", "联调支付网关", "修复分页越界",
            "优化首屏加载", "补充单元测试", "撰写接入文档", "压测消息队列",
            "梳理需求清单", "评审技术方案", "灰度发布验证", "线上复盘总结"
    };

    private static final List<Map<String, Object>> ROWS;

    static {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (int i = 0; i < NAMES.length; i++) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", i + 1);
            row.put("name", NAMES[i]);
            row.put("done", (i + 1) % 3 == 0);
            rows.add(row);
        }
        ROWS = Collections.unmodifiableList(rows);
    }

    private PjaxDemoTaskRepository() {
    }

    /**
     * @return 任务总数
     */
    public static int total() {
        return ROWS.size();
    }

    /**
     * @return 已完成任务数
     */
    public static int doneCount() {
        int count = 0;
        for (Map<String, Object> row : ROWS) {
            if (Boolean.TRUE.equals(row.get("done"))) {
                count++;
            }
        }
        return count;
    }

    /**
     * 分页取数。
     *
     * @param pageNum 页码（从 1 开始）
     * @param perPage 每页条数
     * @return 当前页数据（越界返回空列表）
     */
    public static List<Map<String, Object>> page(int pageNum, int perPage) {
        int from = Math.max(0, (pageNum - 1) * perPage);
        if (from >= ROWS.size()) {
            return new ArrayList<>();
        }
        int to = Math.min(from + perPage, ROWS.size());
        return new ArrayList<>(ROWS.subList(from, to));
    }
}
