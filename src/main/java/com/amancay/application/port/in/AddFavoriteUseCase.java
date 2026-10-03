package com.amancay.application.port.in;

import java.util.UUID;

public interface AddFavoriteUseCase {
    FavoriteProduct add(UUID userId, UUID productId);
}
