package com.amancay.infrastructure.adapters.in.rest.mapper;

import com.amancay.domain.model.PurchaseOrder;
import com.amancay.dto.OrderDto;
import com.amancay.dto.OrderItemDto;

public final class OrderApiMapper {
    private OrderApiMapper() {
    }

    public static OrderDto toDto(PurchaseOrder order) {
        return new OrderDto(order.id(), order.userId(), order.shippingAddressId(),
                com.amancay.entity.OrderStatus.valueOf(order.state().name()), order.subtotal(), order.shippingCost(),
                order.total(), order.createdAt(), order.updatedAt(), order.items().stream()
                        .map(item -> new OrderItemDto(item.id(), item.productVariantId(), item.quantity(),
                                item.unitPrice(), item.subtotal()))
                        .toList());
    }
}