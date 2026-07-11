package com.weacsoft.jaravel.app.http.controller;

import com.weacsoft.jaravel.app.model.User;
import com.weacsoft.jaravel.app.service.UserService;
import com.weacsoft.jaravel.vendor.http.controller.Controllers;
import com.weacsoft.jaravel.vendor.http.controller.request.Request;
import com.weacsoft.jaravel.vendor.http.controller.response.Response;
import com.weacsoft.jaravel.vendor.http.controller.response.ResponseBuilder;
import org.springframework.stereotype.Controller;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 用户控制器，对齐 Laravel 的 {@code UserController}。
 * <p>
 * 提供用户列表和详情查询接口。
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
}
