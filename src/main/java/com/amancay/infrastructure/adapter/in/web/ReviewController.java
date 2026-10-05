package com.amancay.infrastructure.adapter.in.web;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.amancay.application.port.in.DeleteReviewUseCase;
import com.amancay.application.port.in.UpdateReviewUseCase;
import com.amancay.infrastructure.adapter.in.web.dto.ReviewResponse;
import com.amancay.infrastructure.adapter.in.web.dto.UpdateReviewRequest;
import com.amancay.infrastructure.security.LoggedUser;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/reviews")
@Validated
public class ReviewController {

    private final UpdateReviewUseCase updateReviewUseCase;
    private final DeleteReviewUseCase deleteReviewUseCase;

    public ReviewController(UpdateReviewUseCase updateReviewUseCase, DeleteReviewUseCase deleteReviewUseCase) {
        this.updateReviewUseCase = updateReviewUseCase;
        this.deleteReviewUseCase = deleteReviewUseCase;
    }

    @PutMapping("/{id}")
    public ResponseEntity<ReviewResponse> update(
            @AuthenticationPrincipal LoggedUser loggedUser,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateReviewRequest request) {
        return ResponseEntity.ok(ReviewResponse.from(updateReviewUseCase.update(loggedUser.id(), id, request.rating(),
                request.title(), request.comment())));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal LoggedUser loggedUser,
            @PathVariable UUID id) {
        deleteReviewUseCase.delete(loggedUser.id(), id);
        return ResponseEntity.noContent().build();
    }
}
