package com.amancay.infrastructure.adapters.out.persistence.mapper;

import com.amancay.domain.model.Favorite;

public final class FavoritePersistenceMapper {
    private FavoritePersistenceMapper() {
    }

    public static Favorite toDomain(com.amancay.entity.Favorite entity) {
        return new Favorite(entity.getId(), entity.getUserId(), entity.getProductId(), entity.getCreatedAt());
    }
}