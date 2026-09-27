package com.amancay.dto;

import com.amancay.entity.PaymentMethod;

import jakarta.validation.constraints.NotNull;

public record CreatePaymentRequest(
        @NotNull PaymentMethod method,
        CardData card) {

    public record CardData(
            String number,
            String holderName,
            String expiry,
            String cvv) {
    }
}
