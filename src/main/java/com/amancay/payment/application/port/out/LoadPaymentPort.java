package com.amancay.payment.application.port.out;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.amancay.payment.domain.model.Payment;
import com.amancay.payment.domain.model.PaymentStatus;

public interface LoadPaymentPort {
    Optional<Payment> findById(UUID id);

    // Mas nuevos primero.
    List<Payment> findByOrderId(UUID orderId);

    // Mas viejos primero.
    List<Payment> findByStatus(PaymentStatus status);

    boolean existsByOrderIdAndStatus(UUID orderId, PaymentStatus status);
}
