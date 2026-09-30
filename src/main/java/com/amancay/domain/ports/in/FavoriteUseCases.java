package com.amancay.domain.ports.in;

import java.util.List;
import java.util.UUID;

import com.amancay.domain.model.FavoritePage;
import com.amancay.domain.model.FavoriteProduct;

public interface FavoriteUseCases {
    FavoriteProduct add(UUID userId, UUID productId);

    void remove(UUID userId, UUID productId);

    FavoritePage list(UUID userId, int page, int size);

    List<UUID> listProductIds(UUID userId);
}