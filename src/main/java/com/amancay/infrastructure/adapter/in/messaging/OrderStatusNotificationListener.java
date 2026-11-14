package com.amancay.infrastructure.adapter.in.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import com.amancay.application.port.in.NotifyOrderStatusChangeUseCase;
import com.amancay.domain.exception.BuyerEmailNotFoundException;
import com.amancay.domain.exception.OrderNotFoundException;
import com.amancay.infrastructure.messaging.OrderStatusChangedMessage;
import com.amancay.infrastructure.messaging.RabbitMqTopology;

// Consume todos los eventos order.status.*; los estados sin mail se descartan. Si el
// SMTP falla se reintenta; si la orden o el comprador no existen va a la DLQ.
@Component
class OrderStatusNotificationListener {

    private static final Logger log = LoggerFactory.getLogger(OrderStatusNotificationListener.class);

    private final NotifyOrderStatusChangeUseCase notifyOrderStatusChangeUseCase;

    OrderStatusNotificationListener(NotifyOrderStatusChangeUseCase notifyOrderStatusChangeUseCase) {
        this.notifyOrderStatusChangeUseCase = notifyOrderStatusChangeUseCase;
    }

    @RabbitListener(queues = RabbitMqTopology.ORDER_NOTIFICATIONS_QUEUE)
    public void onOrderStatusChanged(OrderStatusChangedMessage message) {
        log.info("Order {} changed status: {} -> {} (event {})", message.orderId(), message.previousStatus(),
                message.newStatus(), message.eventId());
        try {
            if (notifyOrderStatusChangeUseCase.notify(message.toEvent())) {
                log.info("Buyer of order {} notified about {}", message.orderId(), message.newStatus());
            }
        } catch (OrderNotFoundException | BuyerEmailNotFoundException exception) {
            log.warn("Event {} for order {} not notified: {}", message.eventId(), message.orderId(),
                    exception.getMessage());
            throw new AmqpRejectAndDontRequeueException(exception.getMessage(), exception);
        }
    }
}
