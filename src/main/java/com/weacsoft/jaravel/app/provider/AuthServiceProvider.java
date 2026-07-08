package com.weacsoft.jaravel.app.provider;

import com.weacsoft.jaravel.app.model.User;
import com.weacsoft.jaravel.app.model.admin.Admin;
import com.weacsoft.jaravel.vendor.auth.AuthManager;
import com.weacsoft.jaravel.vendor.core.Facade;
import com.weacsoft.jaravel.vendor.core.provider.ServiceProvider;
import com.weacsoft.jaravel.vendor.database.EloquentUserProvider;
import org.springframework.stereotype.Component;

/**
 * 认证服务提供者，对齐 Laravel 的 {@code AuthServiceProvider}。
 * <p>
 * 在 register 阶段注册用户提供者（provider）与守卫（guard），由
 * {@code ProviderRegistry} 在所有 Bean 就绪后自动调用。
 *
 * <h3>多 Provider / 多 Guard 模式</h3>
 * <p>
 * 对齐 Laravel 的 {@code config/auth.php}：可注册多个 provider（对应不同用户模型）
 * 与多个 guard（对应不同认证驱动 / 场景）。一个 guard 绑定一个 provider。
 * <pre>
 * // providers
 * 'users' => EloquentUserProvider(User.class, 'number')   // 用户表，凭证字段 number
 * 'admins' => EloquentUserProvider(Admin.class, 'username') // 管理员表（如有 Admin 模型）
 *
 * // guards
 * 'api' => driver=jwt,    provider=users   // API 场景：JWT 驱动
 * 'web' => driver=session, provider=users   // Web 场景：Session 驱动
 * 'admin' => driver=jwt,  provider=admins  // 管理后台：JWT 驱动 + admins provider
 * </pre>
 * <p>
 * <b>认证流程</b>（密码校验在应用层，不在 provider 中）：
 * <ol>
 *   <li>应用层按凭证查出用户：{@code User user = User.findByNumber(number);}</li>
 *   <li>应用层校验密码：{@code if (!password.equals(user.getPassword())) throw ...;}</li>
 *   <li>登入指定 guard：{@code Auth.guard("api").login(user);} 或 {@code Auth.login(user, "api");}</li>
 *   <li>检查登录态：{@code Auth.guard("api").check();}（以主键比对，不涉及密码）</li>
 * </ol>
 */
@Component
public class AuthServiceProvider extends ServiceProvider {

    @Override
    public void register() {
        AuthManager authManager = Facade.resolve(AuthManager.class);
        User userModel = Facade.resolve(User.class);

        // ---- 注册用户提供者（provider）----
        // User provider：用户表，凭证字段 number（工号）
        authManager.registerProvider("users",
                new EloquentUserProvider<>(userModel, "number"));

        // Admin provider：管理员表，凭证字段 username
        Admin adminModel = Facade.resolve(Admin.class);
        authManager.registerProvider("admins",
                new EloquentUserProvider<>(adminModel, "username"));

        // ---- 注册守卫（guard）----
        // api 守卫：JWT 驱动，绑定 users provider（用户场景，无状态）
        authManager.registerGuard("api", "jwt", "users");
        // admin 守卫：JWT 驱动，绑定 admins provider（管理员场景，无状态）
        authManager.registerGuard("admin", "jwt", "admins");

        // ---- 设默认守卫 ----
        authManager.setDefaultGuard("api");
    }
}
