package com.amancay.application.service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.amancay.domain.model.Favorite;
import com.amancay.domain.model.FavoritePage;
import com.amancay.domain.model.FavoriteProduct;
import com.amancay.domain.model.ProductSummary;
import com.amancay.domain.ports.in.FavoriteUseCases;
import com.amancay.domain.ports.out.FavoriteCatalogPort;
import com.amancay.domain.ports.out.FavoriteCatalogPort.FavoriteSlice;
import com.amancay.domain.ports.out.FavoriteProductLookupPort;
import com.amancay.exceptions.DuplicateFavoriteException;
import com.amancay.exceptions.FavoriteNotFoundException;
import com.amancay.exceptions.ProductNotFoundException;

public class FavoriteApplicationService implements FavoriteUseCases {
    private final FavoriteCatalogPort favoriteCatalog;
    private final FavoriteProductLookupPort productLookup;

    public FavoriteApplicationService(FavoriteCatalogPort favoriteCatalog, FavoriteProductLookupPort productLookup) {
        this.favoriteCatalog = favoriteCatalog;
        this.productLookup = productLookup;
    }

    @Override
    public FavoriteProduct add(UUID userId, UUID productId) {
        ProductSummary product = productLookup.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException(productId));
        if (favoriteCatalog.exists(userId, productId)) {
            throw new DuplicateFavoriteException(productId);
        }
        Favorite favorite = favoriteCatalog.save(userId, productId);
        return new FavoriteProduct(productId, product.name(), product.slug(), product.active(), favorite.createdAt());
    }

    @Override
    public void remove(UUID userId, UUID productId) {
        Favorite favorite = favoriteCatalog.find(userId, productId)
                .orElseThrow(() -> new FavoriteNotFoundException(productId));
        favoriteCatalog.delete(favorite);
    }

    @Override
    public FavoritePage list(UUID userId, int page, int size) {
        FavoriteSlice favorites = favoriteCatalog.findByUser(userId, page, size);
        Set<UUID> productIds = favorites.content().stream().map(Favorite::productId).collect(Collectors.toSet());
        Map<UUID, ProductSummary> products = productLookup.findAll(productIds).stream()
                .collect(Collectors.toMap(ProductSummary::id, Function.identity()));
        List<FavoriteProduct> content = favorites.content().stream().map(favorite -> {
            ProductSummary product = products.get(favorite.productId());
            return new FavoriteProduct(favorite.productId(), product.name(), product.slug(), product.active(),
                    favorite.createdAt());
        }).toList();
        return new FavoritePage(content, favorites.page(), favorites.size(), favorites.totalElements(),
                favorites.totalPages());
    }

    @Override
    public List<UUID> listProductIds(UUID userId) {
        return favoriteCatalog.findProductIds(userId);
    }
}