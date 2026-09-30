package com.amancay.infrastructure.adapters.out.persistence.mapper;

import com.amancay.domain.model.Discount;

public final class DiscountPersistenceMapper {
    private DiscountPersistenceMapper() {
    }

    public static Discount toDomain(com.amancay.entity.Discount entity) {
        return new Discount(entity.getId(), entity.getPercentage(), entity.getDescription(), !entity.getProducts().isEmpty());
    }

    public static com.amancay.entity.Discount toPersistence(Discount discount,
            com.amancay.entity.Discount entity) {
        entity.setPercentage(discount.percentage());
        entity.setDescription(discount.description());
        return entity;
    }
}