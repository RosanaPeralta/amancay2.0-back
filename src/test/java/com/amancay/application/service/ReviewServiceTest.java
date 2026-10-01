package com.amancay.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.amancay.application.port.in.ReviewWithAuthor;
import com.amancay.application.service.fake.InMemoryProductRepository;
import com.amancay.application.service.fake.InMemoryReviewRepository;
import com.amancay.application.service.fake.InMemoryUserRepository;
import com.amancay.domain.exception.DuplicateReviewException;
import com.amancay.domain.exception.ProductNotFoundException;
import com.amancay.domain.exception.PurchaseRequiredException;
import com.amancay.domain.exception.ReviewNotFoundException;
import com.amancay.domain.model.PageQuery;
import com.amancay.domain.model.PageResult;
import com.amancay.domain.model.RatingSummary;
import com.amancay.domain.model.ReviewSort;
import com.amancay.domain.model.ReviewStatus;
import com.amancay.domain.model.Role;

class ReviewServiceTest {

    private static final UUID USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID OTHER_USER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final PageQuery PAGE = new PageQuery(0, 20);

    private final InMemoryReviewRepository reviews = new InMemoryReviewRepository();
    private final InMemoryProductRepository products = new InMemoryProductRepository();
    private final InMemoryUserRepository users = new InMemoryUserRepository();
    private boolean purchased = true;
    private ReviewService reviewService;
    private UUID productId;

    @BeforeEach
    void setUp() {
        reviewService = new ReviewService(reviews, products, users, (userId, product) -> purchased);
        productId = products.add("Carpa", "carpa", true, Set.of()).getId();
        users.add(USER_ID, "ada@amancay.com", "Ada", Role.BUYER, true);
        users.add(OTHER_USER_ID, "x@amancay.com", null, Role.BUYER, true);
    }

    @Test
    void createsReviewWhenProductExistsAndPurchaseIsVerified() {
        ReviewWithAuthor result = reviewService.create(USER_ID, productId, 5, "Great", "Loved it");

        assertThat(result.review().getId()).isNotNull();
        assertThat(result.review().getProductId()).isEqualTo(productId);
        assertThat(result.review().getUserId()).isEqualTo(USER_ID);
        assertThat(result.review().getRating()).isEqualTo(5);
        assertThat(result.review().getTitle()).isEqualTo("Great");
        assertThat(result.review().getComment()).isEqualTo("Loved it");
        assertThat(result.review().getStatus()).isEqualTo(ReviewStatus.PUBLISHED);
    }

    @Test
    void exposesAuthorNameButNeverEmail() {
        assertThat(reviewService.create(USER_ID, productId, 4, null, null).authorName()).isEqualTo("Ada");
    }

    @Test
    void rejectsCreateWhenProductDoesNotExist() {
        assertThatThrownBy(() -> reviewService.create(USER_ID, UUID.randomUUID(), 4, null, null))
                .isInstanceOf(ProductNotFoundException.class);
        assertThat(reviews.calls).doesNotContain("save");
    }

    @Test
    void rejectsCreateWhenUserDidNotPurchase() {
        purchased = false;

        assertThatThrownBy(() -> reviewService.create(USER_ID, productId, 4, null, null))
                .isInstanceOf(PurchaseRequiredException.class);
        assertThat(reviews.calls).doesNotContain("save");
    }

    @Test
    void rejectsSecondReviewForSameProduct() {
        UUID existingId = reviewService.create(USER_ID, productId, 3, null, null).review().getId();
        reviews.calls.clear();

        assertThatThrownBy(() -> reviewService.create(USER_ID, productId, 4, null, null))
                .isInstanceOf(DuplicateReviewException.class)
                .hasMessageContaining(existingId.toString());
        assertThat(reviews.calls).doesNotContain("save");
    }

    @Test
    void updatesOwnReview() {
        UUID id = reviewService.create(USER_ID, productId, 3, null, null).review().getId();

        ReviewWithAuthor result = reviewService.update(USER_ID, id, 1, "Meh", "Not for me");

        assertThat(result.review().getRating()).isEqualTo(1);
        assertThat(result.review().getTitle()).isEqualTo("Meh");
        assertThat(result.review().getComment()).isEqualTo("Not for me");
    }

    @Test
    void rejectsUpdateFromNonAuthorAsNotFound() {
        UUID id = reviewService.create(OTHER_USER_ID, productId, 3, null, null).review().getId();
        reviews.calls.clear();

        assertThatThrownBy(() -> reviewService.update(USER_ID, id, 2, null, null))
                .isInstanceOf(ReviewNotFoundException.class);
        assertThat(reviews.calls).doesNotContain("save");
    }

    @Test
    void deletesOwnReview() {
        UUID id = reviewService.create(USER_ID, productId, 3, null, null).review().getId();

        reviewService.delete(USER_ID, id);

        assertThat(reviews.stored(id)).isNull();
    }

    @Test
    void rejectsDeleteFromNonAuthorAsNotFound() {
        UUID id = reviewService.create(OTHER_USER_ID, productId, 3, null, null).review().getId();

        assertThatThrownBy(() -> reviewService.delete(USER_ID, id)).isInstanceOf(ReviewNotFoundException.class);
        assertThat(reviews.stored(id)).isNotNull();
    }

