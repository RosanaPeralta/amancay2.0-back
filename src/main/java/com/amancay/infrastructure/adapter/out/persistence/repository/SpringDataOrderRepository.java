package com.amancay.infrastructure.adapter.out.persistence.repository;

import com.amancay.infrastructure.adapter.out.persistence.entity.OrderJpaEntity;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataOrderRepository extends JpaRepository<OrderJpaEntity, UUID> {
    List<OrderJpaEntity> findByUserId(UUID userId);

    List<OrderJpaEntity> findAllByOrderByCreatedAtDesc();
}
