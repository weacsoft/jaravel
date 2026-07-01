package com.weacsoft.jaravel.app.http.middleware;

import com.weacsoft.jaravel.vendor.http.request.Request;
import com.weacsoft.jaravel.vendor.http.response.Response;
import com.weacsoft.jaravel.vendor.middleware.Middleware;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * 中间件顺序测试中间件，用于验证洋葱模型中间件链的执行顺序。
 * <p>
 * 每个实例携带一个名称（如 "A"、"B"、"C"），在 {@link #handle(Request, NextFunction)} 中：
 * <ol>
 *   <li>记录 before 日志，并将 {@code name-before} 追加到请求属性 {@code _middleware_order}</li>
 *   <li>调用 {@code next.apply(request)} 进入下一层中间件</li>
 *   <li>记录 after 日志，并将 {@code name-after} 追加到请求属性 {@code _middleware_order}</li>
 * </ol>
 * <p>
 * 通过 {@code GET /api/middleware-test} 路由注册三个实例（A → B → C），
 * 验证中间件按洋葱模型执行：A-before → B-before → C-before → Handler → C-after → B-after → A-after。
 * <p>
 * 使用 {@link Request#setAttribute(String, Object)} 和 {@link Request#getAttribute(String, Class)}
 * 在中间件间传递数据，对齐 Laravel 的 {@code $request->attributes}。
 */
public class OrderTestMiddleware implements Middleware {

    private static final Logger log = LoggerFactory.getLogger(OrderTestMiddleware.class);

    private final String name;

    public OrderTestMiddleware(String name) {
        this.name = name;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Response handle(Request request, NextFunction next) {
        log.info("[中间件 {}] before", name);

        // 从请求属性获取执行顺序列表
        List<String> order = request.getAttribute("_middleware_order", List.class);
        if (order == null) {
            order = new ArrayList<>();
        }
        order.add(name + "-before");
        request.setAttribute("_middleware_order", order);

        Response response = next.apply(request);

        // after 阶段也记录顺序
        List<String> orderAfter = request.getAttribute("_middleware_order", List.class);
        if (orderAfter == null) {
            orderAfter = new ArrayList<>();
        }
        orderAfter.add(name + "-after");
        request.setAttribute("_middleware_order", orderAfter);

        log.info("[中间件 {}] after", name);
        return response;
    }
}
