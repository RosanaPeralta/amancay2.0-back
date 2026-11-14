package com.amancay.infrastructure.adapter.out.persistence.mapper;

import org.springframework.stereotype.Component;

import com.amancay.domain.model.Payment;
import com.amancay.infrastructure.adapter.out.persistence.entity.PaymentJpaEntity;

@Component
public class PaymentPersistenceMapper {

    public Payment toDomain(PaymentJpaEntity entity) {
        return new Payment(entity.getId(), entity.getOrderId(), entity.getAmount(), entity.getMethod(),
                entity.getStatus(), entity.getTransferReference(), entity.getExternalId(), entity.getCheckoutUrl(),
                entity.getCreatedAt(), entity.getUpdatedAt());
    }

    public PaymentJpaEntity toNewEntity(Payment payment) {
        PaymentJpaEntity entity = new PaymentJpaEntity();
        entity.setOrderId(payment.getOrderId());
        entity.setAmount(payment.getAmount());
        entity.setMethod(payment.getMethod());
        entity.setCheckoutUrl(payment.getCheckoutUrl());
        applyChanges(payment, entity);
        return entity;
    }

    // Lo unico que cambia de un pago existente es su estado, la referencia de la
    // transferencia y el id en Mercado Pago; orden, monto, metodo y checkout quedan fijos.
    public void applyChanges(Payment payment, PaymentJpaEntity entity) {
        entity.setStatus(payment.getStatus());
        entity.setTransferReference(payment.getTransferReference());
        entity.setExternalId(payment.getExternalId());
    }
}
