package com.amancay.infrastructure.adapter.in.web;

import com.amancay.domain.model.PaymentDetails;
import com.amancay.domain.model.PaymentMethod;

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

    PaymentDetails toDetails() {
        PaymentDetails.CardData cardData = card == null ? null
                : new PaymentDetails.CardData(card.number(), card.holderName(), card.expiry(), card.cvv());
        return new PaymentDetails(method, cardData);
    }
}
