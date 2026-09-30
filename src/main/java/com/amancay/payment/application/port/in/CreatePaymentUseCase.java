package com.amancay.payment.application.port.in;

import java.util.UUID;

import com.amancay.payment.domain.model.PaymentDetails;

public interface CreatePaymentUseCase {
    PaymentOutcome create(UUID requesterId, UUID orderId, PaymentDetails details);
}
