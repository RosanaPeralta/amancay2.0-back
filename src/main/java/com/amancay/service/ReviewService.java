package com.amancay.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.amancay.dto.CreateReviewRequest;
import com.amancay.dto.PageResponse;
import com.amancay.dto.RatingSummaryDto;
import com.amancay.dto.ReviewDto;
import com.amancay.dto.UpdateReviewRequest;
import com.amancay.entity.Review;
import com.amancay.entity.ReviewStatus;
import com.amancay.exceptions.DuplicateReviewException;
import com.amancay.exceptions.ProductNotFoundException;
import com.amancay.exceptions.PurchaseRequiredException;
import com.amancay.exceptions.ReviewNotFoundException;
import com.amancay.repository.ProductRepository;
import com.amancay.repository.ReviewRepository;

@Service
public class ReviewService {
    private static final int MIN_RATING = 1;
    private static final int MAX_RATING = 5;

    private final ReviewRepository reviewRepository;
    private final ProductRepository productRepository;
    private final PurchaseVerifier purchaseVerifier;

    public ReviewService(ReviewRepository reviewRepository, ProductRepository productRepository,
            PurchaseVerifier purchaseVerifier) {
        this.reviewRepository = reviewRepository;
        this.productRepository = productRepository;
        this.purchaseVerifier = purchaseVerifier;
    }

    @Transactional
    public ReviewDto create(UUID userId, UUID productId, CreateReviewRequest request) {
        requireProduct(productId);
        if (!purchaseVerifier.hasPurchased(userId, productId)) {
            throw new PurchaseRequiredException(productId);
        }
        if (reviewRepository.existsByProductIdAndUserId(productId, userId)) {
            UUID existingId = reviewRepository.findByProductIdAndUserId(productId, userId)
                    .map(Review::getId)
                    .orElse(null);
            throw new DuplicateReviewException(productId, existingId);
        }
        Review review = new Review();
        review.setId(UUID.randomUUID());
        review.setProductId(productId);
        review.setUserId(userId);
        review.setStatus(ReviewStatus.PUBLISHED);
        applyReviewFields(review, request.rating(), request.title(), request.comment());
        return toDto(reviewRepository.save(review));
    }

    @Transactional
    public ReviewDto update(UUID userId, UUID reviewId, UpdateReviewRequest request) {
        Review review = findOwnedReview(userId, reviewId);
        applyReviewFields(review, request.rating(), request.title(), request.comment());
        return toDto(reviewRepository.save(review));
    }

    @Transactional
    public void delete(UUID userId, UUID reviewId) {
        Review review = findOwnedReview(userId, reviewId);
        reviewRepository.delete(review);
    }

    @Transactional(readOnly = true)
    public PageResponse<ReviewDto> listByProduct(UUID productId, Pageable pageable) {
        requireProduct(productId);
        Page<Review> reviews = reviewRepository.findByProductIdAndStatus(productId, ReviewStatus.PUBLISHED, pageable);
        return new PageResponse<>(reviews.map(this::toDto).getContent(), reviews.getNumber(), reviews.getSize(),
                reviews.getTotalElements(), reviews.getTotalPages());
    }

    @Transactional(readOnly = true)
    public RatingSummaryDto ratingSummary(UUID productId) {
        requireProduct(productId);
        List<Object[]> rows = reviewRepository.countPublishedGroupedByRating(productId);

        Map<Integer, Long> distribution = new LinkedHashMap<>();
        for (int rating = MIN_RATING; rating <= MAX_RATING; rating++) {
            distribution.put(rating, 0L);
        }
        long total = 0L;
        long weightedSum = 0L;
        for (Object[] row : rows) {
            int rating = ((Number) row[0]).intValue();
            long count = ((Number) row[1]).longValue();
            distribution.put(rating, count);
            total += count;
            weightedSum += (long) rating * count;
        }
        Double average = total == 0 ? null : Math.round((double) weightedSum / total * 10.0) / 10.0;
        return new RatingSummaryDto(average, total, distribution);
    }

    private void requireProduct(UUID productId) {
        if (!productRepository.existsById(productId)) {
            throw new ProductNotFoundException(productId);
        }
    }

    private Review findReview(UUID id) {
        return reviewRepository.findById(id).orElseThrow(() -> new ReviewNotFoundException(id));
    }

    /**
     * A review owned by another user is reported as missing on purpose, so the
     * API never leaks the existence of somebody else's review.
     */
    private Review findOwnedReview(UUID userId, UUID reviewId) {
        Review review = findReview(reviewId);
        if (!review.getUserId().equals(userId)) {
            throw new ReviewNotFoundException(reviewId);
        }
        return review;
    }

    private void applyReviewFields(Review review, Integer rating, String title, String comment) {
        review.setRating(rating);
        review.setTitle(title);
        review.setComment(comment);
    }

    private ReviewDto toDto(Review review) {
        return new ReviewDto(review.getId(), review.getProductId(), review.getUserId(), review.getRating(),
                review.getTitle(), review.getComment(), review.getStatus(), review.getCreatedAt(),
                review.getUpdatedAt());
    }
}
