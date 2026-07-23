package com.weacsoft.jaravel.app.listener;

import com.weacsoft.jaravel.app.event.OrderCreatedEvent;
import com.weacsoft.jaravel.vendor.event.Listener;
import com.weacsoft.jaravel.vendor.event.ListensTo;
import com.weacsoft.jaravel.vendor.event.ShouldQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 处理订单支付监听器，演示分发到 {@code payments} 队列。
 * <p>
 * 与 {@link GenerateInvoiceListener} 监听同一事件，但路由到不同队列，
 * 两个监听器互不阻塞，可并行执行。
 */
@Component
@ListensTo(OrderCreatedEvent.class)
public class ProcessOrderPaymentListener implements Listener<OrderCreatedEvent>, ShouldQueue {

    private static final Logger logger = LoggerFactory.getLogger(ProcessOrderPaymentListener.class);

    @Override
    public String queue() {
        return "payments";
    }

    @Override
    public void handle(OrderCreatedEvent event) {
        logger.info("[payments] 处理订单支付: orderId={}, userId={}, amount={}",
                event.getOrderId(), event.getUserId(), event.getAmount());
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        logger.info("[payments] 订单支付完成: orderId={}", event.getOrderId());
    }
}
