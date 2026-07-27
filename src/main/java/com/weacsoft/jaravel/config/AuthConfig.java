package com.weacsoft.jaravel.config;

import com.weacsoft.jaravel.app.model.User;
import com.weacsoft.jaravel.app.model.admin.Admin;
import com.weacsoft.jaravel.vendor.auth.AuthManager;
import com.weacsoft.jaravel.vendor.core.Facade;
import com.weacsoft.jaravel.vendor.database.EloquentUserProvider;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

/**
 * 认证配置，对齐 Laravel 的 {@code config/auth.php}。
 * <p>
 * 在所有单例 Bean 就绪后，注册用户提供者（provider）与守卫（guard）到 {@link AuthManager}。
 * <p>
 * <h3>认证架构</h3>
 * <ul>
 *   <li><b>认证驱动</b>（driver）：{@code session}（登录态存储）| {@code jwt}（无状态 token）</li>
 *   <li><b>Session 存储</b>：全局配置，不与 Guard 绑定。由 {@link SessionConfig} 决定具体实现</li>
 * </ul>
 *
 * <h3>多 Provider / 多 Guard 模式</h3>
 * <pre>
 * // providers
 * 'users'  => EloquentUserProvider(User.class, 'number')    // 用户表，凭证字段 number
 * 'admins' => EloquentUserProvider(Admin.class, 'username')  // 管理员表
 *
 * // guards
 * 'web'   => driver=session, provider=users    // Web 场景：Session 驱动
 * 'api'   => driver=jwt,    provider=users     // API 场景：JWT 驱动
 * 'admin' => driver=jwt,    provider=admins    // 管理后台：JWT 驱动 + admins provider
 * </pre>
 *
 * <h3>认证流程</h3>（密码校验在应用层，不在 provider 中）：
 * <ol>
 *   <li>应用层按凭证查出用户：{@code User user = User.findByNumber(number);}</li>
 *   <li>应用层校验密码：{@code if (!password.equals(user.getPassword())) throw ...;}</li>
 *   <li>登入指定 guard：{@code Auth.guard("api").login(user);} 或 {@code Auth.login(user, "api");}</li>
 *   <li>检查登录态：{@code Auth.guard("api").check();}（以主键比对，不涉及密码）</li>
 * </ol>
 *
 * <h3>Session 存储切换</h3>
 * Session 存储不在此处配置，而在 {@link SessionConfig} 中全局决定。
 * 默认使用 CookieSessionStore（Servlet HttpSession），如需 Redis 见 {@link SessionConfig}。
 */
@Configuration
public class AuthConfig implements SmartInitializingSingleton {

    @Autowired
    private AuthManager authManager;

    @Override
    public void afterSingletonsInstantiated() {
        // ---- 注册用户提供者（provider）----
        User userModel = Facade.resolve(User.class);
        Admin adminModel = Facade.resolve(Admin.class);

        // User provider：用户表，凭证字段 number（工号）
        authManager.registerProvider("users",
                new EloquentUserProvider<>(userModel, "number"));

        // Admin provider：管理员表，凭证字段 username
        authManager.registerProvider("admins",
                new EloquentUserProvider<>(adminModel, "username"));

        // ---- 注册守卫（guard）----
        // web 守卫：Session 驱动，绑定 users provider
        // Session 存储由 SessionConfig 全局决定（默认 CookieSessionStore）
        authManager.registerGuard("web", "session", "users");
        // api 守卫：JWT 驱动，绑定 users provider（用户场景，无状态）
        authManager.registerGuard("api", "jwt", "users");
        // admin 守卫：JWT 驱动，绑定 admins provider（管理员场景，无状态）
        authManager.registerGuard("admin", "jwt", "admins");

        // ---- 设默认守卫 ----
        authManager.setDefaultGuard("api");
    }
}
