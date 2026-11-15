package com.amancay.application.service.fake;

import java.time.Instant;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.amancay.application.port.out.OrderRepositoryPort;
import com.amancay.domain.model.Order;
import com.amancay.domain.model.OrderStatusChange;

// Imita lo que hace la base: asigna ids y timestamps al guardar, y devuelve una
// copia para que el test no comparta la instancia con el servicio.
public final class InMemoryOrderRepository implements OrderRepositoryPort {

    private final Map<UUID, Order> store = new HashMap<>();

    public void put(Order order) {
        store.put(order.getId(), order);
    }

    @Override
    public Optional<Order> findById(UUID id) {
        return Optional.ofNullable(store.get(id)).map(InMemoryOrderRepository::copy);
    }

    @Override
    public List<Order> findByUserId(UUID userId) {
        return store.values().stream().filter(order -> order.belongsTo(userId)).map(InMemoryOrderRepository::copy)
                .toList();
    }

    @Override
    public List<Order> findAllById(Collection<UUID> ids) {
        return ids.stream().map(store::get).filter(order -> order != null).map(InMemoryOrderRepository::copy).toList();
    }

    @Override
    public List<Order> findAllNewestFirst() {
        return store.values().stream().sorted(Comparator.comparing(Order::getCreatedAt).reversed())
                .map(InMemoryOrderRepository::copy).toList();
    }

    @Override
    public Order save(Order order) {
        UUID id = order.getId() == null ? UUID.randomUUID() : order.getId();
        Instant now = Instant.now();
        Order saved = new Order(id, order.getUserId(), order.getShippingAddressId(), order.getShippingAddress(),
                order.getStatus(), order.getSubtotal(), order.getShippingCost(), order.getTotal(),
                order.getCreatedAt() == null ? now : order.getCreatedAt(), now, order.getItems(),
                order.getStatusHistory().stream()
                        .map(change -> change.isNew()
                                ? new OrderStatusChange(UUID.randomUUID(), change.status(), now)
                                : change)
                        .toList());
        store.put(id, saved);
        return copy(saved);
    }

    private static Order copy(Order order) {
        return new Order(order.getId(), order.getUserId(), order.getShippingAddressId(),
                order.getShippingAddress(), order.getStatus(), order.getSubtotal(), order.getShippingCost(),
                order.getTotal(), order.getCreatedAt(), order.getUpdatedAt(), order.getItems(),
                order.getStatusHistory());
    }
}
