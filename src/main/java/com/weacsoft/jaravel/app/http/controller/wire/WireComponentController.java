package com.weacsoft.jaravel.app.http.controller.wire;

import com.weacsoft.jaravel.vendor.wire.WireController;
import com.weacsoft.jaravel.vendor.wire.WireView;

import java.util.Map;

/**
 * Wire 命名组件演示 — Toast 消息 + Confirm 确认弹窗
 * <p>
 * 演示：控制器通过 {@code wire().component("name", params)} 在 action 中下发命名组件，
 * 前端 WireComponent 运行时自动无感挂载。
 */
public class WireComponentController extends WireController {

    @Override
    protected WireView render() {
        return wireView("wire/component-demo", Map.of("currentPage", "components"));
    }

    @Override
    protected String getUpdateRouteName() { return "wire.components"; }

    public void toast_info()   { wire().component("toast", Map.of("type", "info",    "message", "这是一条信息提示")); }
    public void toast_success() { wire().component("toast", Map.of("type", "success", "message", "操作成功！")); }
    public void toast_warning() { wire().component("toast", Map.of("type", "warning", "message", "请注意，这是一条警告")); }
    public void toast_error()   { wire().component("toast", Map.of("type", "error",   "message", "操作失败，请重试")); }
    public void confirm_show()  { wire().component("confirm", Map.of(
            "title", "确认删除",
            "message", "删除后数据将无法恢复，确定要继续吗？",
            "confirmText", "删除",
            "cancelText", "取消"
    )); }
    public void confirm_delete() { wire().component("toast", Map.of("type", "success", "message", "已确认删除")); }
}
