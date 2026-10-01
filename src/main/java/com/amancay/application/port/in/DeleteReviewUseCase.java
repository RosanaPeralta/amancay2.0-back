package com.amancay.application.port.in;

import java.util.UUID;

public interface DeleteReviewUseCase {
    // Solo el autor; para cualquier otro la resena 'no existe'.
    void delete(UUID userId, UUID reviewId);
}
