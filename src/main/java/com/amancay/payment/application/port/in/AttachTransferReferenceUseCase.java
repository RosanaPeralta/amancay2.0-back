package com.amancay.payment.application.port.in;

import java.util.UUID;

import com.amancay.payment.domain.model.Payment;

public interface AttachTransferReferenceUseCase {
    Payment attachTransferReference(UUID requesterId, UUID paymentId, String transferReference);
}
