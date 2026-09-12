package com.amancay.controllers;

import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.amancay.dto.PageResponse;
import com.amancay.dto.ReviewDto;
import com.amancay.dto.UpdateReviewStatusRequest;
import com.amancay.entity.ReviewStatus;
import com.amancay.service.ReviewService;

import jakarta.validation.Valid;

/**
 * REV-07: moderación de reseñas. Solo ADMIN; el rol lo resuelve {@code UserRoleAuthoritiesFilter}
 * desde {@code users.role} y un BUYER recibe 403.
 */
@RestController
@RequestMapping("/api/admin/reviews")
@PreAuthorize("hasRole('ADMIN')")
@Validated
public class AdminReviewController {
    private final ReviewService reviewService;

    public AdminReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @GetMapping
    public ResponseEntity<PageResponse<ReviewDto>> list(
            @RequestParam(required = false) ReviewStatus status,
            @RequestParam(required = false) UUID productId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100),
                Sort.by("createdAt").descending());
        return ResponseEntity.ok(reviewService.listForModeration(status, productId, pageable));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ReviewDto> changeStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateReviewStatusRequest request) {
        return ResponseEntity.ok(reviewService.changeStatus(id, request.status()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        reviewService.deleteAsAdmin(id);
        return ResponseEntity.noContent().build();
    }
}
