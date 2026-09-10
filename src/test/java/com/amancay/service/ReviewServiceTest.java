package com.amancay.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.amancay.dto.CreateReviewRequest;
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

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {
    private static final UUID USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID OTHER_USER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID PRODUCT_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID REVIEW_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private PurchaseVerifier purchaseVerifier;

    private ReviewService reviewService;

    @BeforeEach
    void setUp() {
        reviewService = new ReviewService(reviewRepository, productRepository, purchaseVerifier);
    }

    @Test
    void createsReviewWhenProductExistsAndPurchaseIsVerified() {
        CreateReviewRequest request = new CreateReviewRequest(5, "Great", "Loved it");
        when(productRepository.existsById(PRODUCT_ID)).thenReturn(true);
        when(purchaseVerifier.hasPurchased(USER_ID, PRODUCT_ID)).thenReturn(true);
        when(reviewRepository.existsByProductIdAndUserId(PRODUCT_ID, USER_ID)).thenReturn(false);
        when(reviewRepository.save(any(Review.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ReviewDto result = reviewService.create(USER_ID, PRODUCT_ID, request);

        assertThat(result.id()).isNotNull();
        assertThat(result.productId()).isEqualTo(PRODUCT_ID);
        assertThat(result.userId()).isEqualTo(USER_ID);
        assertThat(result.rating()).isEqualTo(5);
        assertThat(result.title()).isEqualTo("Great");
        assertThat(result.comment()).isEqualTo("Loved it");
        assertThat(result.status()).isEqualTo(ReviewStatus.PUBLISHED);
    }

    @Test
    void rejectsCreateWhenProductDoesNotExist() {
        when(productRepository.existsById(PRODUCT_ID)).thenReturn(false);

        assertThatThrownBy(() -> reviewService.create(USER_ID, PRODUCT_ID, new CreateReviewRequest(4, null, null)))
                .isInstanceOf(ProductNotFoundException.class);
        verify(reviewRepository, never()).save(any(Review.class));
    }

    @Test
    void rejectsCreateWhenUserDidNotPurchase() {
        when(productRepository.existsById(PRODUCT_ID)).thenReturn(true);
        when(purchaseVerifier.hasPurchased(USER_ID, PRODUCT_ID)).thenReturn(false);

        assertThatThrownBy(() -> reviewService.create(USER_ID, PRODUCT_ID, new CreateReviewRequest(4, null, null)))
                .isInstanceOf(PurchaseRequiredException.class);
        verify(reviewRepository, never()).save(any(Review.class));
    }

    @Test
    void rejectsSecondReviewForSameProduct() {
        when(productRepository.existsById(PRODUCT_ID)).thenReturn(true);
        when(purchaseVerifier.hasPurchased(USER_ID, PRODUCT_ID)).thenReturn(true);
        when(reviewRepository.existsByProductIdAndUserId(PRODUCT_ID, USER_ID)).thenReturn(true);
        when(reviewRepository.findByProductIdAndUserId(PRODUCT_ID, USER_ID))
                .thenReturn(Optional.of(review(REVIEW_ID, USER_ID, 3)));

        assertThatThrownBy(() -> reviewService.create(USER_ID, PRODUCT_ID, new CreateReviewRequest(4, null, null)))
                .isInstanceOf(DuplicateReviewException.class)
                .hasMessageContaining(REVIEW_ID.toString());
        verify(reviewRepository, never()).save(any(Review.class));
    }

    @Test
    void updatesOwnReview() {
        Review existing = review(REVIEW_ID, USER_ID, 3);
        when(reviewRepository.findById(REVIEW_ID)).thenReturn(Optional.of(existing));
        when(reviewRepository.save(any(Review.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ReviewDto result = reviewService.update(USER_ID, REVIEW_ID, new UpdateReviewRequest(1, "Meh", "Not for me"));

        assertThat(result.rating()).isEqualTo(1);
        assertThat(result.title()).isEqualTo("Meh");
        assertThat(result.comment()).isEqualTo("Not for me");
    }

    @Test
    void rejectsUpdateFromNonAuthorAsNotFound() {
        when(reviewRepository.findById(REVIEW_ID)).thenReturn(Optional.of(review(REVIEW_ID, OTHER_USER_ID, 3)));

        assertThatThrownBy(() -> reviewService.update(USER_ID, REVIEW_ID, new UpdateReviewRequest(2, null, null)))
                .isInstanceOf(ReviewNotFoundException.class);
        verify(reviewRepository, never()).save(any(Review.class));
    }

    @Test
    void deletesOwnReview() {
        Review existing = review(REVIEW_ID, USER_ID, 3);
        when(reviewRepository.findById(REVIEW_ID)).thenReturn(Optional.of(existing));

        reviewService.delete(USER_ID, REVIEW_ID);

        verify(reviewRepository).delete(existing);
    }

    @Test
    void rejectsDeleteFromNonAuthorAsNotFound() {
        when(reviewRepository.findById(REVIEW_ID)).thenReturn(Optional.of(review(REVIEW_ID, OTHER_USER_ID, 3)));

        assertThatThrownBy(() -> reviewService.delete(USER_ID, REVIEW_ID))
                .isInstanceOf(ReviewNotFoundException.class);
        verify(reviewRepository, never()).delete(any(Review.class));
    }

    @Test
    void rejectsUpdateOfUnknownReview() {
        when(reviewRepository.findById(REVIEW_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reviewService.update(USER_ID, REVIEW_ID, new UpdateReviewRequest(2, null, null)))
                .isInstanceOf(ReviewNotFoundException.class);
    }

    @Test
    void returnsEmptySummaryWhenProductHasNoReviews() {
        when(productRepository.existsById(PRODUCT_ID)).thenReturn(true);
        when(reviewRepository.countPublishedGroupedByRating(PRODUCT_ID)).thenReturn(List.of());

        RatingSummaryDto result = reviewService.ratingSummary(PRODUCT_ID);

        assertThat(result.average()).isNull();
        assertThat(result.total()).isZero();
        assertThat(result.distribution()).containsExactlyInAnyOrderEntriesOf(
                java.util.Map.of(1, 0L, 2, 0L, 3, 0L, 4, 0L, 5, 0L));
    }

    @Test
    void computesAverageAndFullDistribution() {
        when(productRepository.existsById(PRODUCT_ID)).thenReturn(true);
        when(reviewRepository.countPublishedGroupedByRating(PRODUCT_ID)).thenReturn(List.of(
                new Object[] { 5, 3L },
                new Object[] { 4, 1L },
                new Object[] { 1, 1L }));

        RatingSummaryDto result = reviewService.ratingSummary(PRODUCT_ID);

        // (5*3 + 4 + 1) / 5 = 4.0
        assertThat(result.average()).isEqualTo(4.0);
        assertThat(result.total()).isEqualTo(5L);
        assertThat(result.distribution()).containsExactlyInAnyOrderEntriesOf(
                java.util.Map.of(1, 1L, 2, 0L, 3, 0L, 4, 1L, 5, 3L));
    }

    @Test
    void roundsAverageToOneDecimal() {
        when(productRepository.existsById(PRODUCT_ID)).thenReturn(true);
        when(reviewRepository.countPublishedGroupedByRating(PRODUCT_ID)).thenReturn(List.of(
                new Object[] { 5, 1L },
                new Object[] { 4, 1L },
                new Object[] { 3, 1L }));

        RatingSummaryDto result = reviewService.ratingSummary(PRODUCT_ID);

        assertThat(result.average()).isEqualTo(4.0);
    }

    @Test
    void rejectsSummaryForUnknownProduct() {
        when(productRepository.existsById(PRODUCT_ID)).thenReturn(false);

        assertThatThrownBy(() -> reviewService.ratingSummary(PRODUCT_ID))
                .isInstanceOf(ProductNotFoundException.class);
    }

    @Test
    void listsOnlyPublishedReviewsOfAProduct() {
        Review published = review(REVIEW_ID, USER_ID, 5);
        when(productRepository.existsById(PRODUCT_ID)).thenReturn(true);
        when(reviewRepository.findByProductIdAndStatus(
                org.mockito.ArgumentMatchers.eq(PRODUCT_ID),
                org.mockito.ArgumentMatchers.eq(ReviewStatus.PUBLISHED),
                any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(
                        List.of(published),
                        org.springframework.data.domain.PageRequest.of(0, 20),
                        1));

        com.amancay.dto.PageResponse<ReviewDto> result = reviewService.listByProduct(PRODUCT_ID,
                org.springframework.data.domain.PageRequest.of(0, 20));

        assertThat(result.content()).hasSize(1);
        assertThat(result.content().getFirst().id()).isEqualTo(REVIEW_ID);
        assertThat(result.page()).isZero();
        assertThat(result.size()).isEqualTo(20);
        assertThat(result.totalElements()).isEqualTo(1L);
        assertThat(result.totalPages()).isEqualTo(1);
    }

    private Review review(UUID id, UUID userId, int rating) {
        Review review = new Review();
        review.setId(id);
        review.setProductId(PRODUCT_ID);
        review.setUserId(userId);
        review.setRating(rating);
        review.setStatus(ReviewStatus.PUBLISHED);
        return review;
    }
}
