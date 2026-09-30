package com.amancay.service;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.amancay.entity.PaymentMethod;

@Component
public class PaymentProcessorResolver {

    private final Map<PaymentMethod, PaymentProcessor> processorsByMethod;

    public PaymentProcessorResolver(CardPaymentProcessor cardPaymentProcessor,
            ManualConfirmationPaymentProcessor manualConfirmationPaymentProcessor) {
        this.processorsByMethod = Map.of(
                PaymentMethod.TARJETA_CREDITO, cardPaymentProcessor,
                PaymentMethod.TARJETA_DEBITO, cardPaymentProcessor,
                PaymentMethod.TRANSFERENCIA, manualConfirmationPaymentProcessor);
    }

    public PaymentProcessor resolve(PaymentMethod method) {
        PaymentProcessor processor = processorsByMethod.get(method);
        if (processor == null) {
            throw new IllegalStateException("No payment processor configured for method " + method);
        }
        return processor;
    }
}
