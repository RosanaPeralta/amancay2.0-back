package com.amancay.application.port.in;

import java.util.UUID;

import com.amancay.domain.model.PaymentDetails;

public interface RetryPaymentUseCase {
    // Crea un intento nuevo para la misma orden; el pago rechazado queda como historial.
    PaymentOutcome retry(UUID requesterId, UUID paymentId, PaymentDetails details);
}
