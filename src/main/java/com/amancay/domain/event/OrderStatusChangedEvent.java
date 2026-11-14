package com.amancay.domain.event;

import java.time.Instant;
import java.util.UUID;

import com.amancay.domain.model.Order;
import com.amancay.domain.model.OrderStatus;

// Se publica despues de persistir el cambio de estado. eventId identifica el evento
// para que los consumidores de la cola puedan descartar duplicados.
public record OrderStatusChangedEvent(UUID eventId, UUID orderId, UUID buyerId, OrderStatus previousStatus,
        OrderStatus newStatus, Instant occurredAt) {

    // La orden ya tiene aplicado el nuevo estado.
    public static OrderStatusChangedEvent of(Order order, OrderStatus previousStatus, Instant occurredAt) {
        return new OrderStatusChangedEvent(UUID.randomUUID(), order.getId(), order.getUserId(), previousStatus,
                order.getStatus(), occurredAt);
    }
}
