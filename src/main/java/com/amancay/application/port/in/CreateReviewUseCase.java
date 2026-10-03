package com.amancay.application.port.in;

import java.util.UUID;

public interface CreateReviewUseCase {
    ReviewWithAuthor create(UUID userId, UUID productId, int rating, String title, String comment);
}
