package com.amancay.infrastructure.adapter.out.persistence.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.amancay.domain.model.RatingCount;
import com.amancay.domain.model.ReviewStatus;
import com.amancay.infrastructure.adapter.out.persistence.entity.ReviewJpaEntity;

public interface SpringDataReviewRepository
        extends JpaRepository<ReviewJpaEntity, UUID>, JpaSpecificationExecutor<ReviewJpaEntity> {
    boolean existsByProductIdAndUserId(UUID productId, UUID userId);

    Optional<ReviewJpaEntity> findByProductIdAndUserId(UUID productId, UUID userId);

    Page<ReviewJpaEntity> findByProductIdAndStatus(UUID productId, ReviewStatus status, Pageable pageable);

    Page<ReviewJpaEntity> findByUserId(UUID userId, Pageable pageable);

    @Query("""
        SELECT new com.amancay.domain.model.RatingCount(r.rating, COUNT(r))
        FROM ReviewJpaEntity r
        WHERE r.productId = :productId
          AND r.status = com.amancay.domain.model.ReviewStatus.PUBLISHED
        GROUP BY r.rating
        """)
    List<RatingCount> countPublishedGroupedByRating(@Param("productId") UUID productId);
}
