package com.amancay.infrastructure.messaging;

import java.time.Instant;
import java.util.UUID;

import com.amancay.domain.event.OrderStatusChangedEvent;
import com.amancay.domain.model.OrderStatus;

// Forma en la que viaja OrderStatusChangedEvent por RabbitMQ (JSON). Separado del evento
// de dominio para que el contrato de la cola no cambie si cambia el modelo.
public record OrderStatusChangedMessage(UUID eventId, UUID orderId, UUID buyerId, OrderStatus previousStatus,
        OrderStatus newStatus, Instant occurredAt) {

    public static OrderStatusChangedMessage from(OrderStatusChangedEvent event) {
        return new OrderStatusChangedMessage(event.eventId(), event.orderId(), event.buyerId(),
                event.previousStatus(), event.newStatus(), event.occurredAt());
    }

    public OrderStatusChangedEvent toEvent() {
        return new OrderStatusChangedEvent(eventId, orderId, buyerId, previousStatus, newStatus, occurredAt);
    }
}
