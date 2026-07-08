package com.weacsoft.jaravel.routes;

import com.weacsoft.jaravel.vendor.http.response.ResponseBuilder;
import com.weacsoft.jaravel.vendor.route.Router;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

/**
 * Web 路由定义，对齐 Laravel 的 {@code routes/web.php}。
 * <p>
 * 首页重定向到登录页，静态资源由 SpringBoot 默认静态资源服务处理。
 */
@Component
public class Web {

    public void register(Router router, ApplicationContext context) {
        // 首页重定向到 index.html
        router.get("/", request -> ResponseBuilder.redirect("/index.html"));
    }
}
