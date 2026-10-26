package com.amancay.application.port.in;

import java.util.List;

import com.amancay.domain.model.Product;
import com.amancay.domain.model.RecommendationSource;

// basedOnCount: cuantos favoritos (de favoritesCount) se usaron en esta tanda.
public record ProductRecommendations(List<Product> products, RecommendationSource source, int favoritesCount,
        int basedOnCount) {

    public ProductRecommendations {
        products = List.copyOf(products);
    }
}
