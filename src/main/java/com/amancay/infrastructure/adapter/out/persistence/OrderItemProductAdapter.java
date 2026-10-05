package com.amancay.infrastructure.adapter.out.persistence;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.amancay.application.port.out.LoadOrderItemProductsPort;
import com.amancay.domain.model.OrderItemProduct;
import com.amancay.infrastructure.adapter.out.persistence.entity.ProductImageJpaEntity;
import com.amancay.infrastructure.adapter.out.persistence.entity.ProductJpaEntity;
import com.amancay.infrastructure.adapter.out.persistence.entity.ProductVariantJpaEntity;
import com.amancay.infrastructure.adapter.out.persistence.repository.SpringDataProductVariantRepository;

@Component
class OrderItemProductAdapter implements LoadOrderItemProductsPort {

    private final SpringDataProductVariantRepository variantRepository;

    OrderItemProductAdapter(SpringDataProductVariantRepository variantRepository) {
        this.variantRepository = variantRepository;
    }

    @Override
    public Map<UUID, OrderItemProduct> findByVariantIds(Set<UUID> variantIds) {
        return variantRepository.findWithProductByIdIn(variantIds).stream()
                .collect(Collectors.toMap(ProductVariantJpaEntity::getId, OrderItemProductAdapter::toOrderItemProduct));
    }

    private static OrderItemProduct toOrderItemProduct(ProductVariantJpaEntity variant) {
        ProductJpaEntity product = variant.getProduct();
        // La primera imagen es la principal, igual que en el catalogo (el front usa images[0]).
        String imageUrl = product.getImages().stream()
                .findFirst()
                .map(ProductImageJpaEntity::getImageUrl)
                .orElse(null);
        return new OrderItemProduct(variant.getId(), product.getId(), product.getName(), imageUrl);
    }
}
