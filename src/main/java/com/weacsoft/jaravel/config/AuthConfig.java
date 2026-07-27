package com.weacsoft.jaravel.config;

import com.weacsoft.jaravel.app.model.User;
import com.weacsoft.jaravel.app.model.admin.Admin;
import com.weacsoft.jaravel.vendor.auth.contract.GuardDefinition;
import com.weacsoft.jaravel.vendor.auth.contract.UserProvider;
import com.weacsoft.jaravel.vendor.database.EloquentUserProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 认证配置（对齐 Laravel 的 {@code config/auth.php}）。
 * <p>
 * 纯 {@code @Bean} 声明式配置，无构造方法副作用：
 * <ul>
 *   <li>{@code @Bean("users")} / {@code @Bean("admins")} — 声明 UserProvider，bean name 即 provider name</li>
 *   <li>{@code @Bean("web")} / {@code @Bean("api")} / {@code @Bean("admin")} — 声明 GuardDefinition，bean name 即 guard name</li>
 * </ul>
 * {@code AuthAutoConfiguration} 通过 {@code Map<String, UserProvider>} 和 {@code Map<String, GuardDefinition>}
 * 自动收集并注册到 {@link com.weacsoft.jaravel.vendor.auth.AuthManager}。
 *
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
 * <h3>类型推断</h3>
 * 由于 provider 在创建时已绑定具体 Model 类型，{@code Auth.guard("web").user()} 可通过
 * 泛型方法 + 目标类型推断直接返回具体用户类型，无需强转：
 * <pre>
 * User user = Auth.guard("web").user();      // 编译器推断 T = User
 * Admin admin = Auth.guard("admin").user();  // 编译器推断 T = Admin
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
 *
 * <h3>替代方案：配置式注册</h3>
 * 也可不写此类，改用 {@code application.yml} 配置式注册（需引入 database 模块以获得 eloquent 驱动）：
 * <pre>
 * jaravel:
 *   auth:
 *     default-guard: api
 *     providers:
 *       users:
 *         driver: eloquent
 *         model: com.weacsoft.jaravel.app.model.User
 *         credential-field: number
 *       admins:
 *         driver: eloquent
 *         model: com.weacsoft.jaravel.app.model.admin.Admin
 *         credential-field: username
 *     guards:
 *       web:   { driver: session, provider: users }
 *       api:   { driver: jwt,     provider: users }
 *       admin: { driver: jwt,     provider: admins }
 * </pre>
 */
@Configuration
public class AuthConfig {

    // ---- 注册用户提供者（provider）----
    // @Bean("users") 的 bean name 即 provider name
    // AuthAutoConfiguration 通过 Map<String, UserProvider> 自动收集

    /**
     * User provider：用户表，凭证字段 number（工号）。
     */
    @Bean("users")
    public UserProvider usersProvider(User userModel) {
        return new EloquentUserProvider<>(userModel, "number");
    }

    /**
     * Admin provider：管理员表，凭证字段 username。
     */
    @Bean("admins")
    public UserProvider adminsProvider(Admin adminModel) {
        return new EloquentUserProvider<>(adminModel, "username");
    }

    // ---- 注册守卫（guard）----
    // @Bean("web") 的 bean name 即 guard name
    // AuthAutoConfiguration 通过 Map<String, GuardDefinition> 自动收集

    /**
     * web 守卫：Session 驱动，绑定 users provider。
     * Session 存储由 SessionConfig 全局决定（默认 CookieSessionStore）。
     */
    @Bean("web")
    public GuardDefinition webGuard() {
        return GuardDefinition.of("session", "users");
    }

    /**
     * api 守卫：JWT 驱动，绑定 users provider（用户场景，无状态）。
     */
    @Bean("api")
    public GuardDefinition apiGuard() {
        return GuardDefinition.of("jwt", "users");
    }

    /**
     * admin 守卫：JWT 驱动，绑定 admins provider（管理员场景，无状态）。
     */
    @Bean("admin")
    public GuardDefinition adminGuard() {
        return GuardDefinition.of("jwt", "admins");
    }
}
