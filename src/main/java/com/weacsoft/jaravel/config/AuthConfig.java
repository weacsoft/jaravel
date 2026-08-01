package com.weacsoft.jaravel.config;

import com.weacsoft.jaravel.app.model.User;
import com.weacsoft.jaravel.app.model.admin.Admin;
import com.weacsoft.jaravel.vendor.auth.RegisterGuard;
import com.weacsoft.jaravel.vendor.auth.RegisterProvider;
import com.weacsoft.jaravel.vendor.auth.contract.GuardDefinition;
import com.weacsoft.jaravel.vendor.auth.contract.UserProvider;
import com.weacsoft.jaravel.vendor.database.EloquentUserProvider;
import org.springframework.context.annotation.Configuration;

/**
 * 认证配置（对齐 Laravel 的 {@code config/auth.php}）。
 *
 * <h2>一、三层注册优先级：声明 → 配置 → 默认</h2>
 * <table border="1">
 *   <caption>guard / provider 的三层来源</caption>
 *   <tr><th>层级</th><th>写法</th><th>说明</th></tr>
 *   <tr>
 *     <td>1. 声明（最高）</td>
 *     <td>{@code @RegisterGuard} / {@code @RegisterProvider}</td>
 *     <td>本类中的注解方法。同名时<b>覆盖</b>配置式；不注册为 Spring Bean，
 *         因此别名可自由取名，不会触发 BeanDefinitionOverrideException</td>
 *   </tr>
 *   <tr>
 *     <td>2. 配置</td>
 *     <td>{@code application.yml} 的 {@code jaravel.auth.guards / providers}</td>
 *     <td>无需写 Java 代码，适合纯配置驱动的项目</td>
 *   </tr>
 *   <tr>
 *     <td>3. 默认（兜底）</td>
 *     <td>—</td>
 *     <td>写了 guard 但没写 {@code driver} → 自动兜底为 {@code session}</td>
 *   </tr>
 * </table>
 *
 * <h2>二、"安装 ≠ 启用"：驱动按需装配</h2>
 * auth 模块<b>自身始终启用</b>（否则 {@code Auth} 静态门面会 NPE），但<b>具体驱动</b>按需装配：
 * <ul>
 *   <li>{@code SessionGuardDriver} — 条件 {@code OnSessionGuardDriverCondition}：
 *       有任意 guard 声明 {@code driver: session}，<b>或</b>一个 driver 都没写（兜底）时装配</li>
 *   <li>{@code JwtGuardDriver} — 条件 {@code OnJwtGuardDriverCondition}：
 *       <b>严格按需</b>，必须显式出现 {@code driver: jwt} 才装配。JWT 不参与兜底</li>
 * </ul>
 * 因此：只用 session 的项目，即使引入了 jwt 依赖，JWT 相关 Bean 也<b>不会进内存</b>。
 *
 * <h2>三、driver 与 provider 的区别</h2>
 * <ul>
 *   <li><b>driver</b>（{@code session} / {@code jwt}）：登录态的<b>承载方式</b>，
 *       由框架 support 声明，一种 driver 全局只有一个实现对象</li>
 *   <li><b>provider</b>（{@code users} / {@code admins}）：<b>不是驱动</b>。
 *       同一个 provider 类（如 {@code EloquentUserProvider}）可以有多个对象——
 *       因为绑定的 Model 类不同。所以是「Model 类 + provider 类」合起来按<b>名字</b>取用，
 *       这个名字就是 {@code @RegisterProvider("users")} 的 value</li>
 * </ul>
 *
 * <h2>四、本项目实际配置</h2>
 * <pre>
 * // providers
 * 'users'  =&gt; EloquentUserProvider(User.class,  'number')    // 用户表，凭证字段 number
 * 'admins' =&gt; EloquentUserProvider(Admin.class, 'username')  // 管理员表
 *
 * // guards
 * 'web'   =&gt; driver=session, provider=users    // Web 场景：Session 驱动
 * 'api'   =&gt; driver=jwt,     provider=users    // API 场景：JWT 驱动（默认守卫）
 * 'admin' =&gt; driver=jwt,     provider=admins   // 管理后台：JWT 驱动 + admins provider
 * </pre>
 *
 * <h2>五、类型推断</h2>
 * provider 创建时已绑定具体 Model 类型，{@code Auth.guard("web").user()} 可直接返回具体类型：
 * <pre>
 * User  user  = Auth.guard("web").user();    // 编译器推断 T = User
 * Admin admin = Auth.guard("admin").user();  // 编译器推断 T = Admin
 * </pre>
 *
 * <h2>六、认证流程</h2>（密码校验在应用层，不在 provider 中）
 * <ol>
 *   <li>应用层按凭证查出用户：{@code User user = User.findByNumber(number);}</li>
 *   <li>应用层校验密码：{@code if (!password.equals(user.getPassword())) throw ...;}</li>
 *   <li>登入指定 guard：{@code Auth.guard("api").login(user);}</li>
 *   <li>检查登录态：{@code Auth.guard("api").check();}（以主键比对，不涉及密码）</li>
 * </ol>
 *
 * <h2>七、Session 存储切换</h2>
 * Session <b>存储介质</b>不在此处配置，而在 {@link SessionConfig} 中全局决定
 * （默认 CookieSessionStore，可切 Redis）。driver=session 只表示"用会话承载登录态"。
 */
