package com.amancay.infrastructure.adapter.out.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.amancay.domain.event.OrderStatusChangedEvent;
import com.amancay.infrastructure.messaging.OrderStatusChangedMessage;
import com.amancay.infrastructure.messaging.RabbitMqTopology;

// Reenvia a RabbitMQ los cambios de estado que publica SpringOrderEventPublisher, recien
// cuando la transaccion se confirmo: asi nunca sale a la cola un cambio que no se guardo.
// Si RabbitMQ no responde en ese momento el evento se pierde (queda en el log); para
// garantizar la entrega habria que pasar a un Transactional Outbox.
@Component
class RabbitOrderEventRelay {

    private static final Logger log = LoggerFactory.getLogger(RabbitOrderEventRelay.class);

    private final AmqpTemplate amqpTemplate;

    RabbitOrderEventRelay(AmqpTemplate amqpTemplate) {
        this.amqpTemplate = amqpTemplate;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void relay(OrderStatusChangedEvent event) {
        String routingKey = RabbitMqTopology.statusEventRoutingKey(event.newStatus());
        try {
            amqpTemplate.convertAndSend(RabbitMqTopology.ORDERS_EXCHANGE, routingKey,
                    OrderStatusChangedMessage.from(event), message -> {
                        message.getMessageProperties().setMessageId(event.eventId().toString());
                        return message;
                    });
            log.info("Order {} status event {} published to RabbitMQ ({})", event.orderId(), event.eventId(),
                    routingKey);
        } catch (AmqpException exception) {
            log.error("Could not publish order {} status event {} ({}) to RabbitMQ", event.orderId(),
                    event.eventId(), routingKey, exception);
        }
    }
}
