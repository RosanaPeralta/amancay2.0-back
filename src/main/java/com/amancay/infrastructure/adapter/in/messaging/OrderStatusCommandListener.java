package com.amancay.infrastructure.adapter.in.messaging;

import java.time.Clock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import com.amancay.application.port.in.AdvanceOrderStatusCommand;
import com.amancay.application.port.in.ApplyOrderStatusCommandUseCase;
import com.amancay.application.port.in.ApplyOrderStatusCommandUseCase.Result;
import com.amancay.domain.exception.InvalidOrderStatusTransitionException;
import com.amancay.domain.exception.OrderNotFoundException;
import com.amancay.infrastructure.messaging.AdvanceOrderStatusMessage;
import com.amancay.infrastructure.messaging.RabbitMqTopology;

// Consumidor de la cola de logistica. Idempotente: un comando repetido no hace nada.
// Errores permanentes (orden inexistente, transicion invalida, mensaje incompleto) van
// directo a la dead letter queue; los demas (base caida, etc.) se reintentan antes.
@Component
class OrderStatusCommandListener {

    private static final Logger log = LoggerFactory.getLogger(OrderStatusCommandListener.class);

    private final ApplyOrderStatusCommandUseCase applyOrderStatusCommandUseCase;
    private final Clock clock;

    OrderStatusCommandListener(ApplyOrderStatusCommandUseCase applyOrderStatusCommandUseCase, Clock clock) {
        this.applyOrderStatusCommandUseCase = applyOrderStatusCommandUseCase;
        this.clock = clock;
    }

    @RabbitListener(queues = RabbitMqTopology.STATUS_COMMANDS_QUEUE)
    public void onAdvanceOrderStatus(AdvanceOrderStatusMessage message) {
        try {
            AdvanceOrderStatusCommand command = message.toCommand(clock.instant());
            Result result = applyOrderStatusCommandUseCase.apply(command);
            log.info("Command {} from {}: order {} -> {} ({})", command.commandId(), command.requestedBy(),
                    command.orderId(), command.targetStatus(), result);
        } catch (IllegalArgumentException | OrderNotFoundException | InvalidOrderStatusTransitionException exception) {
            log.warn("Command {} for order {} rejected: {}", message.messageId(), message.orderId(),
                    exception.getMessage());
            throw new AmqpRejectAndDontRequeueException(exception.getMessage(), exception);
        }
    }
}
