package com.amancay.infrastructure.adapter.in.web;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.amancay.application.port.in.ConfirmPaymentUseCase;
import com.amancay.application.port.in.ListPendingPaymentsQuery;
import com.amancay.infrastructure.adapter.in.web.dto.ConfirmPaymentRequest;
import com.amancay.infrastructure.adapter.in.web.dto.PaymentResponse;
import com.amancay.infrastructure.adapter.in.web.dto.PendingPaymentResponse;
import com.amancay.infrastructure.security.LoggedUser;

import jakarta.validation.Valid;

/** Revisión de pagos por transferencia. Solo ADMIN. */
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
    public ResponseEntity<List<PendingPaymentResponse>> listPending(@AuthenticationPrincipal LoggedUser loggedUser) {
        return ResponseEntity.ok(listPendingPaymentsQuery.listPending(loggedUser.id()).stream()
                .map(PendingPaymentResponse::from)
                .toList());
    }

    @PatchMapping("/{paymentId}/confirm")
    public ResponseEntity<PaymentResponse> confirm(@AuthenticationPrincipal LoggedUser loggedUser,
            @PathVariable UUID paymentId,
            @Valid @RequestBody ConfirmPaymentRequest request) {
        return ResponseEntity.ok(PaymentResponse.from(confirmPaymentUseCase.confirm(loggedUser.id(), paymentId,
                request.status())));
    }
}
