package com.amancay.service;

import com.amancay.dto.CreatePaymentRequest;
import com.amancay.entity.Order;

public interface PaymentProcessor {
    PaymentResult process(Order order, CreatePaymentRequest request);
}
