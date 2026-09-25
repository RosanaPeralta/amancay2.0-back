package com.amancay.service;

import org.springframework.stereotype.Component;

import com.amancay.dto.CreatePaymentRequest;
import com.amancay.entity.Order;

// Transferencia bancaria: a diferencia de la tarjeta, no hay forma de saber en
// el momento si el dinero llego. El intento siempre queda PENDIENTE hasta que
// alguien (hoy: cualquier caller de PaymentService.confirmPayment; el dia de
// mañana, un admin desde el panel) lo confirme o lo rechace a mano.
@Component
public class ManualConfirmationPaymentProcessor implements PaymentProcessor {
    @Override
    public PaymentResult process(Order order, CreatePaymentRequest request) {
        return PaymentResult.pending("Awaiting manual confirmation (bank transfer)");
    }
}
