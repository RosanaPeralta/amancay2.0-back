package com.amancay.infrastructure.adapter.out.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.amancay.application.port.out.FavoriteRepositoryPort;
import com.amancay.domain.model.Favorite;
import com.amancay.domain.model.PageQuery;
import com.amancay.domain.model.PageResult;
import com.amancay.infrastructure.adapter.out.persistence.mapper.FavoritePersistenceMapper;
import com.amancay.infrastructure.adapter.out.persistence.mapper.PageMapper;
import com.amancay.infrastructure.adapter.out.persistence.repository.SpringDataFavoriteRepository;

@Component
class FavoriteRepositoryAdapter implements FavoriteRepositoryPort {

    private final SpringDataFavoriteRepository repository;
    private final FavoritePersistenceMapper mapper;

    FavoriteRepositoryAdapter(SpringDataFavoriteRepository repository, FavoritePersistenceMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public boolean existsByUserIdAndProductId(UUID userId, UUID productId) {
        return repository.existsByUserIdAndProductId(userId, productId);
    }

    @Override
    public Optional<Favorite> findByUserIdAndProductId(UUID userId, UUID productId) {
        return repository.findByUserIdAndProductId(userId, productId).map(mapper::toDomain);
    }

    @Override
    public PageResult<Favorite> findByUserId(UUID userId, PageQuery page) {
        return PageMapper.toPageResult(
                repository.findByUserId(userId, PageMapper.toPageable(page, PageMapper.NEWEST_FIRST)),
                mapper::toDomain);
    }

    @Override
    public List<UUID> findProductIdsByUserId(UUID userId) {
        return repository.findProductIdsByUserId(userId);
    }

    @Override
    public Favorite save(Favorite favorite) {
        return mapper.toDomain(repository.saveAndFlush(mapper.toEntity(favorite)));
    }

    @Override
    public void delete(Favorite favorite) {
        repository.deleteById(favorite.getId());
    }
}
