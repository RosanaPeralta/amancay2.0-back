package com.amancay.infrastructure.adapter.in.web.dto;

import java.util.UUID;

import com.amancay.domain.event.OrderStatusChangedEvent;
import com.amancay.domain.model.OrderStatus;

import jakarta.validation.constraints.NotNull;

public record OrderStatusChangedEventRequest(@NotNull UUID orderId, @NotNull OrderStatus previousStatus,
        @NotNull OrderStatus newStatus) {

    public OrderStatusChangedEvent toDomainEvent() {
        return new OrderStatusChangedEvent(orderId, previousStatus, newStatus);
    }
}
