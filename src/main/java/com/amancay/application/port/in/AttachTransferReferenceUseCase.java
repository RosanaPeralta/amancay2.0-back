package com.amancay.application.port.in;

import java.util.UUID;

import com.amancay.domain.model.Payment;

public interface AttachTransferReferenceUseCase {
    Payment attachTransferReference(UUID requesterId, UUID paymentId, String transferReference);
}
