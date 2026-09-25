package com.amancay.dto;

import com.amancay.entity.PaymentMethod;

import jakarta.validation.constraints.NotNull;

public record CreatePaymentRequest(
        @NotNull PaymentMethod method,
        CardData card) {

    // Datos de tarjeta tal cual los manda la UI: no hay validacion real (ni
    // Luhn, ni verificacion con el banco). Solo los usa CardPaymentProcessor.
    public record CardData(
            String number,
            String holderName,
            String expiry,
            String cvv) {
    }
}
