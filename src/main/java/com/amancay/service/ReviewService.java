package com.amancay.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.amancay.dto.CreateReviewRequest;
import com.amancay.dto.PageResponse;
import com.amancay.dto.RatingCount;
import com.amancay.dto.RatingSummaryDto;
import com.amancay.dto.ReviewDto;
import com.amancay.dto.UpdateReviewRequest;
import com.amancay.entity.Review;
import com.amancay.entity.ReviewStatus;
import com.amancay.entity.User;
import com.amancay.exceptions.DuplicateReviewException;
import com.amancay.exceptions.ProductNotFoundException;
import com.amancay.exceptions.PurchaseRequiredException;
import com.amancay.exceptions.ReviewNotFoundException;
import com.amancay.repository.ProductRepository;
import com.amancay.repository.ReviewRepository;
import com.amancay.repository.ReviewSpecifications;
import com.amancay.repository.UserRepository;

@Service
public class ReviewService {
    private static final int MIN_RATING = 1;
    private static final int MAX_RATING = 5;

    private final ReviewRepository reviewRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final PurchaseVerifier purchaseVerifier;

    public ReviewService(ReviewRepository reviewRepository, ProductRepository productRepository,
            UserRepository userRepository, PurchaseVerifier purchaseVerifier) {
        this.reviewRepository = reviewRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
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
        Review review = Review.publish(productId, userId, request.rating(), request.title(), request.comment());
        return toDto(reviewRepository.saveAndFlush(review));
    }

    @Transactional
    public ReviewDto update(UUID userId, UUID reviewId, UpdateReviewRequest request) {
        Review review = findOwnedReview(userId, reviewId);
        review.edit(request.rating(), request.title(), request.comment());
        return toDto(reviewRepository.saveAndFlush(review));
    }

    @Transactional
    public void delete(UUID userId, UUID reviewId) {
        Review review = findOwnedReview(userId, reviewId);
        reviewRepository.delete(review);
    }

    @Transactional(readOnly = true)
    public PageResponse<ReviewDto> listByProduct(UUID productId, Pageable pageable) {
        requireProduct(productId);
        return toPage(reviewRepository.findByProductIdAndStatus(productId, ReviewStatus.PUBLISHED, pageable));
    }

    /** REV-09: las reseñas del usuario, incluidas las ocultadas por moderación. */
    @Transactional(readOnly = true)
    public PageResponse<ReviewDto> listByUser(UUID userId, Pageable pageable) {
        return toPage(reviewRepository.findByUserId(userId, pageable));
    }

    @Transactional(readOnly = true)
    public RatingSummaryDto ratingSummary(UUID productId) {
        requireProduct(productId);
        List<RatingCount> rows = reviewRepository.countPublishedGroupedByRating(productId);

        Map<Integer, Long> distribution = new LinkedHashMap<>();
        for (int rating = MIN_RATING; rating <= MAX_RATING; rating++) {
            distribution.put(rating, 0L);
        }
        long total = 0L;
        long weightedSum = 0L;
        for (RatingCount row : rows) {
            distribution.put(row.rating(), row.count());
            total += row.count();
            weightedSum += (long) row.rating() * row.count();
        }
        Double average = total == 0 ? null : Math.round((double) weightedSum / total * 10.0) / 10.0;
        return new RatingSummaryDto(average, total, distribution);
    }

    // --- Moderación (REV-07) ---------------------------------------------------------------

    @Transactional(readOnly = true)
    public PageResponse<ReviewDto> listForModeration(ReviewStatus status, UUID productId, Pageable pageable) {
        return toPage(reviewRepository.findAll(ReviewSpecifications.matching(status, productId), pageable));
    }

    @Transactional
    public ReviewDto changeStatus(UUID reviewId, ReviewStatus status) {
        Review review = findReview(reviewId);
        switch (status) {
            case HIDDEN -> review.hide();
            case PUBLISHED -> review.republish();
        }
        return toDto(reviewRepository.saveAndFlush(review));
    }

    /** A diferencia de {@link #delete}, no exige ser el autor: es la acción "eliminar" del panel ADMIN. */
    @Transactional
    public void deleteAsAdmin(UUID reviewId) {
        reviewRepository.delete(findReview(reviewId));
    }

    // --- Helpers ---------------------------------------------------------------------------

    private void requireProduct(UUID productId) {
        if (!productRepository.existsById(productId)) {
            throw new ProductNotFoundException(productId);
        }
    }

    private Review findReview(UUID id) {
        return reviewRepository.findById(id).orElseThrow(() -> new ReviewNotFoundException(id));
    }

    private Review findOwnedReview(UUID userId, UUID reviewId) {
        Review review = findReview(reviewId);
        if (!review.getUserId().equals(userId)) {
            throw new ReviewNotFoundException(reviewId);
        }
        return review;
    }

    private ReviewDto toDto(Review review) {
        String authorName = userRepository.findById(review.getUserId()).map(User::getName).orElse(null);
        return toDto(review, authorName);
    }

    /** Resuelve los nombres de autor de toda la página con una sola consulta. */
    private PageResponse<ReviewDto> toPage(Page<Review> reviews) {
        Set<UUID> userIds = reviews.getContent().stream().map(Review::getUserId).collect(Collectors.toSet());
        Map<UUID, String> namesById = userRepository.findAllById(userIds).stream()
                .filter(user -> user.getName() != null)
                .collect(Collectors.toMap(User::getId, User::getName, (a, b) -> a));
        List<ReviewDto> content = reviews.getContent().stream()
                .map(review -> toDto(review, namesById.get(review.getUserId())))
                .toList();
        return new PageResponse<>(content, reviews.getNumber(), reviews.getSize(),
                reviews.getTotalElements(), reviews.getTotalPages());
    }

    private ReviewDto toDto(Review review, String authorName) {
        return new ReviewDto(review.getId(), review.getProductId(), review.getUserId(), authorName,
                review.getRating(), review.getTitle(), review.getComment(), review.getStatus(),
                review.getCreatedAt(), review.getUpdatedAt());
    }
}
