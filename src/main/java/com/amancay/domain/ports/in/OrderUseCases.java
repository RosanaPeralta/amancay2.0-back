package com.amancay.domain.ports.in;

import java.util.List;
import java.util.UUID;

import com.amancay.domain.model.OrderState;
import com.amancay.domain.model.PurchaseOrder;

public interface OrderUseCases {
    List<PurchaseOrder> list(UUID requesterId, UUID requestedUserId);

    PurchaseOrder get(UUID requesterId, UUID orderId, UUID requestedUserId);

    PurchaseOrder create(UUID requesterId, UUID addressId, List<OrderItemInput> items);

    PurchaseOrder changeStatus(UUID requesterId, UUID orderId, OrderState newState);

    PurchaseOrder changeStatus(UUID orderId, OrderState newState);

    record OrderItemInput(UUID productVariantId, Integer quantity) {
    }
}