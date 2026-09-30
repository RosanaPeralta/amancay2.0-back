package com.amancay.domain.ports.in;

import java.util.List;
import java.util.UUID;

import com.amancay.domain.model.ProductReview;
import com.amancay.domain.model.ReviewPage;
import com.amancay.domain.model.ReviewRatingSummary;
import com.amancay.domain.model.ReviewVisibility;

public interface ReviewUseCases {
    ProductReview create(UUID userId, UUID productId, ReviewInput input);

    ProductReview update(UUID userId, UUID reviewId, ReviewInput input);

    void delete(UUID userId, UUID reviewId);

    ReviewPage listByProduct(UUID productId, int page, int size, List<SortOrder> sort);

    ReviewPage listByUser(UUID userId, int page, int size, List<SortOrder> sort);

    ReviewRatingSummary ratingSummary(UUID productId);

    ReviewPage listForModeration(ReviewVisibility visibility, UUID productId, int page, int size,
            List<SortOrder> sort);

    ProductReview changeStatus(UUID reviewId, ReviewVisibility visibility);

    void deleteAsAdmin(UUID reviewId);

    record ReviewInput(int rating, String title, String comment) {
    }

    record SortOrder(String property, boolean ascending) {
    }
}