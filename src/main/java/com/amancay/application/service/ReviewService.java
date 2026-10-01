package com.amancay.application.service;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.amancay.application.port.in.CreateReviewUseCase;
import com.amancay.application.port.in.DeleteReviewUseCase;
import com.amancay.application.port.in.ListReviewsQuery;
import com.amancay.application.port.in.ModerateReviewsUseCase;
import com.amancay.application.port.in.ReviewWithAuthor;
import com.amancay.application.port.in.UpdateReviewUseCase;
import com.amancay.application.port.out.PurchaseVerifierPort;
import com.amancay.domain.exception.DuplicateReviewException;
import com.amancay.domain.exception.ProductNotFoundException;
import com.amancay.domain.exception.PurchaseRequiredException;
import com.amancay.domain.exception.ReviewNotFoundException;
import com.amancay.domain.model.PageQuery;
import com.amancay.domain.model.PageResult;
import com.amancay.domain.model.RatingSummary;
import com.amancay.domain.model.Review;
import com.amancay.domain.model.ReviewSort;
import com.amancay.domain.model.ReviewStatus;
import com.amancay.domain.model.User;
import com.amancay.domain.port.ProductRepositoryPort;
import com.amancay.domain.port.ReviewRepositoryPort;
import com.amancay.domain.port.UserRepositoryPort;

@Service
public class ReviewService implements CreateReviewUseCase, UpdateReviewUseCase, DeleteReviewUseCase, ListReviewsQuery,
        ModerateReviewsUseCase {

    private final ReviewRepositoryPort reviewRepository;
    private final ProductRepositoryPort productRepository;
    private final UserRepositoryPort userRepository;
    private final PurchaseVerifierPort purchaseVerifier;

    public ReviewService(ReviewRepositoryPort reviewRepository, ProductRepositoryPort productRepository,
            UserRepositoryPort userRepository, PurchaseVerifierPort purchaseVerifier) {
        this.reviewRepository = reviewRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
        this.purchaseVerifier = purchaseVerifier;
    }

    @Override
    @Transactional
    public ReviewWithAuthor create(UUID userId, UUID productId, int rating, String title, String comment) {
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
        return withAuthor(reviewRepository.save(Review.create(productId, userId, rating, title, comment)));
    }

    @Override
    @Transactional
    public ReviewWithAuthor update(UUID userId, UUID reviewId, int rating, String title, String comment) {
        Review review = findOwnedReview(userId, reviewId);
        review.update(rating, title, comment);
        return withAuthor(reviewRepository.save(review));
    }

    @Override
    @Transactional
    public void delete(UUID userId, UUID reviewId) {
        reviewRepository.delete(findOwnedReview(userId, reviewId));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<ReviewWithAuthor> listByProduct(UUID productId, ReviewSort sort, PageQuery page) {
        requireProduct(productId);
        return withAuthors(reviewRepository.findPublishedByProduct(productId, sort, page));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<ReviewWithAuthor> listByUser(UUID userId, PageQuery page) {
        return withAuthors(reviewRepository.findByUserId(userId, page));
    }

    @Override
    @Transactional(readOnly = true)
    public RatingSummary ratingSummary(UUID productId) {
        requireProduct(productId);
        return RatingSummary.from(reviewRepository.countPublishedGroupedByRating(productId));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<ReviewWithAuthor> listForModeration(ReviewStatus status, UUID productId, PageQuery page) {
        return withAuthors(reviewRepository.findForModeration(status, productId, page));
    }

    @Override
    @Transactional
    public ReviewWithAuthor changeStatus(UUID reviewId, ReviewStatus status) {
        Review review = findReview(reviewId);
        review.changeStatus(status);
        return withAuthor(reviewRepository.save(review));
    }

    @Override
    @Transactional
    public void deleteAsAdmin(UUID reviewId) {
        reviewRepository.delete(findReview(reviewId));
    }

    private void requireProduct(UUID productId) {
        if (!productRepository.existsById(productId)) {
            throw new ProductNotFoundException(productId);
        }
    }

    private Review findReview(UUID id) {
        return reviewRepository.findById(id).orElseThrow(() -> new ReviewNotFoundException(id));
    }

    // Para cualquier otro usuario, la resena de otro "no existe".
    private Review findOwnedReview(UUID userId, UUID reviewId) {
        Review review = findReview(reviewId);
        if (!review.isWrittenBy(userId)) {
            throw new ReviewNotFoundException(reviewId);
        }
        return review;
    }

    private ReviewWithAuthor withAuthor(Review review) {
        return new ReviewWithAuthor(review, userRepository.findById(review.getUserId()).map(User::getName).orElse(null));
    }

    // Resuelve los nombres de autor de toda la pagina con una sola consulta.
    private PageResult<ReviewWithAuthor> withAuthors(PageResult<Review> reviews) {
        Set<UUID> userIds = reviews.content().stream().map(Review::getUserId).collect(Collectors.toSet());
        Map<UUID, String> namesById = userRepository.findAllById(userIds).stream()
                .filter(user -> user.getName() != null)
                .collect(Collectors.toMap(User::getId, User::getName, (a, b) -> a));
        return reviews.map(review -> new ReviewWithAuthor(review, namesById.get(review.getUserId())));
    }
}
