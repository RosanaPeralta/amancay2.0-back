package com.amancay.repository;

import java.util.UUID;

import org.springframework.data.jpa.domain.Specification;

import com.amancay.entity.Product;

public final class ProductSpecifications {
    private ProductSpecifications() {
    }

    public static Specification<Product> matching(String name, UUID categoryId, Boolean active) {
        return com.amancay.infrastructure.adapters.out.persistence.repository.ProductSpecifications
                .matching(name, categoryId, active);
    }
}