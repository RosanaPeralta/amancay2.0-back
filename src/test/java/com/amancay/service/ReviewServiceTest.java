package com.amancay.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

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
import com.amancay.repository.UserRepository;

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
    private UserRepository userRepository;

    @Mock
    private PurchaseVerifier purchaseVerifier;

    private ReviewService reviewService;

    @BeforeEach
    void setUp() {
        reviewService = new ReviewService(reviewRepository, productRepository, userRepository, purchaseVerifier);
    }

    @Test
    void createsReviewWhenProductExistsAndPurchaseIsVerified() {
        CreateReviewRequest request = new CreateReviewRequest(5, "Great", "Loved it");
        when(productRepository.existsById(PRODUCT_ID)).thenReturn(true);
        when(purchaseVerifier.hasPurchased(USER_ID, PRODUCT_ID)).thenReturn(true);
        when(reviewRepository.existsByProductIdAndUserId(PRODUCT_ID, USER_ID)).thenReturn(false);
        when(reviewRepository.saveAndFlush(any(Review.class))).thenAnswer(invocation -> invocation.getArgument(0));

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
    void exposesAuthorNameButNeverEmail() {
        when(productRepository.existsById(PRODUCT_ID)).thenReturn(true);
        when(purchaseVerifier.hasPurchased(USER_ID, PRODUCT_ID)).thenReturn(true);
        when(reviewRepository.existsByProductIdAndUserId(PRODUCT_ID, USER_ID)).thenReturn(false);
        when(reviewRepository.saveAndFlush(any(Review.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user(USER_ID, "Ada")));

        ReviewDto result = reviewService.create(USER_ID, PRODUCT_ID, new CreateReviewRequest(4, null, null));

        assertThat(result.authorName()).isEqualTo("Ada");
    }

    @Test
    void rejectsCreateWhenProductDoesNotExist() {
        when(productRepository.existsById(PRODUCT_ID)).thenReturn(false);

        assertThatThrownBy(() -> reviewService.create(USER_ID, PRODUCT_ID, new CreateReviewRequest(4, null, null)))
                .isInstanceOf(ProductNotFoundException.class);
        verify(reviewRepository, never()).saveAndFlush(any(Review.class));
    }

    @Test
    void rejectsCreateWhenUserDidNotPurchase() {
        when(productRepository.existsById(PRODUCT_ID)).thenReturn(true);
        when(purchaseVerifier.hasPurchased(USER_ID, PRODUCT_ID)).thenReturn(false);

        assertThatThrownBy(() -> reviewService.create(USER_ID, PRODUCT_ID, new CreateReviewRequest(4, null, null)))
                .isInstanceOf(PurchaseRequiredException.class);
        verify(reviewRepository, never()).saveAndFlush(any(Review.class));
    }

    @Test
    void rejectsSecondReviewForSameProduct() {
        when(productRepository.existsById(PRODUCT_ID)).thenReturn(true);
        when(purchaseVerifier.hasPurchased(USER_ID, PRODUCT_ID)).thenReturn(true);
        when(reviewRepository.existsByProductIdAndUserId(PRODUCT_ID, USER_ID)).thenReturn(true);
        Review existing = review(USER_ID, 3);
        when(reviewRepository.findByProductIdAndUserId(PRODUCT_ID, USER_ID)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> reviewService.create(USER_ID, PRODUCT_ID, new CreateReviewRequest(4, null, null)))
                .isInstanceOf(DuplicateReviewException.class)
                .hasMessageContaining(existing.getId().toString());
        verify(reviewRepository, never()).saveAndFlush(any(Review.class));
    }

    @Test
    void updatesOwnReview() {
        Review existing = review(USER_ID, 3);
        when(reviewRepository.findById(REVIEW_ID)).thenReturn(Optional.of(existing));
        when(reviewRepository.saveAndFlush(any(Review.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ReviewDto result = reviewService.update(USER_ID, REVIEW_ID, new UpdateReviewRequest(1, "Meh", "Not for me"));

        assertThat(result.rating()).isEqualTo(1);
        assertThat(result.title()).isEqualTo("Meh");
        assertThat(result.comment()).isEqualTo("Not for me");
    }

    @Test
    void rejectsUpdateFromNonAuthorAsNotFound() {
        when(reviewRepository.findById(REVIEW_ID)).thenReturn(Optional.of(review(OTHER_USER_ID, 3)));

        assertThatThrownBy(() -> reviewService.update(USER_ID, REVIEW_ID, new UpdateReviewRequest(2, null, null)))
                .isInstanceOf(ReviewNotFoundException.class);
        verify(reviewRepository, never()).saveAndFlush(any(Review.class));
    }

    @Test
    void deletesOwnReview() {
        Review existing = review(USER_ID, 3);
        when(reviewRepository.findById(REVIEW_ID)).thenReturn(Optional.of(existing));

        reviewService.delete(USER_ID, REVIEW_ID);

        verify(reviewRepository).delete(existing);
    }

    @Test
    void rejectsDeleteFromNonAuthorAsNotFound() {
        when(reviewRepository.findById(REVIEW_ID)).thenReturn(Optional.of(review(OTHER_USER_ID, 3)));

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
                Map.of(1, 0L, 2, 0L, 3, 0L, 4, 0L, 5, 0L));
    }

    @Test
    void computesAverageAndFullDistribution() {
        when(productRepository.existsById(PRODUCT_ID)).thenReturn(true);
        when(reviewRepository.countPublishedGroupedByRating(PRODUCT_ID)).thenReturn(List.of(
                new RatingCount(5, 3L),
                new RatingCount(4, 1L),
                new RatingCount(1, 1L)));

        RatingSummaryDto result = reviewService.ratingSummary(PRODUCT_ID);

        // (5*3 + 4 + 1) / 5 = 4.0
        assertThat(result.average()).isEqualTo(4.0);
        assertThat(result.total()).isEqualTo(5L);
        assertThat(result.distribution()).containsExactlyInAnyOrderEntriesOf(
                Map.of(1, 1L, 2, 0L, 3, 0L, 4, 1L, 5, 3L));
    }

    @Test
    void roundsAverageToOneDecimal() {
        when(productRepository.existsById(PRODUCT_ID)).thenReturn(true);
        when(reviewRepository.countPublishedGroupedByRating(PRODUCT_ID)).thenReturn(List.of(
                new RatingCount(5, 1L),
                new RatingCount(4, 1L),
                new RatingCount(3, 1L)));

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
        Review published = review(USER_ID, 5);
        when(productRepository.existsById(PRODUCT_ID)).thenReturn(true);
        when(reviewRepository.findByProductIdAndStatus(eq(PRODUCT_ID), eq(ReviewStatus.PUBLISHED),
                any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(published), PageRequest.of(0, 20), 1));

        PageResponse<ReviewDto> result = reviewService.listByProduct(PRODUCT_ID, PageRequest.of(0, 20));

        assertThat(result.content()).hasSize(1);
        assertThat(result.content().getFirst().id()).isEqualTo(published.getId());
        assertThat(result.page()).isZero();
        assertThat(result.size()).isEqualTo(20);
        assertThat(result.totalElements()).isEqualTo(1L);
        assertThat(result.totalPages()).isEqualTo(1);
    }

    @Test
    void resolvesAuthorNamesOfAPageWithASingleLookup() {
        Review mine = review(USER_ID, 5);
        Review theirs = review(OTHER_USER_ID, 2);
        when(productRepository.existsById(PRODUCT_ID)).thenReturn(true);
        when(reviewRepository.findByProductIdAndStatus(eq(PRODUCT_ID), eq(ReviewStatus.PUBLISHED),
                any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(mine, theirs), PageRequest.of(0, 20), 2));
        when(userRepository.findAllById(any())).thenReturn(List.of(user(USER_ID, "Ada"), user(OTHER_USER_ID, null)));

        PageResponse<ReviewDto> result = reviewService.listByProduct(PRODUCT_ID, PageRequest.of(0, 20));

        assertThat(result.content()).extracting(ReviewDto::authorName).containsExactly("Ada", null);
        verify(userRepository, never()).findById(any());
    }

    @Test
    void listsOwnReviewsIncludingHiddenOnes() {
        Review hidden = review(USER_ID, 1);
        hidden.hide();
        when(reviewRepository.findByUserId(eq(USER_ID), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(hidden), PageRequest.of(0, 20), 1));

        PageResponse<ReviewDto> result = reviewService.listByUser(USER_ID, PageRequest.of(0, 20));

        assertThat(result.content()).hasSize(1);
        assertThat(result.content().getFirst().status()).isEqualTo(ReviewStatus.HIDDEN);
    }

    @Test
    void listsReviewsForModerationWithOptionalFilters() {
        Review any = review(USER_ID, 3);
        when(reviewRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(any), PageRequest.of(0, 20), 1));

        PageResponse<ReviewDto> result = reviewService.listForModeration(null, null, PageRequest.of(0, 20));

        assertThat(result.totalElements()).isEqualTo(1L);
    }

    @Test
    void hidesAndRepublishesAReview() {
        Review existing = review(OTHER_USER_ID, 3);
        when(reviewRepository.findById(REVIEW_ID)).thenReturn(Optional.of(existing));
        when(reviewRepository.saveAndFlush(any(Review.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertThat(reviewService.changeStatus(REVIEW_ID, ReviewStatus.HIDDEN).status())
                .isEqualTo(ReviewStatus.HIDDEN);
        assertThat(reviewService.changeStatus(REVIEW_ID, ReviewStatus.PUBLISHED).status())
                .isEqualTo(ReviewStatus.PUBLISHED);
    }

    @Test
    void adminDeletesAnyReviewRegardlessOfAuthor() {
        Review theirs = review(OTHER_USER_ID, 3);
        when(reviewRepository.findById(REVIEW_ID)).thenReturn(Optional.of(theirs));

        reviewService.deleteAsAdmin(REVIEW_ID);

        verify(reviewRepository).delete(theirs);
    }

    @Test
    void adminDeleteOfUnknownReviewIsNotFound() {
        when(reviewRepository.findById(REVIEW_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reviewService.deleteAsAdmin(REVIEW_ID))
                .isInstanceOf(ReviewNotFoundException.class);
    }

    /**
     * Construye una resenia por el mismo camino que el codigo de produccion: la factory estatica.
     * El id lo asigna ella, asi que las pruebas que lo necesitan lo leen del objeto en vez de
     * imponer una constante. Es una mejora, no una concesion: el test deja de depender de un
     * detalle que el dominio ahora controla.
     */
    private Review review(UUID userId, int rating) {
        return Review.publish(PRODUCT_ID, userId, rating, null, null);
    }

    private User user(UUID id, String name) {
        User user = new User();
        user.setId(id);
        user.setEmail(id + "@amancay.com");
        user.setName(name);
        return user;
    }
}
