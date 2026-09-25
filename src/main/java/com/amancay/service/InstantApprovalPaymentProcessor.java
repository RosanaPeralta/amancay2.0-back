package com.amancay.service;

import org.springframework.stereotype.Component;

import com.amancay.dto.CreatePaymentRequest;
import com.amancay.entity.Order;

// Metodos sin tarjeta (efectivo, transferencia, Mercado Pago): todavia no hay
// integracion real con ninguno, se simulan siempre aprobados.
@Component
public class InstantApprovalPaymentProcessor implements PaymentProcessor {
    @Override
    public PaymentResult process(Order order, CreatePaymentRequest request) {
        return PaymentResult.approved();
    }
}
