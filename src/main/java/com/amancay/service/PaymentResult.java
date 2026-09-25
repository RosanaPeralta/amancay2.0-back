package com.amancay.service;

import com.amancay.entity.PaymentStatus;

// Resultado de una simulacion de cobro. El reason solo viaja en la respuesta
// inmediata al front (create/retry): Payment no tiene columna para guardarlo,
// asi que no esta disponible al listar pagos pasados.
public record PaymentResult(PaymentStatus status, String reason) {

    public static PaymentResult approved() {
        return new PaymentResult(PaymentStatus.APROBADO, null);
    }

    public static PaymentResult rejected(String reason) {
        return new PaymentResult(PaymentStatus.RECHAZADO, reason);
    }

    public static PaymentResult pending(String reason) {
        return new PaymentResult(PaymentStatus.PENDIENTE, reason);
    }
}
