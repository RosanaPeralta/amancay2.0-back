package com.amancay.infrastructure.adapter.out.persistence.mapper;

import org.springframework.stereotype.Component;

import com.amancay.domain.model.Favorite;
import com.amancay.infrastructure.adapter.out.persistence.entity.FavoriteJpaEntity;

@Component
public class FavoritePersistenceMapper {

    public Favorite toDomain(FavoriteJpaEntity entity) {
        return new Favorite(entity.getId(), entity.getUserId(), entity.getProductId(), entity.getCreatedAt());
    }

    public FavoriteJpaEntity toEntity(Favorite favorite) {
        FavoriteJpaEntity entity = new FavoriteJpaEntity();
        entity.setId(favorite.getId());
        entity.setUserId(favorite.getUserId());
        entity.setProductId(favorite.getProductId());
        return entity;
    }
}
