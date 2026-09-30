package com.amancay.infrastructure.adapters.out.persistence.repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.springframework.data.jpa.domain.Specification;

import com.amancay.entity.Product;

import jakarta.persistence.criteria.Predicate;

/**
 * Filtros opcionales del listado de productos. Cada filtro en {@code null} se omite.
 */
public final class ProductSpecifications {

    private ProductSpecifications() {
    }

    public static Specification<Product> matching(String name, UUID categoryId, Boolean active) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (name != null) {
                predicates.add(cb.like(cb.lower(root.get("name")), "%" + escapeLike(name.toLowerCase(Locale.ROOT)) + "%", '\\'));
            }
            if (categoryId != null) {
                predicates.add(cb.isMember(categoryId, root.get("categoryIds")));
            }
            if (active != null) {
                predicates.add(cb.equal(root.get("active"), active));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    private static String escapeLike(String value) {
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
