package com.amancay.infrastructure.adapter.out.persistence.mapper;

import org.springframework.stereotype.Component;

import com.amancay.domain.model.Order;
import com.amancay.domain.model.OrderItem;
import com.amancay.domain.model.OrderStatusChange;
import com.amancay.domain.model.ShippingAddress;
import com.amancay.infrastructure.adapter.out.persistence.entity.OrderItemJpaEntity;
import com.amancay.infrastructure.adapter.out.persistence.entity.OrderJpaEntity;
import com.amancay.infrastructure.adapter.out.persistence.entity.OrderStatusHistoryJpaEntity;
import com.amancay.infrastructure.adapter.out.persistence.entity.ShippingAddressEmbeddable;

@Component
public class OrderPersistenceMapper {

    public Order toDomain(OrderJpaEntity entity) {
        return new Order(entity.getId(), entity.getUserId(), entity.getShippingAddressId(),
                toDomain(entity.getShippingAddress()), entity.getStatus(), entity.getSubtotal(),
                entity.getShippingCost(), entity.getTotal(), entity.getCreatedAt(), entity.getUpdatedAt(),
                entity.getItems().stream()
                        .map(item -> new OrderItem(item.getId(), item.getProductVariantId(), item.getQuantity(),
                                item.getUnitPrice()))
                        .toList(),
                entity.getStatusHistory().stream()
                        .map(history -> new OrderStatusChange(history.getId(), history.getStatus(),
                                history.getChangedAt()))
                        .toList());
    }

    public OrderJpaEntity toNewEntity(Order order) {
        OrderJpaEntity entity = new OrderJpaEntity();
        entity.setUserId(order.getUserId());
        entity.setShippingAddressId(order.getShippingAddressId());
        entity.setShippingAddress(toEmbeddable(order.getShippingAddress()));
        entity.setStatus(order.getStatus());
        entity.setSubtotal(order.getSubtotal());
        entity.setShippingCost(order.getShippingCost());
        entity.setTotal(order.getTotal());
        for (OrderItem item : order.getItems()) {
            OrderItemJpaEntity itemEntity = new OrderItemJpaEntity();
            itemEntity.setProductVariantId(item.productVariantId());
            itemEntity.setQuantity(item.quantity());
            itemEntity.setUnitPrice(item.unitPrice());
            entity.createItem(itemEntity);
        }
        appendNewStatusHistory(order, entity);
        return entity;
    }

    // Una orden existente solo cambia de estado y suma entradas nuevas (sin id) al historial.
    public void applyChanges(Order order, OrderJpaEntity entity) {
        entity.setStatus(order.getStatus());
        appendNewStatusHistory(order, entity);
    }

    private void appendNewStatusHistory(Order order, OrderJpaEntity entity) {
        order.getStatusHistory().stream()
                .filter(OrderStatusChange::isNew)
                .forEach(change -> {
                    OrderStatusHistoryJpaEntity history = new OrderStatusHistoryJpaEntity();
                    history.setStatus(change.status());
                    entity.createStatusHistory(history);
                });
    }

    private ShippingAddress toDomain(ShippingAddressEmbeddable embeddable) {
        if (embeddable == null) {
            return null;
        }
        return new ShippingAddress(embeddable.getStreet(), embeddable.getNumber(), embeddable.getFloorApt(),
                embeddable.getCity(), embeddable.getProvince(), embeddable.getCountry(), embeddable.getPostalCode());
    }

    private ShippingAddressEmbeddable toEmbeddable(ShippingAddress address) {
        ShippingAddressEmbeddable embeddable = new ShippingAddressEmbeddable();
        embeddable.setStreet(address.street());
        embeddable.setNumber(address.number());
        embeddable.setFloorApt(address.floorApt());
        embeddable.setCity(address.city());
        embeddable.setProvince(address.province());
        embeddable.setCountry(address.country());
        embeddable.setPostalCode(address.postalCode());
        return embeddable;
    }
}
