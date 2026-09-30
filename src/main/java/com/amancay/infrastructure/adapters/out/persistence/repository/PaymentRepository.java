package com.amancay.infrastructure.adapters.out.persistence.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;

import com.amancay.entity.Payment;
import com.amancay.entity.PaymentStatus;

@NoRepositoryBean
public interface PaymentRepository extends JpaRepository<Payment, UUID> {
    List<Payment> findByOrderIdOrderByCreatedAtDesc(UUID orderId);

    // El panel de admin para revisar transferencias: pagos pendientes de TODOS los
    // usuarios, mas viejos primero (los que esperan hace mas tiempo, primero en la cola).
    List<Payment> findByStatusOrderByCreatedAtAsc(PaymentStatus status);
}
