package com.amancay.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentRecord(UUID id, UUID orderId, UUID buyerId, BigDecimal amount, PaymentMethodType method,
        PaymentState state, String transferReference, Instant createdAt, String reason) {
}