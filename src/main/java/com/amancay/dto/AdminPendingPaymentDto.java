package com.amancay.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.amancay.entity.PaymentMethod;

public record AdminPendingPaymentDto(
        UUID paymentId,
        UUID orderId,
        BigDecimal amount,
        PaymentMethod method,
        String transferReference,
        Instant createdAt,
        String buyerEmail) {
}
