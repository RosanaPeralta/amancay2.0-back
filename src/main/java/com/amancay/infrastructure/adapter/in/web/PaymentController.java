package com.amancay.infrastructure.adapter.in.web;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.amancay.application.port.in.AttachTransferReferenceUseCase;
import com.amancay.application.port.in.CreatePaymentUseCase;
import com.amancay.application.port.in.ListOrderPaymentsQuery;
import com.amancay.application.port.in.RetryPaymentUseCase;
import com.amancay.application.port.in.VerifyPaymentUseCase;
import com.amancay.infrastructure.adapter.in.web.dto.AttachTransferReferenceRequest;
import com.amancay.infrastructure.adapter.in.web.dto.CreatePaymentRequest;
import com.amancay.infrastructure.adapter.in.web.dto.PaymentResponse;
import com.amancay.infrastructure.security.LoggedUser;

import jakarta.validation.Valid;

@RestController
@Validated
public class PaymentController {

    private final ListOrderPaymentsQuery listOrderPaymentsQuery;
    private final CreatePaymentUseCase createPaymentUseCase;
    private final RetryPaymentUseCase retryPaymentUseCase;
    private final AttachTransferReferenceUseCase attachTransferReferenceUseCase;
    private final VerifyPaymentUseCase verifyPaymentUseCase;

    public PaymentController(ListOrderPaymentsQuery listOrderPaymentsQuery, CreatePaymentUseCase createPaymentUseCase,
            RetryPaymentUseCase retryPaymentUseCase, AttachTransferReferenceUseCase attachTransferReferenceUseCase,
            VerifyPaymentUseCase verifyPaymentUseCase) {
        this.listOrderPaymentsQuery = listOrderPaymentsQuery;
        this.createPaymentUseCase = createPaymentUseCase;
        this.retryPaymentUseCase = retryPaymentUseCase;
        this.attachTransferReferenceUseCase = attachTransferReferenceUseCase;
        this.verifyPaymentUseCase = verifyPaymentUseCase;
    }

    @GetMapping("/api/orders/{orderId}/payments")
    public ResponseEntity<List<PaymentResponse>> listByOrder(
            @AuthenticationPrincipal LoggedUser loggedUser,
            @PathVariable UUID orderId) {
        return ResponseEntity.ok(listOrderPaymentsQuery.listByOrder(loggedUser.id(), orderId).stream()
                .map(PaymentResponse::from)
                .toList());
    }

    @PostMapping("/api/orders/{orderId}/payments")
    public ResponseEntity<PaymentResponse> create(
            @AuthenticationPrincipal LoggedUser loggedUser,
            @PathVariable UUID orderId,
            @Valid @RequestBody CreatePaymentRequest request) {
        return ResponseEntity.ok(PaymentResponse.from(
                createPaymentUseCase.create(loggedUser.id(), orderId, request.toDetails())));
    }

    @PostMapping("/api/payments/{paymentId}/retry")
    public ResponseEntity<PaymentResponse> retry(
            @AuthenticationPrincipal LoggedUser loggedUser,
            @PathVariable UUID paymentId,
            @Valid @RequestBody CreatePaymentRequest request) {
        return ResponseEntity.ok(PaymentResponse.from(
                retryPaymentUseCase.retry(loggedUser.id(), paymentId, request.toDetails())));
    }

    @PatchMapping("/api/payments/{paymentId}/transfer-reference")
    public ResponseEntity<PaymentResponse> attachTransferReference(
            @AuthenticationPrincipal LoggedUser loggedUser,
            @PathVariable UUID paymentId,
            @Valid @RequestBody AttachTransferReferenceRequest request) {
        return ResponseEntity.ok(PaymentResponse.from(attachTransferReferenceUseCase
                .attachTransferReference(loggedUser.id(), paymentId, request.transferReference())));
    }

    // El front lo llama al volver del checkout de Mercado Pago. Si sigue PENDIENTE, devuelve
    // el pago sin cambios (el comprador no termino de pagar o Mercado Pago todavia no lo aprobo).
    @PostMapping("/api/payments/{paymentId}/verify")
    public ResponseEntity<PaymentResponse> verify(
            @AuthenticationPrincipal LoggedUser loggedUser,
            @PathVariable UUID paymentId) {
        return ResponseEntity.ok(PaymentResponse.from(verifyPaymentUseCase.verify(loggedUser.id(), paymentId)));
    }
}