@Configuration
public class AuthConfig {

    // =====================================================================
    // 方式一（本项目采用）：注解声明式 —— 优先级最高
    // =====================================================================

    // ---- 1.1 注册用户提供者（provider）----
    // @RegisterProvider("users") 的 value 即 provider 名字（Model 类 + provider 类的别名）
    // AuthRegistrar 在所有 Bean 初始化完成后扫描注解并注册到 AuthManager

    /**
     * users provider：用户表，凭证字段 number（工号）。
     */
    @RegisterProvider("users")
    public UserProvider usersProvider(User userModel) {
        return new EloquentUserProvider<>(userModel, "number");
    }

    /**
     * admins provider：管理员表，凭证字段 username。
     * <p>
     * 注意这里和 users 用的是<b>同一个 provider 类</b>，只是绑定的 Model 不同，
     * 所以必须靠名字（别名）区分，而不能把 provider 类本身当作"驱动"。
     */
    @RegisterProvider("admins")
    public UserProvider adminsProvider(Admin adminModel) {
        return new EloquentUserProvider<>(adminModel, "username");
    }

    // ---- 1.2 注册守卫（guard）----
    // @RegisterGuard("web") 的 value 即 guard 名字
    // 注解声明的 guard / provider 不会注册为 Spring Bean，因此不会与同名 bean 冲突

    /**
     * web 守卫：session 驱动，绑定 users provider。
     * <p>
     * 因为这里出现了 {@code driver = session}，{@code SessionGuardDriver} 会被装配。
     */
    @RegisterGuard("web")
    public GuardDefinition webGuard() {
        return GuardDefinition.of("session", "users");
    }

    /**
     * api 守卫：jwt 驱动，绑定 users provider（无状态）。标记为默认守卫。
     * <p>
     * 因为这里出现了 {@code driver = jwt}，{@code JwtGuardDriver} 才会被装配；
     * 若把本方法注释掉且 admin 也不用 jwt，JWT 驱动就完全不进内存。
     */
    @RegisterGuard(value = "api", defaultGuard = true)
    public GuardDefinition apiGuard() {
        return GuardDefinition.of("jwt", "users");
    }

    /**
     * admin 守卫：jwt 驱动，绑定 admins provider（管理员场景，无状态）。
     */
    @RegisterGuard("admin")
    public GuardDefinition adminGuard() {
        return GuardDefinition.of("jwt", "admins");
    }

    // =====================================================================
    // 方式二：兜底默认 —— 写了 guard 但不写 driver
    // =====================================================================
    // 只声明 provider，不写 driver，框架自动兜底为 session：
    //
    // @RegisterGuard("simple")
    // public GuardDefinition simpleGuard() {
    //     // 等价于 GuardDefinition.of("session", "users")
    //     return GuardDefinition.provider("users");
    // }
    //
    // 各模块兜底默认值一览：
    //   auth     -> session      queue    -> sync/array
    //   cache    -> array        storage  -> local
    //   session  -> cookie       database -> 无兜底，未配置直接报错
    //   jwt      -> 不参与兜底，必须显式 driver: jwt

    // =====================================================================
    // 方式三：自定义扩展驱动 —— 不改核心代码
    // =====================================================================
    // 典型场景：auth 用 session，但发回浏览器的 cookie 内容想用 JWT 签名。
    // 自己实现 GuardDriver 并声明 support 的驱动名，即可在 guard 里直接用：
    //
    // @RegisterGuardDriver("jwt-cookie")
    // public GuardDriver jwtCookieDriver(JwtManager jwt, SessionStore store) {
    //     return new JwtCookieGuardDriver(jwt, store);
    // }
    //
    // @RegisterGuard("web")
    // public GuardDefinition webGuard() {
    //     return GuardDefinition.of("jwt-cookie", "users");
    // }

    // =====================================================================
    // 方式四：配置式 —— 完全不写本类，改用 application.yml
    // =====================================================================
    // 需引入 database 模块以获得 eloquent provider 驱动：
    //
    // jaravel:
    //   auth:
    //     default-guard: api
    //     providers:
    //       users:
    //         driver: eloquent
    //         model: com.weacsoft.jaravel.app.model.User
    //         credential-field: number
    //       admins:
    //         driver: eloquent
    //         model: com.weacsoft.jaravel.app.model.admin.Admin
    //         credential-field: username
    //     guards:
    //       web:   { driver: session, provider: users }
    //       api:   { driver: jwt,     provider: users }
    //       admin: { driver: jwt,     provider: admins }
    //       # 不写 driver 时兜底为 session：
    //       # simple: { provider: users }

    // =====================================================================
    // 方式五：编程式 —— 运行期动态注册
    // =====================================================================
    // AuthManager 暴露了运行期注册入口，适合插件/多租户场景：
    //
    // @Autowired
    // public void registerTenantGuards(AuthManager auth) {
    //     auth.registerProvider("tenants", new EloquentUserProvider<>(tenantModel, "code"));
    //     auth.registerGuard("tenant", GuardDefinition.of("jwt", "tenants"));
    // }
}
