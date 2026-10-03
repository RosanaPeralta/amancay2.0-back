package com.amancay.infrastructure.adapter.in.web;

import java.net.URI;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.amancay.application.port.in.CreateReviewUseCase;
import com.amancay.application.port.in.ListReviewsQuery;
import com.amancay.domain.model.PageQuery;
import com.amancay.domain.model.ReviewSort;
import com.amancay.infrastructure.adapter.in.web.dto.CreateReviewRequest;
import com.amancay.infrastructure.adapter.in.web.dto.PageResponse;
import com.amancay.infrastructure.adapter.in.web.dto.RatingSummaryResponse;
import com.amancay.infrastructure.adapter.in.web.dto.ReviewResponse;
import com.amancay.infrastructure.security.LoggedUser;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/products/{productId}/reviews")
@Validated
public class ProductReviewController {

    private final ListReviewsQuery listReviewsQuery;
    private final CreateReviewUseCase createReviewUseCase;

    public ProductReviewController(ListReviewsQuery listReviewsQuery, CreateReviewUseCase createReviewUseCase) {
        this.listReviewsQuery = listReviewsQuery;
        this.createReviewUseCase = createReviewUseCase;
    }

    @GetMapping
    public ResponseEntity<PageResponse<ReviewResponse>> list(
            @PathVariable UUID productId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "recent") String sort) {
        return ResponseEntity.ok(PageResponse.from(listReviewsQuery.listByProduct(productId, ReviewSort.from(sort),
                new PageQuery(page, size)), ReviewResponse::from));
    }

    @GetMapping("/summary")
    public ResponseEntity<RatingSummaryResponse> summary(@PathVariable UUID productId) {
        return ResponseEntity.ok(RatingSummaryResponse.from(listReviewsQuery.ratingSummary(productId)));
    }

    @PostMapping
    public ResponseEntity<ReviewResponse> create(
            @AuthenticationPrincipal LoggedUser loggedUser,
            @PathVariable UUID productId,
            @Valid @RequestBody CreateReviewRequest request) {
        ReviewResponse review = ReviewResponse.from(createReviewUseCase.create(loggedUser.id(), productId,
                request.rating(), request.title(), request.comment()));
        return ResponseEntity.created(URI.create("/api/reviews/" + review.id())).body(review);
    }
}
