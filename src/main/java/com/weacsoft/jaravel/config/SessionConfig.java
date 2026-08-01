package com.weacsoft.jaravel.config;

import com.weacsoft.jaravel.vendor.http.session.SessionStore;
import org.springframework.context.annotation.Configuration;

/**
 * Session 存储配置（对齐 Laravel 的 {@code config/session.php}）。
 *
 * <h2>一、Session 存储是全局的</h2>
 * Session 存储<b>不与 Guard 绑定</b>。{@link AuthConfig} 里所有 {@code driver: session}
 * 的守卫，共用此处决定的同一个 {@link SessionStore} 实现。
 *
 * <h2>二、三层优先级：声明 → 配置 → 默认</h2>
 * <ol>
 *   <li><b>声明</b>：本类中注册 {@code @Bean SessionStore}（下方方式一/方式三）</li>
 *   <li><b>配置</b>：{@code application.yml} 的 {@code jaravel.session.driver}（下方方式二）</li>
 *   <li><b>默认（兜底）</b>：什么都不写 → {@code CookieSessionStore}（Servlet HttpSession）</li>
 * </ol>
 *
 * <h2>三、"安装 ≠ 启用"</h2>
 * 即使 pom 里引入了 {@code session-redis}，只要没有出现 {@code driver: redis}，
 * {@code RedisSessionStore} 也<b>不会装配</b>，不会尝试连接 Redis，不会报错。
 *
 * <h2>四、驱动一览</h2>
 * <table border="1">
 *   <caption>可用的 session 驱动</caption>
 *   <tr><th>driver</th><th>实现</th><th>依赖</th><th>说明</th></tr>
 *   <tr><td>{@code cookie}</td><td>CookieSessionStore</td><td>http（内置）</td><td><b>兜底默认</b>，基于 Servlet HttpSession</td></tr>
 *   <tr><td>{@code redis}</td><td>RedisSessionStore</td><td>session-redis</td><td>多实例共享登录态</td></tr>
 *   <tr><td>自定义</td><td>实现 SessionStore</td><td>—</td><td>见方式三</td></tr>
 * </table>
 */
@Configuration
public class SessionConfig {

    // =====================================================================
    // 方式零（本项目采用）：兜底默认 —— 什么都不写
    // =====================================================================
    // http 模块的 HttpSessionAutoConfiguration 会在没有任何 SessionStore 时，
    // 自动注册 CookieSessionStore（Servlet HttpSession）作为默认实现。
    // 启动日志可见：Session 存储 = CookieSessionStore（默认回退）

    // =====================================================================
    // 方式一：声明式 —— Redis 存储（需引入 session-redis 依赖）
    // =====================================================================
    // <dependency>
    //     <groupId>io.github.lijialong1313</groupId>
    //     <artifactId>session-redis</artifactId>
    //     <version>0.1.2</version>
    // </dependency>
    //
    // @Bean
    // public SessionStore sessionStore(RedisManager redisManager) {
    //     // 参数：RedisManager, redis 连接名, key 前缀, 过期分钟数, 集群/库标识
    //     return new RedisSessionStore(redisManager, "default", "session", 30, "manage_session");
    // }

    // =====================================================================
    // 方式二：配置式 —— application.yml
    // =====================================================================
    // jaravel:
    //   session:
    //     driver: redis          # cookie（默认兜底）| redis
    //     lifetime: 30           # 分钟
    //     prefix: "session"
    //     connection: default    # 使用 jaravel.redis.connections 里的哪个连接
    //
    // 只有写了 driver: redis，RedisSessionStore 才会被装配（安装 ≠ 启用）。

    // =====================================================================
    // 方式三：自定义存储 —— 实现 SessionStore 接口
    // =====================================================================
    // 适合接入 Memcached、数据库表、或者"cookie 内容用 JWT 签名"等自定义方案。
    //
    // @Bean
    // public SessionStore sessionStore() {
    //     return new MyCustomSessionStore();
    // }
}
