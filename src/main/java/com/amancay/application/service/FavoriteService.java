package com.amancay.application.service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.amancay.application.port.in.AddFavoriteUseCase;
import com.amancay.application.port.in.FavoriteProduct;
import com.amancay.application.port.in.ListFavoritesQuery;
import com.amancay.application.port.in.RemoveFavoriteUseCase;
import com.amancay.application.port.out.FavoriteRepositoryPort;
import com.amancay.application.port.out.ProductRepositoryPort;
import com.amancay.domain.exception.DuplicateFavoriteException;
import com.amancay.domain.exception.FavoriteNotFoundException;
import com.amancay.domain.exception.ProductNotFoundException;
import com.amancay.domain.model.Favorite;
import com.amancay.domain.model.PageQuery;
import com.amancay.domain.model.PageResult;
import com.amancay.domain.model.ProductSummary;

@Service
public class FavoriteService implements AddFavoriteUseCase, RemoveFavoriteUseCase, ListFavoritesQuery {

    private final FavoriteRepositoryPort favoriteRepository;
    private final ProductRepositoryPort productRepository;

    public FavoriteService(FavoriteRepositoryPort favoriteRepository, ProductRepositoryPort productRepository) {
        this.favoriteRepository = favoriteRepository;
        this.productRepository = productRepository;
    }

    @Override
    @Transactional
    public FavoriteProduct add(UUID userId, UUID productId) {
        ProductSummary product = productRepository.findSummaryById(productId)
                .orElseThrow(() -> new ProductNotFoundException(productId));
        if (favoriteRepository.existsByUserIdAndProductId(userId, productId)) {
            throw new DuplicateFavoriteException(productId);
        }
        return new FavoriteProduct(favoriteRepository.save(Favorite.of(userId, productId)), product);
    }

    @Override
    @Transactional
    public void remove(UUID userId, UUID productId) {
        Favorite favorite = favoriteRepository.findByUserIdAndProductId(userId, productId)
                .orElseThrow(() -> new FavoriteNotFoundException(productId));
        favoriteRepository.delete(favorite);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<FavoriteProduct> list(UUID userId, PageQuery page) {
        PageResult<Favorite> favorites = favoriteRepository.findByUserId(userId, page);
        Set<UUID> productIds = favorites.content().stream().map(Favorite::getProductId).collect(Collectors.toSet());
        Map<UUID, ProductSummary> productsById = productRepository.findSummariesById(productIds).stream()
                .collect(Collectors.toMap(ProductSummary::id, Function.identity()));
        // El producto nunca falta: la FK con ON DELETE CASCADE borra el favorito junto con el producto.
        return favorites.map(favorite -> new FavoriteProduct(favorite, productsById.get(favorite.getProductId())));
    }

    @Override
    @Transactional(readOnly = true)
    public List<UUID> listProductIds(UUID userId) {
        return favoriteRepository.findProductIdsByUserId(userId);
    }
}
