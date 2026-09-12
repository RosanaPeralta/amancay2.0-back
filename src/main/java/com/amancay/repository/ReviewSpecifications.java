package com.amancay.repository;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.domain.Specification;

import com.amancay.entity.Review;
import com.amancay.entity.ReviewStatus;

import jakarta.persistence.criteria.Predicate;

/**
 * Filtros opcionales del listado de moderación (REV-07). Cada filtro en {@code null} se omite,
 * así que agregar un tercero es una línea más y no el doble de ramas.
 */
public final class ReviewSpecifications {

    private ReviewSpecifications() {
    }

    public static Specification<Review> matching(ReviewStatus status, UUID productId) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (productId != null) {
                predicates.add(cb.equal(root.get("productId"), productId));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }
}
