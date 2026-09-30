package com.amancay.domain.ports.out;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import com.amancay.domain.model.ProductReview;
import com.amancay.domain.model.ReviewPage;
import com.amancay.domain.model.ReviewVisibility;
import com.amancay.domain.ports.in.ReviewUseCases.SortOrder;

public interface ReviewCatalogPort {
    boolean productExists(UUID productId);

    boolean existsByProductAndUser(UUID productId, UUID userId);

    Optional<UUID> findIdByProductAndUser(UUID productId, UUID userId);

    ProductReview save(ProductReview review);

    Optional<ProductReview> findById(UUID reviewId);

    void delete(ProductReview review);

    ReviewPage findByProductAndVisibility(UUID productId, ReviewVisibility visibility, int page, int size,
            List<SortOrder> sort);

    ReviewPage findByUser(UUID userId, int page, int size, List<SortOrder> sort);

    ReviewPage search(ReviewVisibility visibility, UUID productId, int page, int size, List<SortOrder> sort);

    Map<Integer, Long> countPublishedByRating(UUID productId);

    Map<UUID, String> findAuthorNames(Set<UUID> userIds);

    Optional<String> findAuthorName(UUID userId);
}