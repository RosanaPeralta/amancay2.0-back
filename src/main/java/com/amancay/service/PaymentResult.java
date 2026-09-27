package com.amancay.service;

import com.amancay.entity.PaymentStatus;

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
