package com.amancay.domain.port;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.amancay.domain.model.Payment;
import com.amancay.domain.model.PaymentStatus;

// Puerto de salida para persistir pagos; lo implementa PaymentRepositoryAdapter.
public interface PaymentRepositoryPort {
    Optional<Payment> findById(UUID id);

    // Mas nuevos primero.
    List<Payment> findByOrderId(UUID orderId);

    // Mas viejos primero.
    List<Payment> findByStatus(PaymentStatus status);

    boolean existsByOrderIdAndStatus(UUID orderId, PaymentStatus status);

    // Devuelve el pago tal como quedo persistido (con id y timestamps asignados).
    Payment save(Payment payment);
}
