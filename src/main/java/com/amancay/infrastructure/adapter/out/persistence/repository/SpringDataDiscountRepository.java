package com.amancay.infrastructure.adapter.out.persistence.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.amancay.infrastructure.adapter.out.persistence.entity.DiscountJpaEntity;

public interface SpringDataDiscountRepository extends JpaRepository<DiscountJpaEntity, Long> {
    List<DiscountJpaEntity> findByDescription(String description);
}
