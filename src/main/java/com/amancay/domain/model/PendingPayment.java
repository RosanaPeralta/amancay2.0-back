package com.amancay.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PendingPayment(UUID paymentId, UUID orderId, BigDecimal amount, PaymentMethodType method,
        String transferReference, Instant createdAt, String buyerEmail) {
}