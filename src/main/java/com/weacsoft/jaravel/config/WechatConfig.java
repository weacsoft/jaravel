package com.weacsoft.jaravel.config;

import com.weacsoft.jaravel.vendor.wechat.WechatProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 微信 SDK 配置，对齐 Laravel 的 {@code config/easywechat.php}。
 * <p>
 * 微信 SDK 的 Bean（{@code OkHttpClient}、{@code AccessTokenManager}、
 * {@code OfficialAccountService}、{@code MiniProgramService}）由
 * {@code WechatAutoConfiguration} 自动装配创建，本类仅用于：
 * <ul>
 *   <li>启用 {@link WechatProperties} 配置属性绑定</li>
 *   <li>在启动时打印微信配置摘要，便于排查问题</li>
 * </ul>
 * <p>
 * 配置示例（application.yml）：
 * <pre>
 * jaravel:
 *   wechat:
 *     enabled: true
 *     official-accounts:
 *       default:
 *         app-id: your-app-id
 *         secret: your-secret
 *     mini-apps:
 *       default:
 *         app-id: your-mini-app-id
 *         secret: your-mini-secret
 * </pre>
 * <p>
 * 使用时直接注入即可：
 * <pre>
 * &#64;Autowired
 * private OfficialAccountService mpService;   // 公众号服务
 *
 * &#64;Autowired
 * private MiniProgramService miniService;     // 小程序服务
 *
 * &#64;Autowired
 * private AccessTokenManager tokenManager;    // access_token 管理器
 * </pre>
 */
@Configuration
@EnableConfigurationProperties(WechatProperties.class)
public class WechatConfig {

    private static final Logger log = LoggerFactory.getLogger(WechatConfig.class);

    private final WechatProperties wechatProperties;

    public WechatConfig(WechatProperties wechatProperties) {
        this.wechatProperties = wechatProperties;
        logConfigSummary();
    }

    /**
     * 启动时打印微信配置摘要（脱敏，不输出 secret 全文）。
     */
    private void logConfigSummary() {
        log.info("[Wechat] SDK 已启用: {}", wechatProperties.isEnabled());

        wechatProperties.getOfficialAccounts().forEach((name, config) -> {
            log.info("[Wechat] 公众号配置 [{}] appId={}, secret={}****",
                    name, config.getAppId(),
                    config.getSecret() != null && config.getSecret().length() > 4
                            ? config.getSecret().substring(0, 4) : "(empty)");
        });

        wechatProperties.getMiniApps().forEach((name, config) -> {
            log.info("[Wechat] 小程序配置 [{}] appId={}, secret={}****",
                    name, config.getAppId(),
                    config.getSecret() != null && config.getSecret().length() > 4
                            ? config.getSecret().substring(0, 4) : "(empty)");
        });

        log.info("[Wechat] HTTP 超时={}s, 重试={}",
                wechatProperties.getHttp().getTimeout(),
                wechatProperties.getHttp().isRetry());
    }
}
