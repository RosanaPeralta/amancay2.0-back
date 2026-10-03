package com.amancay.application.port.in;

import java.util.UUID;

import com.amancay.domain.model.Payment;
import com.amancay.domain.model.PaymentStatus;

// El admin aprueba o rechaza una transferencia pendiente. Sin chequeo de dueno: solo
// se llega via AdminPaymentController, que ya exige ADMIN.
public interface ConfirmPaymentUseCase {
    Payment confirm(UUID requesterId, UUID paymentId, PaymentStatus decision);
}
