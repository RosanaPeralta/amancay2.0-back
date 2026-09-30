package com.amancay.infrastructure.adapters.out.persistence.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;

import com.amancay.entity.Category;

@NoRepositoryBean
public interface CategoryRepository extends JpaRepository<Category, UUID> {

    boolean existsByName(String name);
    boolean existsByNameAndIdNot(String name, UUID id);
}