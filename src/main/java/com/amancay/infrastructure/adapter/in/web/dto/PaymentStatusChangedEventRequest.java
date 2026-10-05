package com.amancay.infrastructure.adapter.in.web.dto;

import java.util.UUID;

import com.amancay.domain.event.PaymentStatusChangedEvent;
import com.amancay.domain.model.PaymentStatus;

import jakarta.validation.constraints.NotNull;

// Lo que el servicio de cola entrega en POST /internal/events/payment-status-changed.
public record PaymentStatusChangedEventRequest(@NotNull UUID paymentId, @NotNull PaymentStatus previousStatus,
        @NotNull PaymentStatus newStatus) {

    public PaymentStatusChangedEvent toDomainEvent() {
        return new PaymentStatusChangedEvent(paymentId, previousStatus, newStatus);
    }
}
