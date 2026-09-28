package com.amancay.controllers;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.amancay.dto.AdminPendingPaymentDto;
import com.amancay.dto.ConfirmPaymentRequest;
import com.amancay.dto.PaymentDto;
import com.amancay.service.PaymentService;

import jakarta.validation.Valid;

/**
 * El dueno de la tienda revisando que la plata de una transferencia realmente llego:
 * esta es la unica parte de la API que muestra pagos de todos los usuarios, no solo
 * los propios.
 */
@RestController
@RequestMapping("/api/admin/payments")
@PreAuthorize("hasRole('ADMIN')")
@Validated
public class AdminPaymentController {

    private final PaymentService paymentService;

    public AdminPaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @GetMapping("/pending")
    public ResponseEntity<List<AdminPendingPaymentDto>> listPending() {
        return ResponseEntity.ok(paymentService.listPendingForAdmin());
    }

    @PatchMapping("/{paymentId}/confirm")
    public ResponseEntity<PaymentDto> confirm(@PathVariable UUID paymentId,
            @Valid @RequestBody ConfirmPaymentRequest request) {
        return ResponseEntity.ok(paymentService.confirmPayment(paymentId, request));
    }
}
