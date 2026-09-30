package com.amancay.infrastructure.adapters.out.persistence.mapper;

import com.amancay.domain.model.PaymentMethodType;
import com.amancay.domain.model.PaymentRecord;
import com.amancay.domain.model.PaymentState;

public final class PaymentPersistenceMapper {
    private PaymentPersistenceMapper() {
    }

    public static PaymentRecord toDomain(com.amancay.entity.Payment entity) {
        return new PaymentRecord(entity.getId(), entity.getOrder().getId(), entity.getOrder().getUserId(),
                entity.getAmount(), PaymentMethodType.valueOf(entity.getMethod().name()),
                PaymentState.valueOf(entity.getStatus().name()), entity.getTransferReference(), entity.getCreatedAt(),
                null);
    }

    public static com.amancay.entity.Payment toPersistence(PaymentRecord payment,
            com.amancay.entity.Payment entity, com.amancay.entity.Order order) {
        if (entity == null) {
            entity = new com.amancay.entity.Payment();
        }
        entity.setOrder(order);
        entity.setAmount(payment.amount());
        entity.setMethod(com.amancay.entity.PaymentMethod.valueOf(payment.method().name()));
        entity.setStatus(com.amancay.entity.PaymentStatus.valueOf(payment.state().name()));
        entity.setTransferReference(payment.transferReference());
        return entity;
    }
}