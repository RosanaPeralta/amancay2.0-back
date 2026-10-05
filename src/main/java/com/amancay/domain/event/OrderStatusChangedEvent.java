package com.amancay.domain.event;

import java.util.UUID;

import com.amancay.domain.model.OrderStatus;

// Se publica despues de persistir el cambio de estado.
public record OrderStatusChangedEvent(UUID orderId, OrderStatus previousStatus, OrderStatus newStatus) {
}
