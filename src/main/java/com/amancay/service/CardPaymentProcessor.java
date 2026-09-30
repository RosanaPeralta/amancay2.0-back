package com.amancay.service;

import org.springframework.stereotype.Component;

import com.amancay.dto.CreatePaymentRequest;
import com.amancay.order.domain.model.Order;

@Component
public class CardPaymentProcessor implements PaymentProcessor {

    private static final String SIMULATED_DECLINE_SUFFIX = "0000";

    @Override
    public PaymentResult process(Order order, CreatePaymentRequest request) {
        CreatePaymentRequest.CardData card = request.card();
        if (card == null || card.number() == null || card.number().isBlank()) {
            return PaymentResult.rejected("Card data is required for this payment method");
        }

        String digitsOnly = card.number().replaceAll("\\s+", "");
        if (digitsOnly.endsWith(SIMULATED_DECLINE_SUFFIX)) {
            return PaymentResult.rejected("Card declined (simulated)");
        }
        return PaymentResult.approved();
    }
}
