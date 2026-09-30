package com.amancay.payment.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import lombok.Getter;

// Una orden puede tener varios Payment (uno por cada intento): asi se modela el
// reintento de un pago fallido, sin necesidad de una entidad extra. La orden se
// referencia solo por id: pertenece al modulo order.
@Getter
public class Payment {

    private final UUID id;
    private final UUID orderId;
    private final BigDecimal amount;
    private final PaymentMethod method;
    private PaymentStatus status;
    // Solo se usa con TRANSFERENCIA: el comprador lo carga desde el detalle de la orden
    // mientras el pago sigue PENDIENTE, y el admin lo usa para confirmar o rechazar.
    private String transferReference;
    private final Instant createdAt;
    private final Instant updatedAt;

    // Reconstruye un pago ya existente (lo usa el adaptador de persistencia).
    public Payment(UUID id, UUID orderId, BigDecimal amount, PaymentMethod method, PaymentStatus status,
            String transferReference, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.orderId = orderId;
        this.amount = amount;
        this.method = method;
        this.status = status;
        this.transferReference = transferReference;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // Un intento nuevo nace con el resultado que devolvio la estrategia de cobro.
    public static Payment attempt(UUID orderId, BigDecimal amount, PaymentMethod method, PaymentResult result) {
        return new Payment(null, orderId, amount, method, result.status(), null, null, null);
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
}
