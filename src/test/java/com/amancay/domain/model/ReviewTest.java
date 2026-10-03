package com.amancay.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;

import org.junit.jupiter.api.Test;

class ReviewTest {

    private static final UUID PRODUCT_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @Test
    void publishCreatesACompleteReview() {
        Review review = Review.create(PRODUCT_ID, USER_ID, 5, "Excelente", "La recompro");

        assertThat(review.getId()).isNotNull();
        assertThat(review.getProductId()).isEqualTo(PRODUCT_ID);
        assertThat(review.getUserId()).isEqualTo(USER_ID);
        assertThat(review.getRating()).isEqualTo(5);
        assertThat(review.getTitle()).isEqualTo("Excelente");
        assertThat(review.getComment()).isEqualTo("La recompro");
        assertThat(review.getStatus()).isEqualTo(ReviewStatus.PUBLISHED);
    }

    @Test
    void publishAssignsADifferentIdToEachReview() {
        Review first = Review.create(PRODUCT_ID, USER_ID, 4, null, null);
        Review second = Review.create(PRODUCT_ID, USER_ID, 4, null, null);

        assertThat(first.getId()).isNotEqualTo(second.getId());
    }

    @Test
    void publishAcceptsAReviewWithoutTitleOrComment() {
        Review review = Review.create(PRODUCT_ID, USER_ID, 3, null, null);

        assertThat(review.getTitle()).isNull();
        assertThat(review.getComment()).isNull();
        assertThat(review.getStatus()).isEqualTo(ReviewStatus.PUBLISHED);
    }

    @Test
    void publishRejectsARatingBelowTheAllowedRange() {
        assertThatThrownBy(() -> Review.create(PRODUCT_ID, USER_ID, 0, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("rating");
    }

    @Test
    void publishRejectsARatingAboveTheAllowedRange() {
        assertThatThrownBy(() -> Review.create(PRODUCT_ID, USER_ID, 6, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("rating");
    }

    @Test
    void publishRejectsATitleLongerThanTheColumn() {
        String tooLong = "x".repeat(151);

        assertThatThrownBy(() -> Review.create(PRODUCT_ID, USER_ID, 4, tooLong, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("title");
    }

    @Test
    void publishRejectsAMissingProduct() {
        assertThatThrownBy(() -> Review.create(null, USER_ID, 4, null, null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void publishRejectsAMissingUser() {
        assertThatThrownBy(() -> Review.create(PRODUCT_ID, null, 4, null, null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void hideAndRepublishToggleTheStatusWithoutTouchingTheRest() {
        Review review = Review.create(PRODUCT_ID, USER_ID, 4, "Buena", null);

        review.hide();
        assertThat(review.getStatus()).isEqualTo(ReviewStatus.HIDDEN);
        assertThat(review.getRating()).isEqualTo(4);
        assertThat(review.getTitle()).isEqualTo("Buena");

        review.republish();
        assertThat(review.getStatus()).isEqualTo(ReviewStatus.PUBLISHED);
    }
}
