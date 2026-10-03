package com.amancay.infrastructure.adapter.out.persistence.mapper;

import org.springframework.stereotype.Component;

import com.amancay.domain.model.Discount;
import com.amancay.infrastructure.adapter.out.persistence.entity.DiscountJpaEntity;

@Component
public class DiscountPersistenceMapper {

    public Discount toDomain(DiscountJpaEntity entity) {
        return entity == null ? null : new Discount(entity.getId(), entity.getPercentage(), entity.getDescription());
    }

    public void copyToEntity(Discount discount, DiscountJpaEntity entity) {
        entity.setPercentage(discount.getPercentage());
        entity.setDescription(discount.getDescription());
    }
}
