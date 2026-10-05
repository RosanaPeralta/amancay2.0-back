package com.amancay.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import lombok.Getter;
import lombok.Setter;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import com.amancay.exceptions.InvalidPaymentStatusTransitionException;

// Un Order puede tener varios Payment (uno por cada intento): asi se modela el
// reintento de un pago fallido, sin necesidad de una entidad extra.
@Entity
@Table(name = "payments")
@Getter
@Setter
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", length = 36, nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    // Cada valor de PaymentMethod va a mapear a una estrategia de cobro distinta.
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PaymentMethod method;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentStatus status = PaymentStatus.PENDIENTE;

    // Solo se usa con TRANSFERENCIA: el comprador lo carga desde el detalle de la orden
    // mientras el pago sigue PENDIENTE, y el admin lo usa para confirmar o rechazar.
    @Column(name = "transfer_reference")
    private String transferReference;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    // Valida la transicion contra el propio PaymentStatus (patron State), igual que
    // Order.changeStatus(). No la usa attemptPayment: ahi el status se resuelve una sola
    // vez para una fila recien creada (nunca hubo un status previo persistido que
    // "transicionar"), asi que se asigna directo.
    public void changeStatus(PaymentStatus newStatus) {
        if (!status.canTransitionTo(newStatus)) {
            throw new InvalidPaymentStatusTransitionException(status, newStatus);
        }
        this.status = newStatus;
    }
}
