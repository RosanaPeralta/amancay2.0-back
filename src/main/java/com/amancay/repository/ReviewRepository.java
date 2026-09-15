package com.amancay.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.amancay.dto.RatingCount;
import com.amancay.entity.Review;
import com.amancay.entity.ReviewStatus;

public interface ReviewRepository extends JpaRepository<Review, UUID>, JpaSpecificationExecutor<Review> {
    boolean existsByProductIdAndUserId(UUID productId, UUID userId);

    Optional<Review> findByProductIdAndUserId(UUID productId, UUID userId);

    Page<Review> findByProductIdAndStatus(UUID productId, ReviewStatus status, Pageable pageable);

    Page<Review> findByUserId(UUID userId, Pageable pageable);

    @Query("""
        SELECT new com.amancay.dto.RatingCount(r.rating, COUNT(r))
        FROM Review r
        WHERE r.productId = :productId
          AND r.status = com.amancay.entity.ReviewStatus.PUBLISHED
        GROUP BY r.rating
        """)
    List<RatingCount> countPublishedGroupedByRating(@Param("productId") UUID productId);
}
