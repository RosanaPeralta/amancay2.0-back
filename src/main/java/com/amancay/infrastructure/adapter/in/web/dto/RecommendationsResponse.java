package com.amancay.infrastructure.adapter.in.web.dto;

import java.util.List;

import com.amancay.application.port.in.ProductRecommendations;

// source: AI | CATEGORIES | NONE, para que el front explique de donde salen las sugerencias.
public record RecommendationsResponse(String source, int favoritesCount, int basedOnCount,
        List<ProductResponse> products) {

    public static RecommendationsResponse from(ProductRecommendations recommendations) {
        return new RecommendationsResponse(recommendations.source().name(), recommendations.favoritesCount(),
                recommendations.basedOnCount(),
                recommendations.products().stream().map(ProductResponse::from).toList());
    }
}
