package com.weacsoft.jaravel.app.http.controller.wire;

import com.weacsoft.jaravel.vendor.http.controller.Controllers;
import com.weacsoft.jaravel.vendor.http.controller.request.Request;
import com.weacsoft.jaravel.vendor.http.controller.response.Response;
import com.weacsoft.jaravel.vendor.wire.WireRequest;
import com.weacsoft.jaravel.vendor.wire.WireResponse;

import java.util.Map;

/**
 * Wire 命名组件演示 — Toast 消息 + Confirm 确认弹窗
 * <p>
 * 演示：控制器通过 WireResponse.withComponent() 在 JSON 响应中下发命名组件，
 * 前端 WireComponent 运行时自动无感挂载。
 */
public class WireComponentController implements Controllers {

    public Response page(Request request) {
        return com.weacsoft.jaravel.vendor.http.controller.response.ResponseBuilder
                .view("wire/component-demo", Map.of("currentPage", "components"));
    }

    public Response update(Request request) {
        WireRequest wr = WireRequest.from(request);
        String action = wr.getAction();
        WireResponse resp = WireResponse.of();

        if ("toast_info".equals(action)) {
            resp.withComponent("toast", Map.of("type", "info", "message", "这是一条信息提示"));
        } else if ("toast_success".equals(action)) {
            resp.withComponent("toast", Map.of("type", "success", "message", "操作成功！"));
        } else if ("toast_warning".equals(action)) {
            resp.withComponent("toast", Map.of("type", "warning", "message", "请注意，这是一条警告"));
        } else if ("toast_error".equals(action)) {
            resp.withComponent("toast", Map.of("type", "error", "message", "操作失败，请重试"));
        } else if ("confirm_show".equals(action)) {
            resp.withComponent("confirm", Map.of(
                    "title", "确认删除",
                    "message", "删除后数据将无法恢复，确定要继续吗？",
                    "confirmText", "删除",
                    "cancelText", "取消"
            ));
        } else if ("confirm_delete".equals(action)) {
            resp.withComponent("toast", Map.of("type", "success", "message", "已确认删除"));
        }

        return resp.build();
    }
}
