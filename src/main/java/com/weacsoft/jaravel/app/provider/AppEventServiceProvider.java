package com.weacsoft.jaravel.app.provider;

import com.weacsoft.jaravel.vendor.event.EventServiceProvider;
import com.weacsoft.jaravel.vendor.event.example.LogRegistrationListener;
import com.weacsoft.jaravel.vendor.event.example.SendWelcomeEmailListener;
import com.weacsoft.jaravel.vendor.event.example.UserRegisteredEvent;
import org.springframework.stereotype.Component;

/**
 * 事件服务提供者，对齐 Laravel 的 {@code App\Providers\EventServiceProvider}。
 * <p>
 * 在 {@link #register()} 阶段注册事件监听器，对齐 Laravel 的 {@code $listen} 属性。
 * 由 {@code ProviderRegistry} 在所有 Bean 就绪后自动调用。
 * <p>
 * 示例：监听 {@link UserRegisteredEvent}，触发日志记录与欢迎邮件发送。
 */
@Component
public class AppEventServiceProvider extends EventServiceProvider {

    @Override
    public void register() {
        listen(UserRegisteredEvent.class, new LogRegistrationListener());
        listen(UserRegisteredEvent.class, new SendWelcomeEmailListener());
    }
}
