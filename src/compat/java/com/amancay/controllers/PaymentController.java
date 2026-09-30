package com.amancay.controllers;

public class PaymentController extends com.amancay.infrastructure.adapters.in.web.PaymentController {
    public PaymentController(com.amancay.service.PaymentService paymentService) {
        super(paymentService);
    }
}
