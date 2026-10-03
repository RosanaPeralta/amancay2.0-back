package com.amancay.infrastructure.adapter.out.persistence.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.amancay.infrastructure.adapter.out.persistence.entity.CategoryJpaEntity;

public interface SpringDataCategoryRepository extends JpaRepository<CategoryJpaEntity, UUID> {
    boolean existsByName(String name);

    boolean existsByNameAndIdNot(String name, UUID id);
}
