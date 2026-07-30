package com.weacsoft.jaravel.app.http.middleware;

import com.weacsoft.jaravel.vendor.http.controller.request.Request;
import com.weacsoft.jaravel.vendor.http.middleware.VerifyCsrfToken;
import com.weacsoft.jaravel.vendor.springboot.annotation.MiddlewareAlias;

/**
 * 应用层 CSRF 校验中间件，对齐 Laravel {@code VerifyCsrfToken}。
 * <p>
 * 直接复用 vendor 提供的 {@link VerifyCsrfToken} 逻辑（token 生成、session 存储、
 * 请求头/XSRF-TOKEN cookie 校验、排除路由等），仅通过
 * {@code @MiddlewareAlias("VerifyCsrfToken")} 将其挂到 Web 路由组，
 * 使 POST/PUT/PATCH/DELETE 等“非安全”请求必须经过 CSRF token 校验，
 * GET/HEAD/OPTIONS 以及排除路由（如 api/、logout 等）自动放行。
 * <p>
 * token 存放于 servlet {@code HttpSession}（key={@code csrf_token}），
 * 与模板辅助函数 {@code csrf_field()}/{@code csrf_token()} 通过
 * {@link com.weacsoft.jaravel.vendor.jblade.BladeFunctions} 注册的 {@code csrf_token}
 * 函数读取的是同一个值，保证表单渲染出的隐藏域 value 与校验用的 token 一致、且非空。
 */
@MiddlewareAlias("VerifyCsrfToken")
public class AppVerifyCsrfToken extends VerifyCsrfToken {

    /**
     * 暴露 vendor {@link VerifyCsrfToken} 中受保护的 session key（{@code csrf_token}），
     * 供 {@code BladeEngineProvider} 注册 {@code csrf_token} 动态函数时复用，
     * 确保模板渲染读到的 token 与中间件校验的 token 使用同一 session 键。
     */
    public static String csrfSessionKey() {
        return CSRF_SESSION_KEY;
    }

    /**
     * 读取当前请求 session 中的 CSRF token，若不存在则当场生成并写回 session，
     * 与 {@link VerifyCsrfToken#handle} 的 token 同源。供模板 {@code csrf_token()} 函数调用，
     * 保证 {@code csrf_field()} 渲染出的隐藏域 value 非空。
     */
    public static String currentToken(Request request) {
        if (request == null) {
            return "";
        }
        return new AppVerifyCsrfToken().getSessionToken(request);
    }
}
