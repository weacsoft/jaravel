package com.weacsoft.jaravel.app.http.controller;

import com.weacsoft.jaravel.app.model.User;
import com.weacsoft.jaravel.app.service.UserService;
import com.weacsoft.jaravel.vendor.auth.contract.AuthGuard;
import com.weacsoft.jaravel.vendor.auth.facade.Auth;
import com.weacsoft.jaravel.vendor.controller.Controllers;
import com.weacsoft.jaravel.vendor.http.request.Request;
import com.weacsoft.jaravel.vendor.http.response.Response;
import com.weacsoft.jaravel.vendor.http.response.ResponseBuilder;
import com.weacsoft.jaravel.vendor.jwt.JwtGuard;
import org.springframework.stereotype.Controller;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 认证控制器，对齐 Laravel 的 {@code AuthController}。
 * <p>
 * 演示全链路：Request 取参 → Service 查询用户 + 校验密码（应用层）→ Auth 门面登录 → JWT token 返回。
 * <p>
 * <b>认证流程</b>（密码校验在应用层 Service，不在 provider / guard 中）：
 * <ol>
 *   <li>查询用户：{@code UserService.login(number, password)} 内部按 number 查出用户并校验密码；</li>
 *   <li>登入：{@code Auth.login(user)} 或 {@code Auth.guard("api").login(user)} 登入指定 guard；</li>
 *   <li>检查：{@code Auth.check()} 或 {@code Auth.guard("api").check()} 以主键校验登录态。</li>
 * </ol>
 * <p>
 * <b>多 Guard 演示</b>：api guard（JWT 驱动，无状态）与 web guard（Session 驱动，有状态）共存，
 * 可分别登录 / 检查 / 登出。
 */
@Controller
public class AuthController implements Controllers {

    /**
     * 用户注册：创建用户后通过默认 guard（api=JWT）登录。
     */
    public Response register(Request request) {
        String name = request.input("name");
        String number = request.input("number");
        String password = request.input("password");
        String email = request.input("email");
        User user = UserService.register(name, number, password, email);
        // 登入默认 guard（api=JWT）
        Auth.login(user);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("user", user);
        result.put("token", Auth.token());
        result.put("refresh_token", getRefreshToken());
        result.put("message", "注册成功");
        return ResponseBuilder.json(result);
    }

    /**
     * 用户登录（默认 api guard = JWT）。
     * <p>
     * 流程：Service 查询用户 + 校验密码 → Auth.login 登入 → 返回 JWT token + refresh token。
     */
    public Response login(Request request) {
        String number = request.input("number");
        String password = request.input("password");
        // Service 层负责查询用户 + 校验密码（应用层责任，不在 provider 中）
        User user = UserService.login(number, password);
        // 登入默认 guard（api=JWT），Auth 以主键比对，不涉及密码
        Auth.login(user);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("token", Auth.token());
        result.put("refresh_token", getRefreshToken());
        result.put("user", user);
        return ResponseBuilder.json(result);
    }

    /**
     * 通过指定 guard 登录，演示多 guard 模式。
     * <p>
     * 请求参数 {@code guard} 指定守卫名称（api=JWT / web=Session）：
     * <ul>
     *   <li>{@code guard=api}：通过 JWT 驱动登录，返回 JWT token；</li>
     *   <li>{@code guard=web}：通过 Session 驱动登录，登录态写入 HTTP Session。</li>
     * </ul>
     */
    public Response loginViaGuard(Request request) {
        String number = request.input("number");
        String password = request.input("password");
        String guardName = request.input("guard", "api");
        User user = UserService.login(number, password);
        // 登入指定 guard
        Auth.login(user, guardName);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("guard", guardName);
        result.put("user", user);
        // JWT guard 返回 token；Session guard 无 token
        String token = Auth.token(guardName);
        if (token != null) {
            result.put("token", token);
        }
        result.put("message", "通过 " + guardName + " guard 登录成功");
        return ResponseBuilder.json(result);
    }

    /**
     * 登出（默认 guard）。JWT guard 会将当前 token 加入黑名单。
     */
    public Response logout(Request request) {
        Auth.logout();
        return ResponseBuilder.json(Map.of("message", "已退出登录"));
    }

    /**
     * 登出指定 guard，演示多 guard 登出。
     * <p>
     * 路径参数 {@code guard} 指定守卫名称（api / web）。
     */
    public Response logoutViaGuard(Request request) {
        String guardName = request.routeParam("guard");
        if (guardName == null || guardName.isEmpty()) {
            guardName = request.input("guard", "api");
        }
        Auth.logout(guardName);
        return ResponseBuilder.json(Map.of(
                "guard", guardName,
                "message", "已退出 " + guardName + " guard 登录"
        ));
    }

    /**
     * 获取当前登录用户信息（默认 guard）。
     */
    public Response me(Request request) {
        User user = (User) Auth.user();
        if (user == null) {
            return ResponseBuilder.error(401, "Unauthorized");
        }
        return ResponseBuilder.json(buildUserInfo(user));
    }

    /**
     * 检查指定 guard 的登录态，演示多 guard 检查。
     * <p>
     * 路径参数 {@code guard} 指定守卫名称（api / web），返回该 guard 的登录状态与用户信息。
     */
    public Response checkGuard(Request request) {
        String guardName = request.routeParam("guard");
        if (guardName == null || guardName.isEmpty()) {
            guardName = request.input("guard", "api");
        }
        AuthGuard guard = Auth.guard(guardName);
        boolean loggedIn = guard.check();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("guard", guardName);
        result.put("authenticated", loggedIn);
        if (loggedIn) {
            Object u = guard.user();
            if (u instanceof User user) {
                result.put("user", buildUserInfo(user));
            } else {
                result.put("user", u);
            }
        }
        return ResponseBuilder.json(result);
    }

    /**
     * JWT token 刷新：用 refresh token 换取新的 access token。
     * <p>
     * 请求参数 {@code refresh_token} 为登录时签发的 refresh token。
     * 校验通过后返回新的 access token，原 refresh token 仍可使用直到自然过期。
     * <p>
     * 此外，若 {@code refresh-enabled=true}（默认），每次携带 access token 请求时，
     * 若 token 已过半 TTL 会自动续期，新 token 通过响应中的 {@code refreshed_token} 字段返回。
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

    /**
     * 获取当前用户信息，并返回自动续期的新 token（如有）。
     * <p>
     * 当 {@code refresh-enabled=true} 且 access token 已过半 TTL 时，
     * JwtGuard.user() 会自动签发新 token，此处通过 {@code refreshed_token} 返回给客户端。
     */
    public Response profile(Request request) {
        AuthGuard guard = Auth.guard("api");
        User user = (User) guard.user();
        if (user == null) {
            return ResponseBuilder.error(401, "Unauthorized");
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("user", buildUserInfo(user));
        // 自动续期的新 token（如有）
        String refreshedToken = guard.token();
        if (refreshedToken != null) {
            result.put("refreshed_token", refreshedToken);
            result.put("message", "token 已自动续期，请使用新 token");
        }
        return ResponseBuilder.json(result);
    }

    /** 构建用户信息响应体 */
    private Map<String, Object> buildUserInfo(User user) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", user.getId());
        result.put("name", user.getName());
        result.put("number", user.getNumber());
        result.put("email", user.getEmail());
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
