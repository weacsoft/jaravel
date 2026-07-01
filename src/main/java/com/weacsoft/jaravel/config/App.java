package com.weacsoft.jaravel.config;

import org.springframework.context.annotation.Configuration;

/**
 * 应用引导配置，对齐 Laravel 的 {@code bootstrap/app.php}。
 * <p>
 * 全局中间件注册已移至 {@link com.weacsoft.jaravel.app.provider.RouteServiceProvider#boot()}，
 * 通过 Spring {@code @Component} 单例管理无状态中间件，对齐 Laravel 在 RouteServiceProvider
 * 统一注册系统中间件的做法。
 * <p>
 * 此类保留用于未来扩展其他全局引导逻辑（如异常处理、全局生命周期回调等）。
 */
@Configuration
public class App {
}
