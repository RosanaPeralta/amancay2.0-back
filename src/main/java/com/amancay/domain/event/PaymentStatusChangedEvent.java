package com.amancay.domain.event;

import java.util.UUID;

import com.amancay.domain.model.PaymentStatus;

public record PaymentStatusChangedEvent(UUID paymentId, PaymentStatus previousStatus, PaymentStatus newStatus) {
}
