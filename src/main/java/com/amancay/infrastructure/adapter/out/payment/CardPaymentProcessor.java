package com.amancay.infrastructure.adapter.out.payment;

import org.springframework.stereotype.Component;

import com.amancay.application.port.out.PayableOrderPort.PayableOrder;
import com.amancay.domain.model.PaymentDetails;
import com.amancay.domain.model.PaymentResult;

// Gateway de tarjeta simulado: rechaza las tarjetas que terminan en 0000.
@Component
class CardPaymentProcessor implements PaymentProcessor {

    private static final String SIMULATED_DECLINE_SUFFIX = "0000";

    @Override
    public PaymentResult process(PayableOrder order, PaymentDetails details) {
        PaymentDetails.CardData card = details.card();
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
