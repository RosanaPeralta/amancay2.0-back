package com.amancay.infrastructure.adapter.out.persistence;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.amancay.application.port.out.LoadProductVariantPort;
import com.amancay.domain.model.Discount;
import com.amancay.infrastructure.adapter.out.persistence.entity.ProductVariantJpaEntity;
import com.amancay.infrastructure.adapter.out.persistence.mapper.DiscountPersistenceMapper;
import com.amancay.infrastructure.adapter.out.persistence.repository.SpringDataProductVariantRepository;

@Component
class ProductVariantAdapter implements LoadProductVariantPort {

    private final SpringDataProductVariantRepository productVariantRepository;
    private final DiscountPersistenceMapper discountMapper;

    ProductVariantAdapter(SpringDataProductVariantRepository productVariantRepository,
            DiscountPersistenceMapper discountMapper) {
        this.productVariantRepository = productVariantRepository;
        this.discountMapper = discountMapper;
    }

    @Override
    public Optional<ProductVariantInfo> findById(UUID id) {
        return productVariantRepository.findById(id)
                .map(variant -> new ProductVariantInfo(variant.getId(), finalPrice(variant), variant.getStockQuantity()));
    }

    // Precio con el descuento del producto aplicado, redondeado como se guarda en order_items.
    private BigDecimal finalPrice(ProductVariantJpaEntity variant) {
        Discount discount = discountMapper.toDomain(variant.getProduct().getDiscount());
        BigDecimal price = discount == null ? variant.getPrice() : discount.applyTo(variant.getPrice());
        return price.setScale(2, RoundingMode.HALF_UP);
    }
}
