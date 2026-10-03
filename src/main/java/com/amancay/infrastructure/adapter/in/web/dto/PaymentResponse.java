package com.amancay.infrastructure.adapter.in.web.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.amancay.application.port.in.PaymentOutcome;
import com.amancay.domain.model.Payment;
import com.amancay.domain.model.PaymentMethod;
import com.amancay.domain.model.PaymentStatus;

// Mismos campos que el antiguo PaymentDto: el JSON que recibe el front no cambia.
public record PaymentResponse(
        UUID id,
        UUID orderId,
        BigDecimal amount,
        PaymentMethod method,
        PaymentStatus status,
        String reason,
        String transferReference,
        Instant createdAt) {

    public static PaymentResponse from(Payment payment) {
        return from(payment, null);
    }

    public static PaymentResponse from(PaymentOutcome outcome) {
        return from(outcome.payment(), outcome.reason());
    }

    private static PaymentResponse from(Payment payment, String reason) {
        return new PaymentResponse(payment.getId(), payment.getOrderId(), payment.getAmount(), payment.getMethod(),
                payment.getStatus(), reason, payment.getTransferReference(), payment.getCreatedAt());
    }
}
