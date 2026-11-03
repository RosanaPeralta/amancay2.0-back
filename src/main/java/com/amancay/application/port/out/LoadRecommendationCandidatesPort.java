package com.amancay.application.port.out;

import java.util.List;

import com.amancay.domain.model.RecommendationCandidate;

public interface LoadRecommendationCandidatesPort {
    // Productos activos (acotado a un maximo para no inflar el prompt).
    List<RecommendationCandidate> findActive();
}
