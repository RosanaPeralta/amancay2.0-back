package com.amancay.application.service.fake;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.amancay.domain.model.Favorite;
import com.amancay.domain.model.PageQuery;
import com.amancay.domain.model.PageResult;
import com.amancay.domain.port.FavoriteRepositoryPort;

public class InMemoryFavoriteRepository implements FavoriteRepositoryPort {

    private final Map<UUID, Favorite> store = new LinkedHashMap<>();
    public final List<String> calls = new ArrayList<>();
    private Instant clock = Instant.parse("2026-01-01T00:00:00Z");

    public int size() {
        return store.size();
    }

    @Override
    public boolean existsByUserIdAndProductId(UUID userId, UUID productId) {
        return findByUserIdAndProductId(userId, productId).isPresent();
    }

    @Override
    public Optional<Favorite> findByUserIdAndProductId(UUID userId, UUID productId) {
        return store.values().stream()
                .filter(f -> f.getUserId().equals(userId) && f.getProductId().equals(productId)).findFirst();
    }

    @Override
    public PageResult<Favorite> findByUserId(UUID userId, PageQuery page) {
        return Pages.of(newestFirst(userId), page);
    }

    @Override
    public List<UUID> findProductIdsByUserId(UUID userId) {
        return newestFirst(userId).stream().map(Favorite::getProductId).toList();
    }

    @Override
    public Favorite save(Favorite favorite) {
        calls.add("save");
        clock = clock.plusSeconds(1);
        Favorite saved = new Favorite(favorite.getId(), favorite.getUserId(), favorite.getProductId(), clock);
        store.put(saved.getId(), saved);
        return saved;
    }

    @Override
    public void delete(Favorite favorite) {
        calls.add("delete");
        store.remove(favorite.getId());
    }

    private List<Favorite> newestFirst(UUID userId) {
        return store.values().stream().filter(f -> f.getUserId().equals(userId))
                .sorted(Comparator.comparing(Favorite::getCreatedAt).reversed()).toList();
    }
}
