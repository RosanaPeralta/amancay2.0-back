package com.amancay.infrastructure.adapter.out.persistence.mapper;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.amancay.domain.model.Product;
import com.amancay.domain.model.ProductImage;
import com.amancay.domain.model.ProductSummary;
import com.amancay.domain.model.ProductVariant;
import com.amancay.infrastructure.adapter.out.persistence.entity.ProductImageJpaEntity;
import com.amancay.infrastructure.adapter.out.persistence.entity.ProductJpaEntity;
import com.amancay.infrastructure.adapter.out.persistence.entity.ProductVariantJpaEntity;

@Component
public class ProductPersistenceMapper {

    private final DiscountPersistenceMapper discountMapper;

    public ProductPersistenceMapper(DiscountPersistenceMapper discountMapper) {
        this.discountMapper = discountMapper;
    }

    public Product toDomain(ProductJpaEntity entity) {
        return new Product(entity.getId(), entity.getName(), entity.getSlug(), entity.getShortDescription(),
                entity.getDescription(), entity.isActive(), entity.getCreatedAt(), entity.getUpdatedAt(),
                entity.getVariants().stream()
                        .map(variant -> new ProductVariant(variant.getId(), variant.getPrice(),
                                variant.getStockQuantity()))
                        .toList(),
                entity.getImages().stream().map(image -> new ProductImage(image.getId(), image.getImageUrl()))
                        .toList(),
                entity.getCategoryIds(), discountMapper.toDomain(entity.getDiscount()));
    }

    public ProductSummary toSummary(ProductJpaEntity entity) {
        return new ProductSummary(entity.getId(), entity.getName(), entity.getSlug(), entity.isActive(),
                entity.getCreatedAt(), entity.getUpdatedAt());
    }

    // Variantes e imagenes se sincronizan por id; el descuento lo resuelve el adaptador.
    public void copyToEntity(Product product, ProductJpaEntity entity) {
        entity.setName(product.getName());
        entity.setSlug(product.getSlug());
        entity.setShortDescription(product.getShortDescription());
        entity.setDescription(product.getDescription());
        entity.setActive(product.isActive());
        entity.setCategoryIds(new HashSet<>(product.getCategoryIds()));

        Map<UUID, ProductVariantJpaEntity> variantsById = entity.getVariants().stream()
                .collect(Collectors.toMap(ProductVariantJpaEntity::getId, Function.identity()));
        Set<UUID> keptVariants = new HashSet<>();
        for (ProductVariant variant : product.getVariants()) {
            ProductVariantJpaEntity target;
            if (variant.getId() == null) {
                target = new ProductVariantJpaEntity();
                entity.createVariant(target);
            } else {
                target = variantsById.get(variant.getId());
                keptVariants.add(variant.getId());
            }
            target.setPrice(variant.getPrice());
            target.setStockQuantity(variant.getStockQuantity());
        }
        entity.getVariants().removeIf(variant -> variant.getId() != null && !keptVariants.contains(variant.getId()));

        Map<UUID, ProductImageJpaEntity> imagesById = entity.getImages().stream()
                .collect(Collectors.toMap(ProductImageJpaEntity::getId, Function.identity()));
        Set<UUID> keptImages = new HashSet<>();
        for (ProductImage image : product.getImages()) {
            ProductImageJpaEntity target;
            if (image.getId() == null) {
                target = new ProductImageJpaEntity();
                entity.createImage(target);
            } else {
                target = imagesById.get(image.getId());
                keptImages.add(image.getId());
            }
            target.setImageUrl(image.getImageUrl());
        }
        entity.getImages().removeIf(image -> image.getId() != null && !keptImages.contains(image.getId()));
    }
}
