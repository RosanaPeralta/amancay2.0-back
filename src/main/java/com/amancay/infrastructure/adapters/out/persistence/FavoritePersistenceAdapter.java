package com.amancay.infrastructure.adapters.out.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.amancay.domain.model.Favorite;
import com.amancay.domain.ports.out.FavoriteCatalogPort;
import com.amancay.domain.ports.out.FavoriteCatalogPort.FavoriteSlice;
import com.amancay.infrastructure.adapters.out.persistence.mapper.FavoritePersistenceMapper;
import com.amancay.infrastructure.adapters.out.persistence.repository.FavoriteRepository;

@Repository
public class FavoritePersistenceAdapter implements FavoriteCatalogPort {
    private final FavoriteRepository repository;

    public FavoritePersistenceAdapter(FavoriteRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean exists(UUID userId, UUID productId) {
        return repository.existsByUserIdAndProductId(userId, productId);
    }

    @Override
    @Transactional
    public Favorite save(UUID userId, UUID productId) {
        return FavoritePersistenceMapper.toDomain(repository.saveAndFlush(com.amancay.entity.Favorite.of(userId, productId)));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Favorite> find(UUID userId, UUID productId) {
        return repository.findByUserIdAndProductId(userId, productId).map(FavoritePersistenceMapper::toDomain);
    }

    @Override
    @Transactional
    public void delete(Favorite favorite) {
        repository.deleteById(favorite.id());
    }

    @Override
    @Transactional(readOnly = true)
    public FavoriteSlice findByUser(UUID userId, int page, int size) {
        Page<com.amancay.entity.Favorite> result = repository.findByUserId(userId, PageRequest.of(page, size));
        return new FavoriteSlice(result.getContent().stream().map(FavoritePersistenceMapper::toDomain).toList(),
                result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages());
    }

    @Override
    @Transactional(readOnly = true)
    public List<UUID> findProductIds(UUID userId) {
        return repository.findProductIdsByUserId(userId);
    }
}