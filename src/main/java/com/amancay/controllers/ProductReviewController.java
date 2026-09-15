package com.amancay.controllers;

import java.net.URI;
import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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

import com.amancay.dto.CreateReviewRequest;
import com.amancay.dto.PageResponse;
import com.amancay.dto.RatingSummaryDto;
import com.amancay.dto.ReviewDto;
import com.amancay.dto.ReviewSort;
import com.amancay.security.LoggedUser;
import com.amancay.service.ReviewService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/products/{productId}/reviews")
@Validated
public class ProductReviewController {
    private final ReviewService reviewService;

    public ProductReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @GetMapping
    public ResponseEntity<PageResponse<ReviewDto>> list(
            @PathVariable UUID productId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "recent") String sort) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100),
                ReviewSort.from(sort).sort());
        return ResponseEntity.ok(reviewService.listByProduct(productId, pageable));
    }

    @GetMapping("/summary")
    public ResponseEntity<RatingSummaryDto> summary(@PathVariable UUID productId) {
        return ResponseEntity.ok(reviewService.ratingSummary(productId));
    }

    @PostMapping
    public ResponseEntity<ReviewDto> create(
            @AuthenticationPrincipal LoggedUser loggedUser,
            @PathVariable UUID productId,
            @Valid @RequestBody CreateReviewRequest request) {
        ReviewDto review = reviewService.create(loggedUser.id(), productId, request);
        return ResponseEntity.created(URI.create("/api/reviews/" + review.id())).body(review);
    }
}
