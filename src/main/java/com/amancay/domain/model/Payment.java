package com.amancay.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

// Una orden puede tener varios Payment (uno por cada intento): asi se modela el
// reintento de un pago fallido, sin necesidad de una entidad extra. La orden se
// referencia solo por id.
public class Payment {

    private final UUID id;
    private final UUID orderId;
    private final BigDecimal amount;
    private final PaymentMethod method;
    private PaymentStatus status;
    // Solo para TRANSFERENCIA.
    private String transferReference;
    // Solo para MERCADO_PAGO: id del pago en Mercado Pago (se completa al aprobarse).
    private String externalId;
    // Solo para MERCADO_PAGO: URL del checkout al que se redirige al comprador.
    private final String checkoutUrl;
    private final Instant createdAt;
    private final Instant updatedAt;

    // Reconstruye un pago ya existente (lo usa el adaptador de persistencia).
    public Payment(UUID id, UUID orderId, BigDecimal amount, PaymentMethod method, PaymentStatus status,
            String transferReference, Instant createdAt, Instant updatedAt) {
        this(id, orderId, amount, method, status, transferReference, null, null, createdAt, updatedAt);
    }

    public Payment(UUID id, UUID orderId, BigDecimal amount, PaymentMethod method, PaymentStatus status,
            String transferReference, String externalId, String checkoutUrl, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.orderId = orderId;
        this.amount = amount;
        this.method = method;
        this.status = status;
        this.transferReference = transferReference;
        this.externalId = externalId;
        this.checkoutUrl = checkoutUrl;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // Un intento nuevo nace con el resultado que devolvio la estrategia de cobro.
    public static Payment attempt(UUID orderId, BigDecimal amount, PaymentMethod method, PaymentResult result) {
        return new Payment(null, orderId, amount, method, result.status(), null, null, result.checkoutUrl(), null,
                null);
    }

    public boolean isApproved() {
        return status == PaymentStatus.APROBADO;
    }

    public void ensureRetryable() {
        if (status != PaymentStatus.RECHAZADO) {
            throw new IllegalStateException("Only a rejected payment can be retried");
        }
    }

    // Se expone aparte para que el caso de uso rechace una decision invalida antes de
    // ir a buscar el pago.
    public static void requireFinalDecision(PaymentStatus decision) {
        if (!decision.isFinal()) {
            throw new IllegalArgumentException("status must be APROBADO or RECHAZADO");
        }
    }

    // El admin decide sobre una transferencia que quedo PENDIENTE.
    public void confirm(PaymentStatus decision) {
        requireFinalDecision(decision);
        if (status != PaymentStatus.PENDIENTE) {
            throw new IllegalStateException("Only a pending payment can be confirmed");
        }
        this.status = decision;
    }

    // Mercado Pago informo el pago como aprobado: queda aprobado sin intervencion del admin.
    public void approveFromProvider(String externalId) {
        if (method != PaymentMethod.MERCADO_PAGO) {
            throw new IllegalArgumentException("Only Mercado Pago payments are approved by the provider");
        }
        if (status != PaymentStatus.PENDIENTE) {
            throw new IllegalStateException("Only a pending payment can be approved");
        }
        if (externalId == null || externalId.isBlank()) {
            throw new IllegalArgumentException("externalId is required");
        }
        this.externalId = externalId;
        this.status = PaymentStatus.APROBADO;
    }

    // Se puede reemplazar si se vuelve a enviar antes de que el admin confirme.
    public void attachTransferReference(String reference) {
        if (method != PaymentMethod.TRANSFERENCIA) {
            throw new IllegalArgumentException("Only bank transfer payments accept a reference code");
        }
        if (status != PaymentStatus.PENDIENTE) {
            throw new IllegalStateException("Only a pending payment can receive a reference code");
        }
        this.transferReference = reference;
    }

    public UUID getId() {
        return id;
    }

    public UUID getOrderId() {
        return orderId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public PaymentMethod getMethod() {
        return method;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public String getTransferReference() {
        return transferReference;
    }

    public String getExternalId() {
        return externalId;
    }

    public String getCheckoutUrl() {
        return checkoutUrl;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
