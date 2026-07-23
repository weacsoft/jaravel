package com.weacsoft.jaravel.app.listener;

import com.weacsoft.jaravel.app.event.UserRegisteredEvent;
import com.weacsoft.jaravel.vendor.event.Listener;
import com.weacsoft.jaravel.vendor.event.ListensTo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 记录用户注册日志监听器，演示同步执行（不实现 ShouldQueue）。
 * <p>
 * 未实现 {@code ShouldQueue}，该监听器将在事件分发时同步执行，
 * 适用于轻量级、需要立即完成的操作（如记录日志、更新计数器）。
 */
@Component
@ListensTo(UserRegisteredEvent.class)
public class RecordUserRegistrationListener implements Listener<UserRegisteredEvent> {

    private static final Logger logger = LoggerFactory.getLogger(RecordUserRegistrationListener.class);

    @Override
    public void handle(UserRegisteredEvent event) {
        logger.info("[sync] 记录用户注册: userId={}, userName={}", event.getUserId(), event.getUserName());
    }
}
