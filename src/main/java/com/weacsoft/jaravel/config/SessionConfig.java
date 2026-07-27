package com.weacsoft.jaravel.config;

import com.weacsoft.jaravel.vendor.auth.contract.SessionStore;
import org.springframework.context.annotation.Configuration;

/**
 * Session 存储配置，对齐 Laravel 的 {@code config/session.php}。
 * <p>
 * <b>Session 存储是全局配置，不与 Guard 绑定</b>。所有 {@code session} 驱动的守卫
 * 都使用此处注册的 {@link SessionStore} 实现。
 * <p>
 * <h3>默认实现</h3>
 * 如果本配置类不注册任何 {@code SessionStore} Bean，auth 模块的
 * {@code AuthAutoConfiguration} 会自动提供 {@code CookieSessionStore}
 * （使用 Servlet 容器的 HttpSession，即 Cookie 方式）。
 * <p>
 * <h3>切换为 Redis 存储</h3>
 * 引入 {@code session-redis} 依赖后，取消下方注释即可使用 Redis 存储 Session：
 * <pre>
 * &lt;dependency&gt;
 *     &lt;groupId&gt;io.github.lijialong1313&lt;/artifactId&gt;
 *     &lt;artifactId&gt;session-redis&lt;/artifactId&gt;
 *     &lt;version&gt;0.1.2&lt;/version&gt;
 * &lt;/dependency&gt;
 * </pre>
 *
 * <pre>
 * &#64;Bean
 * public SessionStore sessionStore(RedisManager redisManager) {
 *     return new RedisSessionStore(redisManager, "default", "session", 30, "manage_session");
 * }
 * </pre>
 *
 * <h3>自定义存储</h3>
 * 实现 {@link SessionStore} 接口并在此注册为 {@code @Bean} 即可：
 * <pre>
 * &#64;Bean
 * public SessionStore sessionStore() {
 *     return new MyCustomSessionStore();
 * }
 * </pre>
 */
@Configuration
public class SessionConfig {
    // 默认使用 CookieSessionStore（Servlet HttpSession）
    // auth 模块的 AuthAutoConfiguration 会自动注册 CookieSessionStore 作为默认 SessionStore Bean
    //
    // 如需切换为 Redis Session 存储，取消以下注释并引入 session-redis 依赖：
    //
    // @Bean
    // public SessionStore sessionStore(RedisManager redisManager) {
    //     return new RedisSessionStore(redisManager, "default", "session", 30, "manage_session");
    // }
}
