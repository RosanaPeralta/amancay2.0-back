package com.amancay.application.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import com.amancay.domain.model.ProductReview;
import com.amancay.domain.model.ReviewPage;
import com.amancay.domain.model.ReviewRatingSummary;
import com.amancay.domain.model.ReviewVisibility;
import com.amancay.domain.ports.in.ReviewUseCases;
import com.amancay.domain.ports.out.ReviewCatalogPort;
import com.amancay.domain.ports.out.ReviewPurchasePort;
import com.amancay.exceptions.DuplicateReviewException;
import com.amancay.exceptions.ProductNotFoundException;
import com.amancay.exceptions.PurchaseRequiredException;
import com.amancay.exceptions.ReviewNotFoundException;

public class ReviewApplicationService implements ReviewUseCases {
    private static final int MIN_RATING = 1;
    private static final int MAX_RATING = 5;

    private final ReviewCatalogPort reviewCatalog;
    private final ReviewPurchasePort purchasePort;

    public ReviewApplicationService(ReviewCatalogPort reviewCatalog, ReviewPurchasePort purchasePort) {
        this.reviewCatalog = reviewCatalog;
        this.purchasePort = purchasePort;
    }

    @Override
    public ProductReview create(UUID userId, UUID productId, ReviewInput input) {
        requireProduct(productId);
        if (!purchasePort.hasPurchased(userId, productId)) {
            throw new PurchaseRequiredException(productId);
        }
        if (reviewCatalog.existsByProductAndUser(productId, userId)) {
            throw new DuplicateReviewException(productId, reviewCatalog.findIdByProductAndUser(productId, userId).orElse(null));
        }
        return withAuthorName(reviewCatalog.save(ProductReview.create(productId, userId, input.rating(), input.title(),
                input.comment())));
    }

    @Override
    public ProductReview update(UUID userId, UUID reviewId, ReviewInput input) {
        ProductReview review = findOwnedReview(userId, reviewId);
        return withAuthorName(reviewCatalog.save(review.update(input.rating(), input.title(), input.comment())));
    }

    @Override
    public void delete(UUID userId, UUID reviewId) {
        reviewCatalog.delete(findOwnedReview(userId, reviewId));
    }

    @Override
    public ReviewPage listByProduct(UUID productId, int page, int size, List<SortOrder> sort) {
        requireProduct(productId);
        return addAuthorNames(reviewCatalog.findByProductAndVisibility(productId, ReviewVisibility.PUBLISHED, page, size, sort));
    }

    @Override
    public ReviewPage listByUser(UUID userId, int page, int size, List<SortOrder> sort) {
        return addAuthorNames(reviewCatalog.findByUser(userId, page, size, sort));
    }

    @Override
    public ReviewRatingSummary ratingSummary(UUID productId) {
        requireProduct(productId);
        Map<Integer, Long> distribution = new LinkedHashMap<>();
        for (int rating = MIN_RATING; rating <= MAX_RATING; rating++) {
            distribution.put(rating, 0L);
        }
        long total = 0;
        long weightedSum = 0;
        for (Map.Entry<Integer, Long> row : reviewCatalog.countPublishedByRating(productId).entrySet()) {
            distribution.put(row.getKey(), row.getValue());
            total += row.getValue();
            weightedSum += (long) row.getKey() * row.getValue();
        }
        Double average = total == 0 ? null : Math.round((double) weightedSum / total * 10.0) / 10.0;
        return new ReviewRatingSummary(average, total, distribution);
    }

    @Override
    public ReviewPage listForModeration(ReviewVisibility visibility, UUID productId, int page, int size,
            List<SortOrder> sort) {
        return addAuthorNames(reviewCatalog.search(visibility, productId, page, size, sort));
    }

    @Override
    public ProductReview changeStatus(UUID reviewId, ReviewVisibility visibility) {
        ProductReview review = findReview(reviewId);
        ProductReview changed = visibility == ReviewVisibility.HIDDEN ? review.hide() : review.republish();
        return withAuthorName(reviewCatalog.save(changed));
    }

    @Override
    public void deleteAsAdmin(UUID reviewId) {
        reviewCatalog.delete(findReview(reviewId));
    }

    private void requireProduct(UUID productId) {
        if (!reviewCatalog.productExists(productId)) {
            throw new ProductNotFoundException(productId);
        }
    }

    private ProductReview findReview(UUID id) {
        return reviewCatalog.findById(id).orElseThrow(() -> new ReviewNotFoundException(id));
    }

    private ProductReview findOwnedReview(UUID userId, UUID reviewId) {
        ProductReview review = findReview(reviewId);
        if (!review.userId().equals(userId)) {
            throw new ReviewNotFoundException(reviewId);
        }
        return review;
    }

    private ProductReview withAuthorName(ProductReview review) {
        String name = reviewCatalog.findAuthorName(review.userId()).orElse(null);
        return withAuthorName(review, name);
    }

    private ReviewPage addAuthorNames(ReviewPage page) {
        Set<UUID> userIds = page.content().stream().map(ProductReview::userId).collect(Collectors.toSet());
        Map<UUID, String> names = reviewCatalog.findAuthorNames(userIds);
        List<ProductReview> content = page.content().stream()
                .map(review -> withAuthorName(review, names.get(review.userId()))).toList();
        return new ReviewPage(content, page.page(), page.size(), page.totalElements(), page.totalPages());
    }

    private ProductReview withAuthorName(ProductReview review, String authorName) {
        return new ProductReview(review.id(), review.productId(), review.userId(), authorName, review.rating(),
                review.title(), review.comment(), review.visibility(), review.createdAt(), review.updatedAt());
    }
}