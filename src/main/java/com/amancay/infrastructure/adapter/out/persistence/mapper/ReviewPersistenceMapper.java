package com.amancay.infrastructure.adapter.out.persistence.mapper;

import org.springframework.stereotype.Component;

import com.amancay.domain.model.Review;
import com.amancay.infrastructure.adapter.out.persistence.entity.ReviewJpaEntity;

@Component
public class ReviewPersistenceMapper {

    public Review toDomain(ReviewJpaEntity entity) {
        return new Review(entity.getId(), entity.getProductId(), entity.getUserId(), entity.getRating(),
                entity.getTitle(), entity.getComment(), entity.getStatus(), entity.getCreatedAt(),
                entity.getUpdatedAt());
    }

    public void copyToEntity(Review review, ReviewJpaEntity entity) {
        entity.setId(review.getId());
        entity.setProductId(review.getProductId());
        entity.setUserId(review.getUserId());
        entity.setRating(review.getRating());
        entity.setTitle(review.getTitle());
        entity.setComment(review.getComment());
        entity.setStatus(review.getStatus());
    }
}
