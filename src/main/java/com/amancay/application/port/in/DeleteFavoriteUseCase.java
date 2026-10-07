package com.amancay.application.port.in;

import java.util.UUID;

public interface DeleteFavoriteUseCase {
    void delete(UUID userId, UUID productId);
}
