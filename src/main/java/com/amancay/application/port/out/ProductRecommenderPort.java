package com.amancay.application.port.out;

import java.util.List;
import java.util.UUID;

import com.amancay.domain.exception.RecommenderUnavailableException;
import com.amancay.domain.model.RecommendationCandidate;

public interface ProductRecommenderPort {
    // Ids de candidates ordenados por relevancia. Puede devolver ids que no son candidatos
    // (alucinaciones): el caso de uso los descarta.
    List<UUID> rank(List<RecommendationCandidate> favorites, List<RecommendationCandidate> candidates, int limit)
            throws RecommenderUnavailableException;
}
