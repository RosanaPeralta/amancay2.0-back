package com.amancay.application.port.in;

import java.util.UUID;

public interface CreateFavoriteUseCase {
    FavoriteProduct create(UUID userId, UUID productId);
}
