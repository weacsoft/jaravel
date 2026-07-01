package com.weacsoft.jaravel.app.job;

import com.weacsoft.jaravel.vendor.event.Listener;
import com.weacsoft.jaravel.vendor.event.ListensTo;
import com.weacsoft.jaravel.vendor.event.ShouldQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 发送通知的队列任务，对齐 Laravel 中实现 {@code ShouldQueue} 的队列化监听器。
 * <p>
 * 通过 {@code @ListensTo(NotificationEvent.class)} 自动注册到事件调度器，
 * 当 {@link NotificationEvent} 被分发时，本监听器将被异步分发到 "notification" 队列执行。
 * <p>
 * 关键点：
 * <ul>
 *   <li>实现 {@link ShouldQueue} 标记异步执行</li>
 *   <li>{@link #queue()} 返回 "notification"，使用独立线程池，不阻塞其他队列</li>
 *   <li>{@link #delay()} 返回 0，立即执行（可配置延迟）</li>
 *   <li>执行失败时自动重试（由 QueueManager 配置的重试策略控制）</li>
 * </ul>
 * <p>
 * 用法：
 * <pre>
 * // 分发事件后，本 Job 自动异步执行
 * Dispatcher dispatcher = SpringContext.bean(Dispatcher.class);
 * dispatcher.dispatch(new NotificationEvent("email", "alice@test.com", "欢迎注册"));
 * </pre>
 */
@Component
@ListensTo(NotificationEvent.class)
public class SendNotificationJob implements Listener<NotificationEvent>, ShouldQueue {

    private static final Logger log = LoggerFactory.getLogger(SendNotificationJob.class);

    @Override
    public void handle(NotificationEvent event) {
        log.info("[队列] 开始发送通知 | 类型={} 接收者={} 内容={}",
                event.getType(), event.getRecipient(), event.getContent());

        try {
            // 模拟发送耗时操作
            Thread.sleep(500);

            // 根据 type 分发到不同的通知渠道
            switch (event.getType()) {
                case "email":
                    sendEmail(event.getRecipient(), event.getContent());
                    break;
                case "sms":
                    sendSms(event.getRecipient(), event.getContent());
                    break;
                case "webhook":
                    sendWebhook(event.getRecipient(), event.getContent());
                    break;
                default:
                    log.warn("[队列] 未知通知类型: {}", event.getType());
            }

            log.info("[队列] 通知发送完成 | 接收者={}", event.getRecipient());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("[队列] 通知发送被中断 | 接收者={}", event.getRecipient(), e);
        } catch (Exception e) {
            log.error("[队列] 通知发送失败 | 接收者={}", event.getRecipient(), e);
            throw new RuntimeException("通知发送失败", e);
        }
    }

    @Override
    public String queue() {
        // 使用独立的 notification 队列，不阻塞 default / email 等其他队列
        return "notification";
    }

    @Override
    public long delay() {
        // 立即执行，无延迟
        return 0;
    }

    private void sendEmail(String to, String content) {
        log.info("[队列] -> 发送邮件到 {} : {}", to, content);
    }

    private void sendSms(String to, String content) {
        log.info("[队列] -> 发送短信到 {} : {}", to, content);
    }

    private void sendWebhook(String url, String content) {
        log.info("[队列] -> 发送 Webhook 到 {} : {}", url, content);
    }
}
