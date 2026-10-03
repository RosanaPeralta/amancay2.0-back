package com.amancay.infrastructure.adapter.out.persistence.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.amancay.domain.model.PaymentStatus;
import com.amancay.infrastructure.adapter.out.persistence.entity.PaymentJpaEntity;

public interface SpringDataPaymentRepository extends JpaRepository<PaymentJpaEntity, UUID> {
    List<PaymentJpaEntity> findByOrderIdOrderByCreatedAtDesc(UUID orderId);

    List<PaymentJpaEntity> findByStatusOrderByCreatedAtAsc(PaymentStatus status);

    boolean existsByOrderIdAndStatus(UUID orderId, PaymentStatus status);
}
