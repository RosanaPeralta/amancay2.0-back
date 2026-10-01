package com.amancay.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import lombok.Getter;

@Getter
public class Review {

    public static final int MIN_RATING = 1;
    public static final int MAX_RATING = 5;
    private static final int MAX_TITLE_LENGTH = 150;

    private final UUID id;
    private final UUID productId;
    private final UUID userId;
    private int rating;
    private String title;
    private String comment;
    private ReviewStatus status;
    private final Instant createdAt;
    private final Instant updatedAt;

    // Reconstruye una resena ya existente (lo usa el adaptador de persistencia).
    public Review(UUID id, UUID productId, UUID userId, int rating, String title, String comment,
            ReviewStatus status, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.productId = productId;
        this.userId = userId;
        this.rating = rating;
        this.title = title;
        this.comment = comment;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Review create(UUID productId, UUID userId, int rating, String title, String comment) {
        Review review = new Review(UUID.randomUUID(), Objects.requireNonNull(productId, "productId is required"),
                Objects.requireNonNull(userId, "userId is required"), MIN_RATING, null, comment,
                ReviewStatus.PUBLISHED, null, null);
        review.setRating(rating);
        review.setTitle(title);
        return review;
    }

    public boolean isWrittenBy(UUID userId) {
        return this.userId.equals(userId);
    }

    public void update(int rating, String title, String comment) {
        setRating(rating);
        setTitle(title);
        this.comment = comment;
    }

    public void hide() {
        this.status = ReviewStatus.HIDDEN;
    }

    public void republish() {
        this.status = ReviewStatus.PUBLISHED;
    }

    public void changeStatus(ReviewStatus status) {
        switch (status) {
            case HIDDEN -> hide();
            case PUBLISHED -> republish();
        }
    }

    private void setRating(int rating) {
        if (rating < MIN_RATING || rating > MAX_RATING) {
            throw new IllegalArgumentException(
                    "rating must be between " + MIN_RATING + " and " + MAX_RATING + ", but was " + rating);
        }
        this.rating = rating;
    }

    private void setTitle(String title) {
        if (title != null && title.length() > MAX_TITLE_LENGTH) {
            throw new IllegalArgumentException(
                    "title must be at most " + MAX_TITLE_LENGTH + " characters, but was " + title.length());
        }
        this.title = title;
    }
}
