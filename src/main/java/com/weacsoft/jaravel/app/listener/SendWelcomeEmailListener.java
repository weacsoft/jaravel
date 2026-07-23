package com.weacsoft.jaravel.app.listener;

import com.weacsoft.jaravel.app.event.UserRegisteredEvent;
import com.weacsoft.jaravel.vendor.event.Listener;
import com.weacsoft.jaravel.vendor.event.ListensTo;
import com.weacsoft.jaravel.vendor.event.ShouldQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 发送欢迎邮件监听器，演示 {@code ShouldQueue} + 自定义队列名。
 * <p>
 * 实现 {@link ShouldQueue} 并指定 {@code queue() = "emails"}，
 * 该监听器将被异步分发到 {@code emails} 队列执行，不阻塞主流程。
 * <p>
 * 在 sync 驱动（默认）下，使用内存队列（QueueManager）的 {@code emails} 线程池执行；
 * 在 database 驱动下，持久化到 {@code jobs} 表的 {@code emails} 队列，由 worker 消费。
 */
@Component
@ListensTo(UserRegisteredEvent.class)
public class SendWelcomeEmailListener implements Listener<UserRegisteredEvent>, ShouldQueue {

    private static final Logger logger = LoggerFactory.getLogger(SendWelcomeEmailListener.class);

    @Override
    public String queue() {
        return "emails";
    }

    @Override
    public void handle(UserRegisteredEvent event) {
        logger.info("[emails] 发送欢迎邮件: userId={}, userName={}", event.getUserId(), event.getUserName());
        // 模拟邮件发送耗时
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        logger.info("[emails] 欢迎邮件发送完成: userId={}", event.getUserId());
    }
}
