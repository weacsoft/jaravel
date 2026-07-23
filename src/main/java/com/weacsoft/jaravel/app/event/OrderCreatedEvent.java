package com.weacsoft.jaravel.app.event;

import com.weacsoft.jaravel.vendor.event.Event;

/**
 * 订单创建事件，演示 event/listener 分发到不同队列。
 * <p>
 * 该事件有两个监听器：
 * <ul>
 *   <li>{@code ProcessOrderPaymentListener} — 分发到 {@code payments} 队列异步执行</li>
 *   <li>{@code GenerateInvoiceListener} — 分发到 {@code invoices} 队列异步执行（延迟 5 秒）</li>
 * </ul>
 * 演示了同一事件的不同监听器可以被路由到不同的命名队列，互不阻塞。
 */
public class OrderCreatedEvent implements Event {

    private final Long orderId;
    private final Long userId;
    private final Double amount;

    public OrderCreatedEvent(Long orderId, Long userId, Double amount) {
        this.orderId = orderId;
        this.userId = userId;
        this.amount = amount;
    }

    public Long getOrderId() {
        return orderId;
    }

    public Long getUserId() {
        return userId;
    }

    public Double getAmount() {
        return amount;
    }
}
