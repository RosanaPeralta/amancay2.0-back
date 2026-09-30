package com.amancay.controllers;

public class AdminPaymentController extends com.amancay.infrastructure.adapters.in.web.AdminPaymentController {
    public AdminPaymentController(com.amancay.service.PaymentService paymentService) {
        super(paymentService);
    }
}
