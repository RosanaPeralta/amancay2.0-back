package com.amancay.service;

import com.amancay.dto.CreatePaymentRequest;
import com.amancay.entity.Order;

// Strategy de cobro: una implementacion por familia de PaymentMethod. Ninguna
// implementacion cobra de verdad todavia (no hay pasarela real integrada);
// todas simulan un resultado con una regla determinista, para poder demostrar
// el flujo de aprobado/rechazado/reintento de forma reproducible.
public interface PaymentProcessor {
    PaymentResult process(Order order, CreatePaymentRequest request);
}
