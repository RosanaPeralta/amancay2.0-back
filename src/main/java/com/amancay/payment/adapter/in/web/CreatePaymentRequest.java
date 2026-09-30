package com.amancay.payment.adapter.in.web;

import com.amancay.payment.domain.model.PaymentDetails;
import com.amancay.payment.domain.model.PaymentMethod;

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
