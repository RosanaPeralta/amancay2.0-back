package com.amancay.infrastructure.adapters.out.persistence.mapper;

import com.amancay.domain.model.ProductReview;
import com.amancay.domain.model.ReviewVisibility;

public final class ReviewPersistenceMapper {
    private ReviewPersistenceMapper() {
    }

    public static ProductReview toDomain(com.amancay.entity.Review entity) {
        return new ProductReview(entity.getId(), entity.getProductId(), entity.getUserId(), null, entity.getRating(),
                entity.getTitle(), entity.getComment(), ReviewVisibility.valueOf(entity.getStatus().name()),
                entity.getCreatedAt(), entity.getUpdatedAt());
    }

    public static com.amancay.entity.Review toPersistence(ProductReview review,
            com.amancay.entity.Review entity) {
        if (entity == null) {
            entity = com.amancay.entity.Review.create(review.productId(), review.userId(), review.rating(),
                    review.title(), review.comment());
        } else {
            entity.update(review.rating(), review.title(), review.comment());
        }
        if (review.visibility() == ReviewVisibility.HIDDEN) {
            entity.hide();
        } else {
            entity.republish();
        }
        return entity;
    }
}