package com.amancay.infrastructure.adapter.out.persistence.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.amancay.domain.model.ReturnRequestStatus;
import com.amancay.infrastructure.adapter.out.persistence.entity.ReturnRequestJpaEntity;

public interface SpringDataReturnRequestRepository
        extends JpaRepository<ReturnRequestJpaEntity, UUID> {

    Optional<ReturnRequestJpaEntity> findByOrderId(UUID orderId);

    List<ReturnRequestJpaEntity> findByStatus(ReturnRequestStatus status);
}