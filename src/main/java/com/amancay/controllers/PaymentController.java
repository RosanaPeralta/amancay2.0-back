package com.amancay.controllers;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.amancay.dto.ConfirmPaymentRequest;
import com.amancay.dto.CreatePaymentRequest;
import com.amancay.dto.PaymentDto;
import com.amancay.service.PaymentService;

import jakarta.validation.Valid;

@RestController
@Validated
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @GetMapping("/api/orders/{orderId}/payments")
    public ResponseEntity<List<PaymentDto>> listByOrder(@PathVariable UUID orderId) {
        return ResponseEntity.ok(paymentService.listByOrder(orderId));
    }

    @PostMapping("/api/orders/{orderId}/payments")
    public ResponseEntity<PaymentDto> create(@PathVariable UUID orderId,
            @Valid @RequestBody CreatePaymentRequest request) {
        return ResponseEntity.ok(paymentService.createPayment(orderId, request));
    }

    @PostMapping("/api/payments/{paymentId}/retry")
    public ResponseEntity<PaymentDto> retry(@PathVariable UUID paymentId,
            @Valid @RequestBody CreatePaymentRequest request) {
        return ResponseEntity.ok(paymentService.retryPayment(paymentId, request));
    }

    // Sin @PreAuthorize todavia a proposito: la seguridad de este modulo (igual
    // que la de OrderController) se resuelve junto en la Fase 2, no aca.
    @PatchMapping("/api/payments/{paymentId}/confirm")
    public ResponseEntity<PaymentDto> confirm(@PathVariable UUID paymentId,
            @Valid @RequestBody ConfirmPaymentRequest request) {
        return ResponseEntity.ok(paymentService.confirmPayment(paymentId, request));
    }
}
