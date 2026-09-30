package com.amancay.service;

import com.amancay.dto.CreatePaymentRequest;
import com.amancay.order.domain.model.Order;

public interface PaymentProcessor {
    PaymentResult process(Order order, CreatePaymentRequest request);
}