    @Test
    void rejectsUpdateOfUnknownReview() {
        assertThatThrownBy(() -> reviewService.update(USER_ID, UUID.randomUUID(), 2, null, null))
                .isInstanceOf(ReviewNotFoundException.class);
    }

    @Test
    void returnsEmptySummaryWhenProductHasNoReviews() {
        RatingSummary result = reviewService.ratingSummary(productId);

        assertThat(result.average()).isNull();
        assertThat(result.total()).isZero();
        assertThat(result.distribution()).containsExactlyInAnyOrderEntriesOf(
                Map.of(1, 0L, 2, 0L, 3, 0L, 4, 0L, 5, 0L));
    }

    @Test
    void summaryCountsOnlyPublishedReviews() {
        reviewService.create(USER_ID, productId, 5, null, null);
        UUID hidden = reviewService.create(OTHER_USER_ID, productId, 1, null, null).review().getId();
        reviewService.changeStatus(hidden, ReviewStatus.HIDDEN);

        RatingSummary result = reviewService.ratingSummary(productId);

        assertThat(result.total()).isEqualTo(1L);
        assertThat(result.average()).isEqualTo(5.0);
    }

    @Test
    void rejectsSummaryForUnknownProduct() {
        assertThatThrownBy(() -> reviewService.ratingSummary(UUID.randomUUID()))
                .isInstanceOf(ProductNotFoundException.class);
    }

    @Test
    void listsOnlyPublishedReviewsOfAProduct() {
        UUID published = reviewService.create(USER_ID, productId, 5, null, null).review().getId();
        UUID hidden = reviewService.create(OTHER_USER_ID, productId, 2, null, null).review().getId();
        reviewService.changeStatus(hidden, ReviewStatus.HIDDEN);

        PageResult<ReviewWithAuthor> result = reviewService.listByProduct(productId, ReviewSort.RECENT, PAGE);

        assertThat(result.content()).extracting(item -> item.review().getId()).containsExactly(published);
        assertThat(result.page()).isZero();
        assertThat(result.size()).isEqualTo(20);
        assertThat(result.totalElements()).isEqualTo(1L);
        assertThat(result.totalPages()).isEqualTo(1);
    }

    @Test
    void sortsByBestRating() {
        reviewService.create(USER_ID, productId, 2, null, null);
        reviewService.create(OTHER_USER_ID, productId, 5, null, null);

        assertThat(reviewService.listByProduct(productId, ReviewSort.BEST, PAGE).content())
                .extracting(item -> item.review().getRating()).containsExactly(5, 2);
    }

    @Test
    void resolvesAuthorNamesOfAPageWithASingleLookup() {
        reviewService.create(USER_ID, productId, 5, null, null);
        reviewService.create(OTHER_USER_ID, productId, 2, null, null);
        users.calls.clear();

        PageResult<ReviewWithAuthor> result = reviewService.listByProduct(productId, ReviewSort.BEST, PAGE);

        assertThat(result.content()).extracting(ReviewWithAuthor::authorName).containsExactly("Ada", null);
        assertThat(users.count("findAllById")).isEqualTo(1);
        assertThat(users.count("findById")).isZero();
    }

    @Test
    void listsOwnReviewsIncludingHiddenOnes() {
        UUID id = reviewService.create(USER_ID, productId, 1, null, null).review().getId();
        reviewService.changeStatus(id, ReviewStatus.HIDDEN);

        PageResult<ReviewWithAuthor> result = reviewService.listByUser(USER_ID, PAGE);

        assertThat(result.content()).singleElement()
                .satisfies(item -> assertThat(item.review().getStatus()).isEqualTo(ReviewStatus.HIDDEN));
    }

    @Test
    void listsReviewsForModerationWithOptionalFilters() {
        reviewService.create(USER_ID, productId, 3, null, null);
        UUID hidden = reviewService.create(OTHER_USER_ID, productId, 1, null, null).review().getId();
        reviewService.changeStatus(hidden, ReviewStatus.HIDDEN);

        assertThat(reviewService.listForModeration(null, null, PAGE).totalElements()).isEqualTo(2L);
        assertThat(reviewService.listForModeration(ReviewStatus.HIDDEN, productId, PAGE).totalElements())
                .isEqualTo(1L);
        assertThat(reviewService.listForModeration(null, UUID.randomUUID(), PAGE).totalElements()).isZero();
    }

    @Test
    void hidesAndRepublishesAReview() {
        UUID id = reviewService.create(OTHER_USER_ID, productId, 3, null, null).review().getId();

        assertThat(reviewService.changeStatus(id, ReviewStatus.HIDDEN).review().getStatus())
                .isEqualTo(ReviewStatus.HIDDEN);
        assertThat(reviewService.changeStatus(id, ReviewStatus.PUBLISHED).review().getStatus())
                .isEqualTo(ReviewStatus.PUBLISHED);
    }

    @Test
    void adminDeletesAnyReviewRegardlessOfAuthor() {
        UUID id = reviewService.create(OTHER_USER_ID, productId, 3, null, null).review().getId();

        reviewService.deleteAsAdmin(id);

        assertThat(reviews.stored(id)).isNull();
    }

    @Test
    void adminDeleteOfUnknownReviewIsNotFound() {
        assertThatThrownBy(() -> reviewService.deleteAsAdmin(UUID.randomUUID()))
                .isInstanceOf(ReviewNotFoundException.class);
    }
}
