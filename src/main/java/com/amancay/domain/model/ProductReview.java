package com.amancay.domain.model;

import java.time.Instant;
import java.util.UUID;

public record ProductReview(UUID id, UUID productId, UUID userId, String authorName, int rating, String title,
        String comment, ReviewVisibility visibility, Instant createdAt, Instant updatedAt) {
    private static final int MAX_TITLE_LENGTH = 150;

    public static ProductReview create(UUID productId, UUID userId, int rating, String title, String comment) {
        validate(rating, title);
        return new ProductReview(null, productId, userId, null, rating, title, comment, ReviewVisibility.PUBLISHED,
                null, null);
    }

    public ProductReview update(int newRating, String newTitle, String newComment) {
        validate(newRating, newTitle);
        return new ProductReview(id, productId, userId, authorName, newRating, newTitle, newComment, visibility,
                createdAt, updatedAt);
    }

    public ProductReview hide() {
        return withVisibility(ReviewVisibility.HIDDEN);
    }

    public ProductReview republish() {
        return withVisibility(ReviewVisibility.PUBLISHED);
    }

    private ProductReview withVisibility(ReviewVisibility newVisibility) {
        return new ProductReview(id, productId, userId, authorName, rating, title, comment, newVisibility,
                createdAt, updatedAt);
    }

    private static void validate(int rating, String title) {
        if (rating < 1 || rating > 5) {
            throw new IllegalArgumentException("rating must be between 1 and 5, but was " + rating);
        }
        if (title != null && title.length() > MAX_TITLE_LENGTH) {
            throw new IllegalArgumentException("title must be at most 150 characters, but was " + title.length());
        }
    }
}