package com.amancay.application.port.in;

import java.util.UUID;

public interface UpdateReviewUseCase {
    // Solo el autor; para cualquier otro la resena 'no existe'.
    ReviewWithAuthor update(UUID userId, UUID reviewId, int rating, String title, String comment);
}
