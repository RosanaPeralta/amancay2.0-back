package com.amancay.payment.adapter.out.persistence;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.amancay.payment.domain.model.PaymentStatus;

interface SpringDataPaymentRepository extends JpaRepository<PaymentJpaEntity, UUID> {
    List<PaymentJpaEntity> findByOrderIdOrderByCreatedAtDesc(UUID orderId);

    List<PaymentJpaEntity> findByStatusOrderByCreatedAtAsc(PaymentStatus status);

    boolean existsByOrderIdAndStatus(UUID orderId, PaymentStatus status);
}
