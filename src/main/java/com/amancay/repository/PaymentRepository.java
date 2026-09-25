package com.amancay.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.amancay.entity.Payment;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {
    List<Payment> findByOrderIdOrderByCreatedAtDesc(UUID orderId);
}
