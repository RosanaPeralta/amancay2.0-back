package com.amancay.domain.model;

// Cada valor se mapea a una estrategia de cobro distinta (ver PaymentProcessorRouter).
public enum PaymentMethod {
    TARJETA_CREDITO,
    TARJETA_DEBITO,
    TRANSFERENCIA,
    // Checkout Pro: lo confirma Mercado Pago (webhook o verificacion), no el admin.
    MERCADO_PAGO
}
