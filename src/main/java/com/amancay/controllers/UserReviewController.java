package com.amancay.controllers;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.amancay.dto.PageResponse;
import com.amancay.dto.ReviewDto;
import com.amancay.security.LoggedUser;
import com.amancay.service.ReviewService;

/** REV-09: mis reseñas, incluidas las ocultadas por moderación (el DTO trae el {@code status}). */
@RestController
@RequestMapping("/api/me/reviews")
public class UserReviewController {
    private final ReviewService reviewService;

    public UserReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @GetMapping
    public ResponseEntity<PageResponse<ReviewDto>> list(
            @AuthenticationPrincipal LoggedUser loggedUser,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100),
                Sort.by("createdAt").descending());
        return ResponseEntity.ok(reviewService.listByUser(loggedUser.id(), pageable));
    }
}
