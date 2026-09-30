package com.amancay.repository;

import java.util.UUID;

import org.springframework.data.jpa.domain.Specification;

import com.amancay.entity.Review;
import com.amancay.entity.ReviewStatus;

public final class ReviewSpecifications {
    private ReviewSpecifications() {
    }

    public static Specification<Review> matching(ReviewStatus status, UUID productId) {
        return com.amancay.infrastructure.adapters.out.persistence.repository.ReviewSpecifications
                .matching(status, productId);
    }
}