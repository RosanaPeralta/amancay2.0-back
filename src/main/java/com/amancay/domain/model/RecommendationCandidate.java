package com.amancay.domain.model;

import java.util.List;
import java.util.Set;
import java.util.UUID;

// Lo que el recomendador necesita saber de un producto activo: ids de categoria para el
// fallback y nombres legibles para el prompt de la IA.
public record RecommendationCandidate(UUID id, String name, String shortDescription, Set<UUID> categoryIds,
        List<String> categoryNames) {

    public RecommendationCandidate {
        categoryIds = Set.copyOf(categoryIds);
        categoryNames = List.copyOf(categoryNames);
    }
}
