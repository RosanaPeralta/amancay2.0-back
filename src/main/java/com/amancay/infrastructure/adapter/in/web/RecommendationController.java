package com.amancay.infrastructure.adapter.in.web;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.amancay.application.port.in.RecommendProductsQuery;
import com.amancay.infrastructure.adapter.in.web.dto.RecommendationsResponse;
import com.amancay.infrastructure.security.LoggedUser;

/** Sugerencias de productos para el usuario autenticado, en base a sus favoritos. */
@RestController
@RequestMapping("/api/me/recommendations")
public class RecommendationController {

    private final RecommendProductsQuery recommendProductsQuery;

    public RecommendationController(RecommendProductsQuery recommendProductsQuery) {
        this.recommendProductsQuery = recommendProductsQuery;
    }

    @GetMapping
    public ResponseEntity<RecommendationsResponse> list(
            @AuthenticationPrincipal LoggedUser loggedUser,
            @RequestParam(defaultValue = "8") int limit) {
        return ResponseEntity.ok(RecommendationsResponse.from(recommendProductsQuery.recommend(loggedUser.id(), limit)));
    }
}
