package com.weacsoft.jaravel.app.http.controller;

import com.weacsoft.jaravel.app.model.User;
import com.weacsoft.jaravel.app.model.admin.Admin;
import com.weacsoft.jaravel.app.service.CaptchaService;
import com.weacsoft.jaravel.app.service.UserService;
import com.weacsoft.jaravel.vendor.auth.contract.AuthGuard;
import com.weacsoft.jaravel.vendor.auth.facade.Auth;
import com.weacsoft.jaravel.vendor.http.controller.Controllers;
import com.weacsoft.jaravel.vendor.http.controller.request.Request;
import com.weacsoft.jaravel.vendor.http.controller.response.Response;
import com.weacsoft.jaravel.vendor.http.controller.response.ResponseBuilder;
import com.weacsoft.jaravel.vendor.jwt.JwtGuard;
import org.springframework.stereotype.Controller;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 认证控制器，对齐 Laravel 的 {@code AuthController}。
 * <p>
 * 提供三种 Guard 认证，展示认证驱动与 Session 存储分离的架构：
 * <ul>
 *   <li><b>web</b>：Session 驱动 + cookie 存储，适合传统 Web 页面（有状态，登录态存于 HttpSession）</li>
 *   <li><b>api</b>：JWT 驱动，适合 API / SPA 场景（无状态，返回 token）</li>
 *   <li><b>admin</b>：JWT 驱动，管理员场景（无状态，返回 token）</li>
 * </ul>
 * 密码校验在应用层完成（Service / Controller），不在 provider / guard 中。
 */
@Controller
public class AuthController implements Controllers {

    @Autowired
    private CaptchaService captchaService;

    // ===== Session 认证（web guard，cookie 存储）=====

    /**
     * 用户 Session 登录（工号 + 密码），登入 web guard。
     * <p>
     * 与 JWT 登录不同，Session 登录不返回 token，登录态由 Servlet HttpSession 维护，
     * 浏览器通过 JSESSIONID cookie 自动携带。适合传统 Web 页面场景。
     */
    public Response sessionLogin(Request request) {
        String number = request.input("number");
        String password = request.input("password");

        User user = UserService.login(number, password);
        Auth.guard("web").login(user);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("user", buildUserInfo(user));
        result.put("message", "Session 登录成功");
        return ResponseBuilder.json(result);
    }

    /**
     * Session 登出（web guard），清除 HttpSession 中的登录态。
     */
    public Response sessionLogout(Request request) {
        Auth.logout("web");
        return ResponseBuilder.json(Map.of("message", "已退出 Session 登录"));
    }

    /**
     * 获取当前 Session 登录用户（web guard）。
     */
    public Response sessionMe(Request request) {
        User user = (User) Auth.guard("web").user();
        if (user == null) {
            return ResponseBuilder.error(401, "Unauthorized");
        }
        return ResponseBuilder.json(buildUserInfo(user));
    }

    // ===== 管理员认证（admin guard，JWT 驱动）=====

    /**
     * 管理员登录（用户名 + 密码 + 验证码），登入 admin guard，返回 JWT token。
     * <p>
     * 验证码与账号密码在<b>同一次请求</b>里提交：{@code captchaKey} 是生成接口
     * 下发的合并凭证（{@code type.captchaKey}），配合 {@code captchaInput}
     * 两个参数即可完成校验，无需前端先单独调一次验证码校验接口。
     */
    public Response adminLogin(Request request) {
        String username = request.input("username");
        String password = request.input("password");
        String captchaKey = request.input("captchaKey");
        String captchaInput = request.input("captchaInput");

        // 验证码校验（无状态：合并凭证 + 用户输入两个参数）
        if (!captchaService.verify(captchaKey, captchaInput)) {
            return ResponseBuilder.error(403, "验证码校验失败或已过期，请重新完成验证");
        }

        Admin admin = Admin.findByUsername(username);
        if (admin == null || !password.equals(admin.getPassword())) {
            return ResponseBuilder.error(401, "用户名或密码错误");
        }
        if (admin.getStatus() == null || admin.getStatus() != 1) {
            return ResponseBuilder.error(403, "管理员账号已禁用");
        }

        Auth.guard("admin").login(admin);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("token", Auth.guard("admin").token());
        result.put("admin", buildAdminInfo(admin));
        result.put("message", "管理员登录成功");
        return ResponseBuilder.json(result);
    }

