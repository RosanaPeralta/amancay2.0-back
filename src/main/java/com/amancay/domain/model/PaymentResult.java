package com.amancay.domain.model;

// Lo que devuelve una estrategia de cobro. reason no se persiste: solo viaja en la
// respuesta del intento, para que el front pueda mostrar por que se rechazo.
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
