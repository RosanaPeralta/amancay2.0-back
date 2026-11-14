package com.amancay.application.port.in;

import java.util.UUID;

import com.amancay.domain.model.Payment;

// El comprador vuelve del checkout de Mercado Pago y pide revisar su pago. Si el
// proveedor ya lo tiene aprobado, se aprueba aca tambien (sin esperar al webhook).
public interface VerifyPaymentUseCase {
    Payment verify(UUID requesterId, UUID paymentId);
}
