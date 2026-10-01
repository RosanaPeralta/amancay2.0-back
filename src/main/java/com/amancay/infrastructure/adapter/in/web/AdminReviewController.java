package com.amancay.infrastructure.adapter.in.web;

import java.util.UUID;

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

import com.amancay.application.port.in.ModerateReviewsUseCase;
import com.amancay.domain.model.PageQuery;
import com.amancay.domain.model.ReviewStatus;
import com.amancay.infrastructure.adapter.in.web.dto.PageResponse;
import com.amancay.infrastructure.adapter.in.web.dto.ReviewResponse;
import com.amancay.infrastructure.adapter.in.web.dto.UpdateReviewStatusRequest;

import jakarta.validation.Valid;

/**
 * Moderación de reseñas. Solo ADMIN; el rol lo resuelve {@code UserRoleAuthoritiesFilter}
 * desde {@code users.role} y un BUYER recibe 403.
 */
@RestController
@RequestMapping("/api/admin/reviews")
@PreAuthorize("hasRole('ADMIN')")
@Validated
public class AdminReviewController {

    private final ModerateReviewsUseCase moderateReviewsUseCase;

    public AdminReviewController(ModerateReviewsUseCase moderateReviewsUseCase) {
        this.moderateReviewsUseCase = moderateReviewsUseCase;
    }

    @GetMapping
    public ResponseEntity<PageResponse<ReviewResponse>> list(
            @RequestParam(required = false) ReviewStatus status,
            @RequestParam(required = false) UUID productId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(PageResponse.from(
                moderateReviewsUseCase.listForModeration(status, productId, new PageQuery(page, size)),
                ReviewResponse::from));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ReviewResponse> changeStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateReviewStatusRequest request) {
        return ResponseEntity.ok(ReviewResponse.from(moderateReviewsUseCase.changeStatus(id, request.status())));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        moderateReviewsUseCase.deleteAsAdmin(id);
        return ResponseEntity.noContent().build();
    }
}
