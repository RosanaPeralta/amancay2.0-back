package com.amancay.order.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

// Ordenes de dominio listas para usar en tests, sin pasar por JPA.
public final class OrderFixtures {

    public static final ShippingAddress ADDRESS = new ShippingAddress("Calle", "1", null, "Ciudad", null, "Pais",
            null);

    private OrderFixtures() {
    }

    public static Order newOrder() {
        return Order.create(UUID.randomUUID(), UUID.randomUUID(), ADDRESS,
                List.of(OrderItem.create(UUID.randomUUID(), 1, new BigDecimal("10.00"))));
    }

    public static Order persisted(UUID id, UUID userId, OrderStatus status, BigDecimal total, List<OrderItem> items) {
        return new Order(id, userId, UUID.randomUUID(), ADDRESS, status, total, BigDecimal.ZERO, total,
                Instant.now(), Instant.now(), items, List.of());
    }

    public static Order persisted(UUID id, UUID userId, OrderStatus status) {
        return persisted(id, userId, status, new BigDecimal("100.00"), List.of());
    }
}
