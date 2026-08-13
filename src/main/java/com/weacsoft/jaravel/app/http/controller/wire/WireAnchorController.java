package com.weacsoft.jaravel.app.http.controller.wire;

import com.weacsoft.jaravel.vendor.wire.WireController;
import com.weacsoft.jaravel.vendor.wire.WireView;

import java.util.Map;

/**
 * Wire 锚点改写演示 — 验证「HTML 注释非法位置」的 section 替换。
 * <p>
 * wire 的局部刷新依赖 {@code <!--wire:section-start:NAME-->} 注释锚点定位 DOM 片段。
 * 但以下位置的 HTML 注释不会被浏览器当作注释解析，而是退化为纯文本，污染内容：
 * <ul>
 *   <li>原始文本元素内部：{@code <title>} / {@code <textarea>} / {@code <script>} / {@code <style>}</li>
 *   <li>标签属性值内部：如 {@code <body class="...">} / {@code <meta content="...">}</li>
 * </ul>
 * 框架在渲染出口通过 {@code WireAnchorRewriter} 将这些位置的注释锚点改写为
 * {@code wire:section-text} / {@code wire:section-attr} 标记属性，
 * 并在透明导航的 diff 响应中以 {@code anchors} 字段下发真实值，
 * 由前端 {@code wire-navigate.js} 的 {@code applyAnchors} 精确回填。
 * <p>
 * 本页把 title / body class / meta description 三处都交给 section 驱动，
 * 用于人工与自动化回归验证。
 */
public class WireAnchorController extends WireController {

    @Override
    protected WireView render() {
        return wireView("wire/anchor-demo");
    }

    @Override
    protected String getUpdateRouteName() { return "wire.anchors"; }

    /** ping 按钮 action：仅测试 wire 请求是否正常 */
    public void ping() {
        wire().component("toast", Map.of(
                "type", "success",
                "message", "锚点页 wire 请求正常，section 标记未污染页面"
        ));
    }
}
