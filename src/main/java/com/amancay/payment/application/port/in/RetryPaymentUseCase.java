package com.amancay.payment.application.port.in;

import java.util.UUID;

import com.amancay.payment.domain.model.PaymentDetails;

public interface RetryPaymentUseCase {
    // Crea un intento nuevo para la misma orden; el pago rechazado queda como historial.
    PaymentOutcome retry(UUID requesterId, UUID paymentId, PaymentDetails details);
}
