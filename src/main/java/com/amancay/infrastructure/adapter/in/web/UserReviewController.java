package com.amancay.infrastructure.adapter.in.web;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.amancay.application.port.in.ListReviewsQuery;
import com.amancay.domain.model.PageQuery;
import com.amancay.infrastructure.adapter.in.web.dto.PageResponse;
import com.amancay.infrastructure.adapter.in.web.dto.ReviewResponse;
import com.amancay.infrastructure.security.LoggedUser;

/** Mis reseñas, incluidas las ocultadas por moderación (la respuesta trae el {@code status}). */
@RestController
@RequestMapping("/api/me/reviews")
public class UserReviewController {

    private final ListReviewsQuery listReviewsQuery;

    public UserReviewController(ListReviewsQuery listReviewsQuery) {
        this.listReviewsQuery = listReviewsQuery;
    }

    @GetMapping
    public ResponseEntity<PageResponse<ReviewResponse>> list(
            @AuthenticationPrincipal LoggedUser loggedUser,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(PageResponse.from(listReviewsQuery.listByUser(loggedUser.id(), new PageQuery(page, size)),
                ReviewResponse::from));
    }
}