    /**
     * 管理员登出（admin guard），JWT token 加入黑名单。
     */
    public Response adminLogout(Request request) {
        Auth.logout("admin");
        return ResponseBuilder.json(Map.of("message", "管理员已退出登录"));
    }

    /**
     * 获取当前管理员信息（admin guard）。
     */
    public Response adminMe(Request request) {
        Admin admin = (Admin) Auth.guard("admin").user();
        if (admin == null) {
            return ResponseBuilder.error(401, "Unauthorized");
        }
        return ResponseBuilder.json(buildAdminInfo(admin));
    }

    // ===== 用户认证 =====

    /**
     * 用户注册：创建用户后通过默认 guard（api=JWT）登录。
     */
    public Response register(Request request) {
        String name = request.input("name");
        String number = request.input("number");
        String password = request.input("password");
        String email = request.input("email");
        User user = UserService.register(name, number, password, email);
        Auth.login(user);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("user", user);
        result.put("token", Auth.token());
        result.put("refresh_token", getRefreshToken());
        result.put("message", "注册成功");
        return ResponseBuilder.json(result);
    }

    /**
     * 用户登录（工号 + 密码），登入 api guard，返回 JWT token。
     * <p>
     * 验证码与账号密码在<b>同一次请求</b>里提交：{@code captchaKey} 是生成接口
     * 下发的合并凭证（{@code type.captchaKey}），配合 {@code captchaInput}
     * 两个参数即可完成校验。
     */
    public Response userLogin(Request request) {
        String number = request.input("number");
        String password = request.input("password");
        String captchaKey = request.input("captchaKey");
        String captchaInput = request.input("captchaInput");

        // 验证码校验（无状态：合并凭证 + 用户输入两个参数）
        if (!captchaService.verify(captchaKey, captchaInput)) {
            return ResponseBuilder.error(403, "验证码校验失败或已过期，请重新完成验证");
        }

        User user = UserService.login(number, password);
        Auth.login(user);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("token", Auth.token());
        result.put("refresh_token", getRefreshToken());
        result.put("user", user);
        result.put("message", "登录成功");
        return ResponseBuilder.json(result);
    }

    /**
     * 用户登出（api guard），JWT token 加入黑名单。
     */
    public Response logout(Request request) {
        Auth.logout();
        return ResponseBuilder.json(Map.of("message", "已退出登录"));
    }

    /**
     * 获取当前用户信息（api guard）。
     */
    public Response me(Request request) {
        User user = (User) Auth.user();
        if (user == null) {
            return ResponseBuilder.error(401, "Unauthorized");
        }
        return ResponseBuilder.json(buildUserInfo(user));
    }

    /**
     * JWT token 刷新：用 refresh token 换取新的 access token。
     */
    public Response refresh(Request request) {
        String refreshToken = request.input("refresh_token");
        if (refreshToken == null || refreshToken.isEmpty()) {
            return ResponseBuilder.error(400, "缺少 refresh_token 参数");
        }
        AuthGuard guard = Auth.guard("api");
        if (!(guard instanceof JwtGuard jwtGuard)) {
            return ResponseBuilder.error(500, "默认 guard 不是 JWT 驱动");
        }
        String accessToken = jwtGuard.refresh(refreshToken);
        if (accessToken == null) {
            return ResponseBuilder.error(401, "refresh_token 无效或已过期");
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("token", accessToken);
        result.put("message", "token 刷新成功");
        return ResponseBuilder.json(result);
    }

    // ===== 私有工具方法 =====

    /** 构建用户信息响应体 */
    private Map<String, Object> buildUserInfo(User user) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", user.getId());
        result.put("name", user.getName());
        result.put("number", user.getNumber());
        result.put("email", user.getEmail());
        return result;
    }

    /** 构建管理员信息响应体 */
    private Map<String, Object> buildAdminInfo(Admin admin) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", admin.getId());
        result.put("username", admin.getUsername());
        result.put("nickname", admin.getNickname());
        result.put("status", admin.getStatus());
        return result;
    }

    /** 获取当前 api guard 的 refresh token */
    private String getRefreshToken() {
        AuthGuard guard = Auth.guard("api");
        if (guard instanceof JwtGuard jwtGuard) {
            return jwtGuard.refreshToken();
        }
        return null;
    }
}
