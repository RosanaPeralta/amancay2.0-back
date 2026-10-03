package com.amancay.application.port.in;

import java.util.UUID;

import com.amancay.domain.model.PageQuery;
import com.amancay.domain.model.PageResult;
import com.amancay.domain.model.ReviewStatus;

public interface ModerateReviewsUseCase {
    PageResult<ReviewWithAuthor> listForModeration(UUID requesterId, ReviewStatus status, UUID productId, PageQuery page);

    ReviewWithAuthor changeStatus(UUID requesterId, UUID reviewId, ReviewStatus status);

    // A diferencia de DeleteReviewUseCase, no exige ser el autor.
    void deleteAsAdmin(UUID requesterId, UUID reviewId);
}
