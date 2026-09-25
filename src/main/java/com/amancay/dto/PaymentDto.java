package com.amancay.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.amancay.entity.PaymentMethod;
import com.amancay.entity.PaymentStatus;

public record PaymentDto(
        UUID id,
        UUID orderId,
        BigDecimal amount,
        PaymentMethod method,
        PaymentStatus status,
        String reason,
        Instant createdAt) {
}
