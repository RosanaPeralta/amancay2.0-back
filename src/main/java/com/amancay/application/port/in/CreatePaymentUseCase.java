package com.amancay.application.port.in;

import java.util.UUID;

import com.amancay.domain.model.PaymentDetails;

public interface CreatePaymentUseCase {
    PaymentOutcome create(UUID requesterId, UUID orderId, PaymentDetails details);
}
