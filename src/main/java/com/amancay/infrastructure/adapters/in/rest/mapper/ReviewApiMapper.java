package com.amancay.infrastructure.adapters.in.rest.mapper;

import com.amancay.domain.model.ProductReview;
import com.amancay.dto.ReviewDto;

public final class ReviewApiMapper {
    private ReviewApiMapper() {
    }

    public static ReviewDto toDto(ProductReview review) {
        return new ReviewDto(review.id(), review.productId(), review.userId(), review.authorName(), review.rating(),
                review.title(), review.comment(), com.amancay.entity.ReviewStatus.valueOf(review.visibility().name()),
                review.createdAt(), review.updatedAt());
    }
}