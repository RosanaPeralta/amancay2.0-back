package com.amancay.domain.ports.out;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.amancay.domain.model.Favorite;

public interface FavoriteCatalogPort {
    boolean exists(UUID userId, UUID productId);

    Favorite save(UUID userId, UUID productId);

    Optional<Favorite> find(UUID userId, UUID productId);

    void delete(Favorite favorite);

    FavoriteSlice findByUser(UUID userId, int page, int size);

    List<UUID> findProductIds(UUID userId);

    record FavoriteSlice(List<Favorite> content, int page, int size, long totalElements, int totalPages) {
        public FavoriteSlice {
            content = List.copyOf(content);
        }
    }
}