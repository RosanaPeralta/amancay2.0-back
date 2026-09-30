package com.amancay.service;

import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.amancay.application.service.ReviewApplicationService;
import com.amancay.domain.model.ProductReview;
import com.amancay.domain.model.ReviewPage;
import com.amancay.domain.model.ReviewRatingSummary;
import com.amancay.domain.model.ReviewVisibility;
import com.amancay.domain.ports.in.ReviewUseCases;
import com.amancay.domain.ports.in.ReviewUseCases.ReviewInput;
import com.amancay.domain.ports.in.ReviewUseCases.SortOrder;
import com.amancay.domain.ports.out.ReviewCatalogPort;
import com.amancay.domain.ports.out.ReviewPurchasePort;
import com.amancay.dto.CreateReviewRequest;
import com.amancay.dto.PageResponse;
import com.amancay.dto.RatingCount;
import com.amancay.dto.RatingSummaryDto;
import com.amancay.dto.ReviewDto;
import com.amancay.dto.UpdateReviewRequest;
import com.amancay.entity.ReviewStatus;
import com.amancay.infrastructure.adapters.in.rest.mapper.ReviewApiMapper;
import com.amancay.infrastructure.adapters.out.persistence.ReviewPersistenceAdapter;
import com.amancay.infrastructure.adapters.out.persistence.ReviewPurchaseAdapter;
import com.amancay.repository.ProductRepository;
import com.amancay.repository.ReviewRepository;
import com.amancay.repository.UserRepository;

@Service
public class ReviewService {
    private final ReviewUseCases reviewUseCases;

    @Autowired
    public ReviewService(ReviewUseCases reviewUseCases) {
        this.reviewUseCases = reviewUseCases;
    }

    public ReviewService(ReviewRepository reviewRepository, ProductRepository productRepository,
            UserRepository userRepository, PurchaseVerifier purchaseVerifier) {
        ReviewCatalogPort catalog = new ReviewPersistenceAdapter(reviewRepository, productRepository, userRepository);
        ReviewPurchasePort purchasePort = new ReviewPurchaseAdapter(purchaseVerifier);
        this.reviewUseCases = new ReviewApplicationService(catalog, purchasePort);
    }

    @Transactional
    public ReviewDto create(UUID userId, UUID productId, CreateReviewRequest request) {
        return ReviewApiMapper.toDto(reviewUseCases.create(userId, productId,
                new ReviewInput(request.rating(), request.title(), request.comment())));
    }

    @Transactional
    public ReviewDto update(UUID userId, UUID reviewId, UpdateReviewRequest request) {
        return ReviewApiMapper.toDto(reviewUseCases.update(userId, reviewId,
            new ReviewInput(request.rating(), request.title(), request.comment())));
    }

    @Transactional
    public void delete(UUID userId, UUID reviewId) {
        reviewUseCases.delete(userId, reviewId);
    }

    @Transactional(readOnly = true)
    public PageResponse<ReviewDto> listByProduct(UUID productId, Pageable pageable) {
        return toPage(reviewUseCases.listByProduct(productId, pageable.getPageNumber(), pageable.getPageSize(), toSort(pageable)));
    }

    /** Las reseñas del usuario, incluidas las ocultadas por moderación. */
    @Transactional(readOnly = true)
    public PageResponse<ReviewDto> listByUser(UUID userId, Pageable pageable) {
        return toPage(reviewUseCases.listByUser(userId, pageable.getPageNumber(), pageable.getPageSize(), toSort(pageable)));
    }

    @Transactional(readOnly = true)
    public RatingSummaryDto ratingSummary(UUID productId) {
        ReviewRatingSummary summary = reviewUseCases.ratingSummary(productId);
        return new RatingSummaryDto(summary.average(), summary.total(), summary.distribution());
    }


    @Transactional(readOnly = true)
    public PageResponse<ReviewDto> listForModeration(ReviewStatus status, UUID productId, Pageable pageable) {
        ReviewVisibility visibility = status == null ? null : ReviewVisibility.valueOf(status.name());
        return toPage(reviewUseCases.listForModeration(visibility, productId, pageable.getPageNumber(),
            pageable.getPageSize(), toSort(pageable)));
    }

    @Transactional
    public ReviewDto changeStatus(UUID reviewId, ReviewStatus status) {
        return ReviewApiMapper.toDto(reviewUseCases.changeStatus(reviewId, ReviewVisibility.valueOf(status.name())));
    }

    /** A diferencia de {@link #delete}, no exige ser el autor: es la acción "eliminar" del panel ADMIN. */
    @Transactional
    public void deleteAsAdmin(UUID reviewId) {
        reviewUseCases.deleteAsAdmin(reviewId);
    }

    private List<SortOrder> toSort(Pageable pageable) {
        return pageable.getSort().stream().map(order -> new SortOrder(order.getProperty(), order.isAscending())).toList();
    }

    private PageResponse<ReviewDto> toPage(ReviewPage page) {
        return new PageResponse<>(page.content().stream().map(ReviewApiMapper::toDto).toList(), page.page(), page.size(),
                page.totalElements(), page.totalPages());
    }
}
