package com.amancay.infrastructure.adapter.in.web.dto;

import java.util.UUID;

import com.amancay.domain.event.PaymentStatusChangedEvent;
import com.amancay.domain.model.PaymentStatus;

import jakarta.validation.constraints.NotNull;

public record PaymentStatusChangedEventRequest(@NotNull UUID paymentId, @NotNull PaymentStatus previousStatus,
        @NotNull PaymentStatus newStatus) {

    public PaymentStatusChangedEvent toDomainEvent() {
        return new PaymentStatusChangedEvent(paymentId, previousStatus, newStatus);
    }
}
