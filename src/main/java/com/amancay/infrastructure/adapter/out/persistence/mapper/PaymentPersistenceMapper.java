package com.amancay.infrastructure.adapter.out.persistence.mapper;

import org.springframework.stereotype.Component;

import com.amancay.domain.model.Payment;
import com.amancay.infrastructure.adapter.out.persistence.entity.PaymentJpaEntity;

@Component
public class PaymentPersistenceMapper {

    public Payment toDomain(PaymentJpaEntity entity) {
        return new Payment(entity.getId(), entity.getOrderId(), entity.getAmount(), entity.getMethod(),
                entity.getStatus(), entity.getTransferReference(), entity.getCreatedAt(), entity.getUpdatedAt());
    }

    public PaymentJpaEntity toNewEntity(Payment payment) {
        PaymentJpaEntity entity = new PaymentJpaEntity();
        entity.setOrderId(payment.getOrderId());
        entity.setAmount(payment.getAmount());
        entity.setMethod(payment.getMethod());
        applyChanges(payment, entity);
        return entity;
    }

    // Lo unico que cambia de un pago existente es su estado y la referencia de la
    // transferencia; orden, monto y metodo quedan fijos al crearlo.
    public void applyChanges(Payment payment, PaymentJpaEntity entity) {
        entity.setStatus(payment.getStatus());
        entity.setTransferReference(payment.getTransferReference());
    }
}
