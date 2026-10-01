package com.amancay.domain.model;

public enum PaymentStatus {
    PENDIENTE,
    APROBADO,
    RECHAZADO;

    // Lo que el admin puede decidir sobre un pago pendiente.
    public boolean isFinal() {
        return this == APROBADO || this == RECHAZADO;
    }
}
