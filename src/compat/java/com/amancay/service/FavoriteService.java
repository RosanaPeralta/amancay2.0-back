package com.amancay.service;

import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.amancay.application.service.FavoriteApplicationService;
import com.amancay.domain.model.FavoritePage;
import com.amancay.domain.ports.in.FavoriteUseCases;
import com.amancay.domain.ports.out.FavoriteCatalogPort;
import com.amancay.domain.ports.out.FavoriteProductLookupPort;
import com.amancay.dto.FavoriteDto;
import com.amancay.dto.PageResponse;
import com.amancay.infrastructure.adapters.out.persistence.FavoritePersistenceAdapter;
import com.amancay.infrastructure.adapters.out.persistence.FavoriteProductPersistenceAdapter;
import com.amancay.repository.FavoriteRepository;
import com.amancay.repository.ProductRepository;

@Service
public class FavoriteService {
    private final FavoriteUseCases favoriteUseCases;

    @Autowired
    public FavoriteService(FavoriteUseCases favoriteUseCases) {
        this.favoriteUseCases = favoriteUseCases;
    }

    public FavoriteService(FavoriteRepository favoriteRepository, ProductRepository productRepository) {
        FavoriteCatalogPort catalog = new FavoritePersistenceAdapter(favoriteRepository);
        FavoriteProductLookupPort products = new FavoriteProductPersistenceAdapter(productRepository);
        this.favoriteUseCases = new FavoriteApplicationService(catalog, products);
    }

    @Transactional
    public FavoriteDto add(UUID userId, UUID productId) {
        com.amancay.domain.model.FavoriteProduct favorite = favoriteUseCases.add(userId, productId);
        return new FavoriteDto(favorite.productId(), favorite.name(), favorite.slug(), favorite.active(),
            favorite.createdAt());
    }

    @Transactional
    public void remove(UUID userId, UUID productId) {
        favoriteUseCases.remove(userId, productId);
    }

    /** Página de favoritos con los datos del producto resueltos en una sola consulta. */
    @Transactional(readOnly = true)
    public PageResponse<FavoriteDto> list(UUID userId, Pageable pageable) {
        FavoritePage page = favoriteUseCases.list(userId, pageable.getPageNumber(), pageable.getPageSize());
        List<FavoriteDto> content = page.content().stream().map(favorite -> new FavoriteDto(favorite.productId(),
            favorite.name(), favorite.slug(), favorite.active(), favorite.createdAt())).toList();
        return new PageResponse<>(content, page.page(), page.size(), page.totalElements(), page.totalPages());
    }

    /** Solo los ids, para que el front marque el corazón en el catálogo sin paginar. */
    @Transactional(readOnly = true)
    public List<UUID> listProductIds(UUID userId) {
        return favoriteUseCases.listProductIds(userId);
    }
}
