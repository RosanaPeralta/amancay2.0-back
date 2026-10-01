package com.amancay.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.amancay.domain.exception.InvalidOrderStatusTransitionException;

import lombok.Getter;

// Modelo de dominio puro: no sabe nada de JPA ni de Spring. Lo que es propio de
// la base (ids generados, timestamps, el cast al enum nativo de Postgres) vive en
// infrastructure/adapter/out/persistence y se traduce con OrderPersistenceMapper.
@Getter
public class Order {

    private final UUID id;
    private final UUID userId;
    // Referencia a la Address "viva" del usuario, solo para trazabilidad. Los datos
    // que realmente se usan para el envio son el snapshot de shippingAddress.
    private final UUID shippingAddressId;
    private final ShippingAddress shippingAddress;
    private OrderStatus status;
    private final BigDecimal subtotal;
    private final BigDecimal shippingCost;
    private final BigDecimal total;
    private final Instant createdAt;
    private final Instant updatedAt;
    private final List<OrderItem> items;
    private final List<OrderStatusChange> statusHistory;

    // Reconstruye una orden ya existente (lo usa el adaptador de persistencia).
    // Para una orden nueva se usa Order.create, que calcula los totales.
    public Order(UUID id, UUID userId, UUID shippingAddressId, ShippingAddress shippingAddress, OrderStatus status,
            BigDecimal subtotal, BigDecimal shippingCost, BigDecimal total, Instant createdAt, Instant updatedAt,
            List<OrderItem> items, List<OrderStatusChange> statusHistory) {
        this.id = id;
        this.userId = userId;
        this.shippingAddressId = shippingAddressId;
        this.shippingAddress = shippingAddress;
        this.status = status;
        this.subtotal = subtotal;
        this.shippingCost = shippingCost;
        this.total = total;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.items = List.copyOf(items);
        this.statusHistory = new ArrayList<>(statusHistory);
    }

    public static Order create(UUID userId, UUID shippingAddressId, ShippingAddress shippingAddress,
            List<OrderItem> items) {
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("An order needs at least one item");
        }
        BigDecimal subtotal = items.stream().map(OrderItem::subtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal shippingCost = BigDecimal.ZERO;
        return new Order(null, userId, shippingAddressId, shippingAddress, OrderStatus.CREADO, subtotal, shippingCost,
                subtotal.add(shippingCost), null, null, items, List.of());
    }

    // Valida la transicion contra el propio OrderStatus (patron State) y deja
    // registro en el historial. No dispara nada asincrono: eso lo hace
    // OrderService despues de persistir.
    public void changeStatus(OrderStatus newStatus) {
        if (!status.canTransitionTo(newStatus)) {
            throw new InvalidOrderStatusTransitionException(status, newStatus);
        }
        this.status = newStatus;
        statusHistory.add(OrderStatusChange.of(newStatus));
    }

    public boolean belongsTo(UUID userId) {
        return this.userId.equals(userId);
    }

    public List<OrderStatusChange> getStatusHistory() {
        return List.copyOf(statusHistory);
    }
}
