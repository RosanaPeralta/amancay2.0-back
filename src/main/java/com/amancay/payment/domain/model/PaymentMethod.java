package com.amancay.payment.domain.model;

// Cada valor se mapea a una estrategia de cobro distinta (ver PaymentProcessorRouter).
public enum PaymentMethod {
    TARJETA_CREDITO,
    TARJETA_DEBITO,
    TRANSFERENCIA
}
