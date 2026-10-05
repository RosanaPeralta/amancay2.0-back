package com.amancay.application.port.in;

import java.util.UUID;

import com.amancay.domain.model.PageQuery;
import com.amancay.domain.model.PageResult;
import com.amancay.domain.model.RatingSummary;
import com.amancay.domain.model.ReviewSort;

public interface ListReviewsQuery {
    // Solo las publicadas.
    PageResult<ReviewWithAuthor> listByProduct(UUID productId, ReviewSort sort, PageQuery page);

    // Las del usuario, incluidas las ocultadas por moderacion.
    PageResult<ReviewWithAuthor> listByUser(UUID userId, PageQuery page);

    RatingSummary ratingSummary(UUID productId);
}
