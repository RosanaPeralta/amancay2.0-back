package com.amancay.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.amancay.entity.Category;

public interface CategoryRepository extends JpaRepository<Category, UUID> {

    boolean existsByName(String name);
    boolean existsByNameAndIdNot(String name, UUID id);
}