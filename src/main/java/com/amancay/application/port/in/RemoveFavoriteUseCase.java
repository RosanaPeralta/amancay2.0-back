package com.amancay.application.port.in;

import java.util.UUID;

public interface RemoveFavoriteUseCase {
    void remove(UUID userId, UUID productId);
}
