package com.amancay.service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.amancay.dto.FavoriteDto;
import com.amancay.dto.PageResponse;
import com.amancay.entity.Favorite;
import com.amancay.entity.Product;
import com.amancay.exceptions.DuplicateFavoriteException;
import com.amancay.exceptions.FavoriteNotFoundException;
import com.amancay.exceptions.ProductNotFoundException;
import com.amancay.repository.FavoriteRepository;
import com.amancay.repository.ProductRepository;

@Service
public class FavoriteService {
    private final FavoriteRepository favoriteRepository;
    private final ProductRepository productRepository;

    public FavoriteService(FavoriteRepository favoriteRepository, ProductRepository productRepository) {
        this.favoriteRepository = favoriteRepository;
        this.productRepository = productRepository;
    }

    @Transactional
    public FavoriteDto add(UUID userId, UUID productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException(productId));
        if (favoriteRepository.existsByUserIdAndProductId(userId, productId)) {
            throw new DuplicateFavoriteException(productId);
        }
        Favorite favorite = favoriteRepository.saveAndFlush(Favorite.of(userId, productId));
        return toDto(favorite, product);
    }

    @Transactional
    public void remove(UUID userId, UUID productId) {
        Favorite favorite = favoriteRepository.findByUserIdAndProductId(userId, productId)
                .orElseThrow(() -> new FavoriteNotFoundException(productId));
        favoriteRepository.delete(favorite);
    }

    /** Página de favoritos con los datos del producto resueltos en una sola consulta. */
    @Transactional(readOnly = true)
    public PageResponse<FavoriteDto> list(UUID userId, Pageable pageable) {
        Page<Favorite> favorites = favoriteRepository.findByUserId(userId, pageable);
        Set<UUID> productIds = favorites.getContent().stream().map(Favorite::getProductId)
                .collect(Collectors.toSet());
        Map<UUID, Product> productsById = productRepository.findAllById(productIds).stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));
        List<FavoriteDto> content = favorites.getContent().stream()
                .map(favorite -> toDto(favorite, productsById.get(favorite.getProductId())))
                .toList();
        return new PageResponse<>(content, favorites.getNumber(), favorites.getSize(),
                favorites.getTotalElements(), favorites.getTotalPages());
    }

    /** Solo los ids, para que el front marque el corazón en el catálogo sin paginar. */
    @Transactional(readOnly = true)
    public List<UUID> listProductIds(UUID userId) {
        return favoriteRepository.findProductIdsByUserId(userId);
    }

    private FavoriteDto toDto(Favorite favorite, Product product) {
        // product nunca es null: la FK con ON DELETE CASCADE borra el favorito junto con el producto.
        return new FavoriteDto(favorite.getProductId(), product.getName(), product.getSlug(), product.isActive(),
                favorite.getCreatedAt());
    }
}
