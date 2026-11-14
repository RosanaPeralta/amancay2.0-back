package com.amancay.infrastructure.adapter.out.messaging;

import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.stereotype.Component;

import com.amancay.application.port.in.AdvanceOrderStatusCommand;
import com.amancay.application.port.out.SendOrderCommandPort;
import com.amancay.domain.exception.MessagingUnavailableException;
import com.amancay.infrastructure.messaging.AdvanceOrderStatusMessage;
import com.amancay.infrastructure.messaging.RabbitMqTopology;

@Component
class RabbitOrderCommandSender implements SendOrderCommandPort {

    private final AmqpTemplate amqpTemplate;

    RabbitOrderCommandSender(AmqpTemplate amqpTemplate) {
        this.amqpTemplate = amqpTemplate;
    }

    @Override
    public void send(AdvanceOrderStatusCommand command) {
        try {
            amqpTemplate.convertAndSend(RabbitMqTopology.ORDERS_EXCHANGE, RabbitMqTopology.ADVANCE_ORDER_ROUTING_KEY,
                    AdvanceOrderStatusMessage.from(command), message -> {
                        message.getMessageProperties().setMessageId(command.commandId().toString());
                        return message;
                    });
        } catch (AmqpException exception) {
            throw new MessagingUnavailableException("La cola de pedidos no esta disponible", exception);
        }
    }
}
