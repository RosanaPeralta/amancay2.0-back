package com.amancay.payment.adapter.in.web;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.amancay.payment.application.port.in.ListPendingPaymentsQuery.PendingPayment;
import com.amancay.payment.domain.model.PaymentMethod;

// Mismos campos que el antiguo AdminPendingPaymentDto.
public record PendingPaymentResponse(
        UUID paymentId,
        UUID orderId,
        BigDecimal amount,
        PaymentMethod method,
        String transferReference,
        Instant createdAt,
        String buyerEmail) {

    static PendingPaymentResponse from(PendingPayment pending) {
        return new PendingPaymentResponse(pending.payment().getId(), pending.payment().getOrderId(),
                pending.payment().getAmount(), pending.payment().getMethod(),
                pending.payment().getTransferReference(), pending.payment().getCreatedAt(), pending.buyerEmail());
    }
}
