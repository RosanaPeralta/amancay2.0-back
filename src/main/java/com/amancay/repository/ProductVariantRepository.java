package com.amancay.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.amancay.entity.ProductVariant;

public interface ProductVariantRepository extends JpaRepository<ProductVariant, UUID> {
}
