package com.weacsoft.jaravel.app.listener;

import com.weacsoft.jaravel.app.event.OrderCreatedEvent;
import com.weacsoft.jaravel.vendor.event.Listener;
import com.weacsoft.jaravel.vendor.event.ListensTo;
import com.weacsoft.jaravel.vendor.event.ShouldQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 生成发票监听器，演示分发到 {@code invoices} 队列并延迟执行。
 * <p>
 * 与 {@link ProcessOrderPaymentListener} 监听同一事件但路由到不同队列，
 * 且通过 {@code delay() = 5000} 延迟 5 秒执行，模拟等待支付确认后生成发票。
 */
@Component
@ListensTo(OrderCreatedEvent.class)
public class GenerateInvoiceListener implements Listener<OrderCreatedEvent>, ShouldQueue {

    private static final Logger logger = LoggerFactory.getLogger(GenerateInvoiceListener.class);

    @Override
    public String queue() {
        return "invoices";
    }

    @Override
    public long delay() {
        return 5000; // 延迟 5 秒执行
    }

    @Override
    public void handle(OrderCreatedEvent event) {
        logger.info("[invoices] 生成发票: orderId={}, userId={}, amount={}",
                event.getOrderId(), event.getUserId(), event.getAmount());
        logger.info("[invoices] 发票生成完成: orderId={}", event.getOrderId());
    }
}
