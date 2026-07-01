package com.weacsoft.jaravel.app.job;

import com.weacsoft.jaravel.vendor.event.Event;

/**
 * 通知事件，对齐 Laravel 的事件定义。
 * <p>
 * 携带通知类型、接收者和内容等业务数据，分发后由
 * {@link SendNotificationJob} 异步处理（实现 ShouldQueue）。
 * <p>
 * 用法：
 * <pre>
 * Dispatcher dispatcher = SpringContext.bean(Dispatcher.class);
 * dispatcher.dispatch(new NotificationEvent("email", "alice@test.com", "您有新消息"));
 * </pre>
 */
public class NotificationEvent implements Event {

    private final String type;
    private final String recipient;
    private final String content;

    public NotificationEvent(String type, String recipient, String content) {
        this.type = type;
        this.recipient = recipient;
        this.content = content;
    }

    /** 通知类型：email / sms / webhook 等 */
    public String getType() {
        return type;
    }

    /** 接收者：邮箱地址 / 手机号 / webhook URL */
    public String getRecipient() {
        return recipient;
    }

    /** 通知内容 */
    public String getContent() {
        return content;
    }

    @Override
    public String toString() {
        return "NotificationEvent{type='" + type + "', recipient='" + recipient + "', content='" + content + "'}";
    }
}
