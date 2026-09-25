package com.amancay.service;

import org.springframework.stereotype.Component;

import com.amancay.dto.CreatePaymentRequest;
import com.amancay.entity.Order;

// Simula el cobro con tarjeta (credito/debito). No valida la tarjeta contra
// ningun banco ni pasarela real, ni siquiera con el algoritmo de Luhn: la
// unica regla es una convencion de testing (la misma que usan sandboxes reales
// como Mercado Pago o Stripe) para poder demostrar el flujo de rechazo y
// reintento de forma reproducible en vez de al azar.
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
