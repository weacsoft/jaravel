package com.weacsoft.jaravel.app.http.controller;

import com.weacsoft.jaravel.app.model.User;
import com.weacsoft.jaravel.app.model.admin.Admin;
import com.weacsoft.jaravel.app.service.CaptchaService;
import com.weacsoft.jaravel.app.service.UserService;
import com.weacsoft.jaravel.vendor.auth.contract.AuthGuard;
import com.weacsoft.jaravel.vendor.auth.facade.Auth;
import com.weacsoft.jaravel.vendor.controller.Controllers;
import com.weacsoft.jaravel.vendor.http.request.Request;
import com.weacsoft.jaravel.vendor.http.response.Response;
import com.weacsoft.jaravel.vendor.http.response.ResponseBuilder;
import com.weacsoft.jaravel.vendor.jwt.JwtGuard;
import org.springframework.stereotype.Controller;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 认证控制器，对齐 Laravel 的 {@code AuthController}。
 * <p>
 * 提供双 Guard 认证：admin guard（管理员）和 api guard（用户），均为 JWT 驱动。
 * 密码校验在应用层完成（Service / Controller），不在 provider / guard 中。
 */
@Controller
public class AuthController implements Controllers {

    @Autowired
    private CaptchaService captchaService;

    // ===== 管理员认证 =====

    /**
     * 管理员登录（用户名 + 密码 + 验证码），登入 admin guard，返回 JWT token。
     */
    public Response adminLogin(Request request) {
        String username = request.input("username");
        String password = request.input("password");
        String captchaType = request.input("captchaType", "rotate");
        String captchaKey = request.input("captchaKey");
        String captchaInput = request.input("captchaInput");

        // 验证码校验（无状态：直接验证 captchaKey + 用户输入）
        if (!captchaService.verify(captchaType, captchaKey, captchaInput)) {
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
     */
    public Response userLogin(Request request) {
        String number = request.input("number");
        String password = request.input("password");
        String captchaType = request.input("captchaType", "rotate");
        String captchaKey = request.input("captchaKey");
        String captchaInput = request.input("captchaInput");

        // 验证码校验（无状态：直接验证 captchaKey + 用户输入）
        if (!captchaService.verify(captchaType, captchaKey, captchaInput)) {
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
