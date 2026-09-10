package com.amancay.controllers;

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

import com.amancay.dto.ReviewDto;
import com.amancay.dto.UpdateReviewRequest;
import com.amancay.security.LoggedUser;
import com.amancay.service.ReviewService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/reviews")
@Validated
public class ReviewController {
    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @PutMapping("/{id}")
    public ResponseEntity<ReviewDto> update(
            @AuthenticationPrincipal LoggedUser loggedUser,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateReviewRequest request) {
        return ResponseEntity.ok(reviewService.update(loggedUser.id(), id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal LoggedUser loggedUser,
            @PathVariable UUID id) {
        reviewService.delete(loggedUser.id(), id);
        return ResponseEntity.noContent().build();
    }
}
