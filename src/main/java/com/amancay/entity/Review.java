package com.amancay.entity;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import lombok.Getter;
import lombok.Setter;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "reviews")
@Getter
public class Review {

    private static final int MIN_RATING = 1;
    private static final int MAX_RATING = 5;
    private static final int MAX_TITLE_LENGTH = 150;

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "product_id", nullable = false, updatable = false)
    private UUID productId;

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @JdbcTypeCode(SqlTypes.SMALLINT)
    @Column(nullable = false)
    private int rating;

    @Column(length = MAX_TITLE_LENGTH)
    private String title;

    @Setter
    @Column(name = "comment", columnDefinition = "TEXT")
    private String comment;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReviewStatus status = ReviewStatus.PUBLISHED;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;


    protected Review() {
    }

    
    public static Review publish(UUID productId, UUID userId, int rating, String title, String comment) {
        Review review = new Review();
        review.id = UUID.randomUUID();
        review.productId = Objects.requireNonNull(productId, "productId is required");
        review.userId = Objects.requireNonNull(userId, "userId is required");
        review.status = ReviewStatus.PUBLISHED;
        review.setRating(rating);
        review.setTitle(title);
        review.comment = comment;
        return review;
    }


    public void edit(int rating, String title, String comment) {
        setRating(rating);
        setTitle(title);
        this.comment = comment;
    }

    /** Moderación (REV-07): la reseña deja de aparecer en el listado público y en el promedio. */
    public void hide() {
        this.status = ReviewStatus.HIDDEN;
    }

    /** Moderación (REV-07): revierte {@link #hide()}. */
    public void republish() {
        this.status = ReviewStatus.PUBLISHED;
    }


    public void setRating(int rating) {
        if (rating < MIN_RATING || rating > MAX_RATING) {
            throw new IllegalArgumentException(
                    "rating must be between " + MIN_RATING + " and " + MAX_RATING + ", but was " + rating);
        }
        this.rating = rating;
    }


    public void setTitle(String title) {
        if (title != null && title.length() > MAX_TITLE_LENGTH) {
            throw new IllegalArgumentException(
                    "title must be at most " + MAX_TITLE_LENGTH + " characters, but was " + title.length());
        }
        this.title = title;
    }
}
