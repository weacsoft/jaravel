package com.weacsoft.jaravel.app.http.controller;

import com.weacsoft.jaravel.app.model.Product;
import com.weacsoft.jaravel.app.model.User;
import com.weacsoft.jaravel.app.service.UserService;
import com.weacsoft.jaravel.vendor.controller.Controllers;
import com.weacsoft.jaravel.vendor.http.request.Request;
import com.weacsoft.jaravel.vendor.http.response.Response;
import com.weacsoft.jaravel.vendor.http.response.ResponseBuilder;
import org.springframework.stereotype.Controller;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 用户控制器，对齐 Laravel 的 {@code UserController}。
 * <p>
 * 演示路径参数、Service 静态调用与 Response 构建。
 * 同时包含多数据库测试方法（products / product），通过 {@link Product} 模型
 * 访问第二数据源（secondaryDataSource）。
 */
@Controller
public class UserController implements Controllers {

    public Response list(Request request) {
        List<User> users = UserService.list();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("list", users);
        result.put("total", users.size());
        return ResponseBuilder.json(result);
    }

    public Response show(Request request) {
        Long id = request.routeParam("id", Long.class);
        User user = UserService.findById(id);
        if (user == null) {
            return ResponseBuilder.error(404, "用户不存在");
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("user", user);
        return ResponseBuilder.json(result);
    }

    // ---- 多数据库测试方法（Product 使用第二数据源） ----

    /**
     * 查询全部产品（多数据库测试），对齐 Laravel Product::all()。
     * <p>
     * {@link Product} 通过 {@code @DataSource("secondaryDataSource")} 注解
     * 自动使用第二数据源执行查询。
     */
    public Response products(Request request) {
        List<Product> products = Product.all();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("list", products);
        result.put("total", products.size());
        return ResponseBuilder.json(result);
    }

    /**
     * 按主键查询产品（多数据库测试），对齐 Laravel Product::find()。
     */
    public Response product(Request request) {
        Long id = request.routeParam("id", Long.class);
        Product product = Product.find(id);
        if (product == null) {
            return ResponseBuilder.error(404, "Product not found");
        }
        return ResponseBuilder.json(Map.of("product", product));
    }
}
