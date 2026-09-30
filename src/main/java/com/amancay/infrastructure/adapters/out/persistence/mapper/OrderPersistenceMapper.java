package com.amancay.infrastructure.adapters.out.persistence.mapper;

import java.util.List;

import com.amancay.domain.model.OrderLine;
import com.amancay.domain.model.OrderState;
import com.amancay.domain.model.PurchaseOrder;
import com.amancay.domain.model.ShippingAddressSnapshot;

public final class OrderPersistenceMapper {
    private OrderPersistenceMapper() {
    }

    public static PurchaseOrder toDomain(com.amancay.entity.Order entity) {
        ShippingAddressSnapshot address = entity.getShippingAddress() == null ? null
                : new ShippingAddressSnapshot(entity.getShippingAddress().getStreet(),
                        entity.getShippingAddress().getNumber(), entity.getShippingAddress().getFloorApt(),
                        entity.getShippingAddress().getCity(), entity.getShippingAddress().getProvince(),
                        entity.getShippingAddress().getCountry(), entity.getShippingAddress().getPostalCode());
        List<OrderLine> items = entity.getItems() == null ? List.of() : entity.getItems().stream()
                .map(item -> new OrderLine(item.getId(), item.getProductVariant().getId(), item.getQuantity(),
                        item.getUnitPrice())).toList();
        return new PurchaseOrder(entity.getId(), entity.getUserId(), entity.getShippingAddressId(), address,
                OrderState.valueOf(entity.getStatus().name()), entity.getSubtotal(), entity.getShippingCost(),
                entity.getTotal(), entity.getCreatedAt(), entity.getUpdatedAt(), items);
    }

    public static com.amancay.entity.Order toPersistence(PurchaseOrder order,
            com.amancay.entity.Order entity,
            com.amancay.infrastructure.adapters.out.persistence.repository.ProductVariantRepository variants) {
        entity.setUserId(order.userId());
        entity.setShippingAddressId(order.shippingAddressId());
        com.amancay.entity.OrderStatus targetStatus = com.amancay.entity.OrderStatus.valueOf(order.state().name());
        if (entity.getStatus() != targetStatus) {
            entity.changeStatus(targetStatus);
        }
        entity.setSubtotal(order.subtotal());
        entity.setShippingCost(order.shippingCost());
        entity.setTotal(order.total());
        if (order.shippingAddress() != null) {
            com.amancay.entity.ShippingAddress shipping = new com.amancay.entity.ShippingAddress();
            shipping.setStreet(order.shippingAddress().street());
            shipping.setNumber(order.shippingAddress().number());
            shipping.setFloorApt(order.shippingAddress().floorApt());
            shipping.setCity(order.shippingAddress().city());
            shipping.setProvince(order.shippingAddress().province());
            shipping.setCountry(order.shippingAddress().country());
            shipping.setPostalCode(order.shippingAddress().postalCode());
            entity.setShippingAddress(shipping);
        }
        if (entity.getId() == null) {
            for (OrderLine line : order.items()) {
                com.amancay.entity.OrderItem item = new com.amancay.entity.OrderItem();
                item.setProductVariant(variants.findById(line.productVariantId())
                        .orElseThrow(() -> new IllegalArgumentException("Product variant not found: " + line.productVariantId())));
                item.setQuantity(line.quantity());
                item.setUnitPrice(line.unitPrice());
                entity.addItem(item);
            }
        }
        return entity;
    }
}