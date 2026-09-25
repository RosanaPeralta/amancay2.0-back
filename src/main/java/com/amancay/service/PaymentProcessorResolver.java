package com.amancay.service;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.amancay.entity.PaymentMethod;

// Punto unico donde se elige la estrategia segun el metodo de pago. Agregar un
// metodo nuevo (ej: una pasarela real) es agregar una entrada en este mapa,
// sin tocar PaymentService.
@Component
public class PaymentProcessorResolver {

    private final Map<PaymentMethod, PaymentProcessor> processorsByMethod;

    public PaymentProcessorResolver(CardPaymentProcessor cardPaymentProcessor,
            InstantApprovalPaymentProcessor instantApprovalPaymentProcessor,
            ManualConfirmationPaymentProcessor manualConfirmationPaymentProcessor) {
        this.processorsByMethod = Map.of(
                PaymentMethod.TARJETA_CREDITO, cardPaymentProcessor,
                PaymentMethod.TARJETA_DEBITO, cardPaymentProcessor,
                PaymentMethod.MERCADO_PAGO, instantApprovalPaymentProcessor,
                PaymentMethod.TRANSFERENCIA, manualConfirmationPaymentProcessor,
                PaymentMethod.EFECTIVO, instantApprovalPaymentProcessor);
    }

    public PaymentProcessor resolve(PaymentMethod method) {
        PaymentProcessor processor = processorsByMethod.get(method);
        if (processor == null) {
            throw new IllegalStateException("No payment processor configured for method " + method);
        }
        return processor;
    }
}
