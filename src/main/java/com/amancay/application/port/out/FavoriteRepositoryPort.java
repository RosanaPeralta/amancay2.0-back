package com.amancay.application.port.out;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.amancay.domain.model.Favorite;
import com.amancay.domain.model.PageQuery;
import com.amancay.domain.model.PageResult;

public interface FavoriteRepositoryPort {
    boolean existsByUserIdAndProductId(UUID userId, UUID productId);

    Optional<Favorite> findByUserIdAndProductId(UUID userId, UUID productId);

    // Mas nuevos primero.
    PageResult<Favorite> findByUserId(UUID userId, PageQuery page);

    // Mas nuevos primero.
    List<UUID> findProductIdsByUserId(UUID userId);

    Favorite save(Favorite favorite);

    void delete(Favorite favorite);
}
