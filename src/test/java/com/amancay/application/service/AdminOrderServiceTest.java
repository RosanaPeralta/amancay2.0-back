package com.amancay.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.amancay.application.port.in.ListAllOrdersQuery.AdminOrder;
import com.amancay.domain.model.Order;
import com.amancay.domain.model.OrderFixtures;
import com.amancay.domain.model.OrderStatus;
import com.amancay.domain.port.OrderRepositoryPort;

class AdminOrderServiceTest {

    @Test
    void listAllKeepsRepositoryOrderAndAttachesBuyerEmails() {
        UUID ana = UUID.randomUUID();
        UUID beto = UUID.randomUUID();
        Order newest = OrderFixtures.persisted(UUID.randomUUID(), ana, OrderStatus.CREADO);
        Order older = OrderFixtures.persisted(UUID.randomUUID(), beto, OrderStatus.DESPACHADO);

        AdminOrderService service = new AdminOrderService(ordersReturning(List.of(newest, older)),
                userIds -> Map.of(ana, "ana@amancay.com"));

        List<AdminOrder> result = service.listAll();

        assertThat(result).extracting(adminOrder -> adminOrder.order().getId())
                .containsExactly(newest.getId(), older.getId());
        assertThat(result).extracting(AdminOrder::buyerEmail).containsExactly("ana@amancay.com", null);
    }

    private static OrderRepositoryPort ordersReturning(List<Order> orders) {
        return new OrderRepositoryPort() {
            @Override
            public Optional<Order> findById(UUID id) {
                return Optional.empty();
            }

            @Override
            public List<Order> findByUserId(UUID userId) {
                return List.of();
            }

            @Override
            public List<Order> findAllById(Collection<UUID> ids) {
                return List.of();
            }

            @Override
            public List<Order> findAllNewestFirst() {
                return orders;
            }

            @Override
            public Order save(Order order) {
                return order;
            }
        };
    }
}
