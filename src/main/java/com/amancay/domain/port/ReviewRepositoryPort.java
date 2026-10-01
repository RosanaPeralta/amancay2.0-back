package com.amancay.domain.port;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.amancay.domain.model.PageQuery;
import com.amancay.domain.model.PageResult;
import com.amancay.domain.model.RatingCount;
import com.amancay.domain.model.Review;
import com.amancay.domain.model.ReviewSort;
import com.amancay.domain.model.ReviewStatus;

public interface ReviewRepositoryPort {
    Optional<Review> findById(UUID id);

    Optional<Review> findByProductIdAndUserId(UUID productId, UUID userId);

    boolean existsByProductIdAndUserId(UUID productId, UUID userId);

    PageResult<Review> findPublishedByProduct(UUID productId, ReviewSort sort, PageQuery page);

    // Incluidas las ocultadas por moderacion. Mas nuevas primero.
    PageResult<Review> findByUserId(UUID userId, PageQuery page);

    // Filtros opcionales (null = sin filtrar). Mas nuevas primero.
    PageResult<Review> findForModeration(ReviewStatus status, UUID productId, PageQuery page);

    List<RatingCount> countPublishedGroupedByRating(UUID productId);

    Review save(Review review);

    void delete(Review review);
}
