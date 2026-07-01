@extends('docs.layout')

@section('content')
<h1>认证</h1>
<p>Jaravel 提供多 Guard / 多 Provider 认证系统，支持 JWT 和 Session 两种 Guard 驱动。Auth 以主键比对登录态，密码校验在应用层完成。</p>

<h2>多 Guard / 多 Provider</h2>
<p>在 <code>AuthServiceProvider</code> 中注册 Provider 和 Guard：</p>
<pre><code>@Component
public class AuthServiceProvider extends ServiceProvider {
    @Override
    public void register() {
        AuthManager authManager = Facade.resolve(AuthManager.class);
        User userModel = Facade.resolve(User.class);

        // 注册用户提供者（provider）：按主键/凭证取出用户
        authManager.registerProvider("users",
            new EloquentUserProvider&lt;&gt;(userModel, "number"));

        // 注册守卫（guard）：一个 guard 绑定一个 provider
        authManager.registerGuard("api", "jwt", "users");      // JWT 驱动
        authManager.registerGuard("web", "session", "users");  // Session 驱动

        authManager.setDefaultGuard("api");
    }
}</code></pre>

<h2>认证流程</h2>
<pre><code>// 1. 应用层查询用户 + 校验密码（Service 层责任）
User user = UserService.login(number, password);

// 2. Auth 以主键登入指定 guard
Auth.login(user);                    // 登入默认 guard（api）
Auth.login(user, "web");             // 登入指定 guard

// 3. 检查登录态（以主键比对）
Auth.check();                        // 检查默认 guard
Auth.guard("api").check();           // 检查指定 guard

// 4. 获取当前用户
Auth.user();                         // 默认 guard 的当前用户
Auth.guard("api").user();            // 指定 guard 的当前用户

// 5. 登出
Auth.logout();                       // 登出默认 guard
Auth.logout("api");                  // 登出指定 guard</code></pre>

<h2>JWT 认证</h2>
<p>JWT 模块提供 token 自动续期与登出黑名单功能：</p>
<pre><code>// Token 自动续期（默认启用）
AuthGuard guard = Auth.guard("api");
User user = (User) guard.user();     // 自动续期发生在此处
String newToken = guard.token();     // 若已续期，返回新 token

// refresh token 换取新 access token
JwtGuard jwtGuard = (JwtGuard) Auth.guard("api");
String newAccessToken = jwtGuard.refresh(refreshToken);</code></pre>

<h2>JWT 配置</h2>
<pre><code>jaravel:
  jwt:
    secret: your-secret-key-must-be-32-bytes
    ttl: 3600000              # access token 有效期（毫秒，默认 1 小时）
    refresh-ttl: 604800000     # refresh token 有效期（毫秒，默认 7 天）
    refresh-enabled: true     # token 自动续期（默认启用）
    blacklist-store: array    # 登出黑名单缓存（array 内存 / file 文件）
    blacklist-prefix: "jwt:blacklist:"</code></pre>

<h2>Session 认证</h2>
<p>Session Guard 使用 Session 存储登录态，适用于 Web 场景（有状态）：</p>
<pre><code>// 登入 web guard（Session 驱动）
Auth.login(user, "web");

// 检查 web guard 登录态
Auth.guard("web").check();

// 登出 web guard
Auth.logout("web");</code></pre>

<h2>Authenticate 中间件</h2>
<pre><code>// 默认 Guard
router.get("/profile", handler).middleware(new Authenticate());

// 指定 Guard（对齐 Laravel auth:api）
router.get("/api/profile", handler).middleware(new Authenticate("api"));
router.get("/admin", handler).middleware(new Authenticate("web"));</code></pre>

<h2>Authenticatable 契约</h2>
<p>Model 实现 <code>Authenticatable</code> 接口，仅以主键标识用户：</p>
<pre><code>public class User extends BaseModel&lt;User, Long&gt; implements Authenticatable {
    @Override
    public Object getAuthIdentifier() { return id; }

    @Override
    public String getAuthIdentifierName() { return "id"; }
}</code></pre>

<div class="note">
    <strong>注意：</strong>Auth 以主键比对登录态，密码校验在应用层（Service/Controller）完成。Authenticatable 契约不包含 getAuthPassword() 方法。生产环境应使用 BCrypt 等加密算法校验密码。
</div>
@endsection
