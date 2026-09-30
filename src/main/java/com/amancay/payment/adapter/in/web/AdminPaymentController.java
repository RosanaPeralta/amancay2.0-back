package com.amancay.payment.adapter.in.web;

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

import com.amancay.payment.application.port.in.ConfirmPaymentUseCase;
import com.amancay.payment.application.port.in.ListPendingPaymentsQuery;

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

    private final ListPendingPaymentsQuery listPendingPaymentsQuery;
    private final ConfirmPaymentUseCase confirmPaymentUseCase;

    public AdminPaymentController(ListPendingPaymentsQuery listPendingPaymentsQuery,
            ConfirmPaymentUseCase confirmPaymentUseCase) {
        this.listPendingPaymentsQuery = listPendingPaymentsQuery;
        this.confirmPaymentUseCase = confirmPaymentUseCase;
    }

    @GetMapping("/pending")
    public ResponseEntity<List<PendingPaymentResponse>> listPending() {
        return ResponseEntity.ok(listPendingPaymentsQuery.listPending().stream()
                .map(PendingPaymentResponse::from)
                .toList());
    }

    @PatchMapping("/{paymentId}/confirm")
    public ResponseEntity<PaymentResponse> confirm(@PathVariable UUID paymentId,
            @Valid @RequestBody ConfirmPaymentRequest request) {
        return ResponseEntity.ok(PaymentResponse.from(confirmPaymentUseCase.confirm(paymentId, request.status())));
    }
}
