package com.amancay.service;

import org.springframework.stereotype.Component;

import com.amancay.dto.CreatePaymentRequest;
import com.amancay.order.domain.model.Order;

@Component
public class ManualConfirmationPaymentProcessor implements PaymentProcessor {
    @Override
    public PaymentResult process(Order order, CreatePaymentRequest request) {
        return PaymentResult.pending("Awaiting manual confirmation (bank transfer)");
    }
}
