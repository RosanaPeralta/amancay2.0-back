package com.amancay.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record PurchaseOrder(UUID id, UUID userId, UUID shippingAddressId, ShippingAddressSnapshot shippingAddress,
        OrderState state, BigDecimal subtotal, BigDecimal shippingCost, BigDecimal total, Instant createdAt,
        Instant updatedAt, List<OrderLine> items) {
    public PurchaseOrder {
        items = items == null ? List.of() : List.copyOf(items);
    }

    public static PurchaseOrder create(UUID userId, UUID addressId, ShippingAddressSnapshot address,
            List<OrderLine> items) {
        BigDecimal subtotal = items.stream().map(OrderLine::subtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal shipping = BigDecimal.ZERO;
        return new PurchaseOrder(null, userId, addressId, address, OrderState.CREADO, subtotal, shipping,
                subtotal.add(shipping), null, null, items);
    }

    public PurchaseOrder transitionTo(OrderState target) {
        if (!state.canTransitionTo(target)) {
            throw new IllegalStateException("Cannot transition order from " + state + " to " + target);
        }
        return new PurchaseOrder(id, userId, shippingAddressId, shippingAddress, target, subtotal, shippingCost,
                total, createdAt, updatedAt, items);
    }
}