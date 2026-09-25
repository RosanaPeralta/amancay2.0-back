package com.amancay.entity;

// Cada valor se va a mapear a una implementacion del Strategy de cobro (PaymentService).
public enum PaymentMethod {
    TARJETA_CREDITO,
    TARJETA_DEBITO,
    MERCADO_PAGO,
    TRANSFERENCIA,
    EFECTIVO
}
