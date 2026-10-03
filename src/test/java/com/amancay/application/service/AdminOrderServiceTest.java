package com.amancay.application.service;

import static com.amancay.application.service.fake.Admins.ADMIN_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.amancay.application.port.in.ListAllOrdersQuery.AdminOrder;
import com.amancay.application.port.out.OrderRepositoryPort;
import com.amancay.application.service.fake.Admins;
import com.amancay.domain.exception.AdminRequiredException;
import com.amancay.domain.model.Order;
import com.amancay.domain.model.OrderFixtures;
import com.amancay.domain.model.OrderStatus;

class AdminOrderServiceTest {

    @Test
    void listAllKeepsRepositoryOrderAndAttachesBuyerEmails() {
        UUID ana = UUID.randomUUID();
        UUID beto = UUID.randomUUID();
        Order newest = OrderFixtures.persisted(UUID.randomUUID(), ana, OrderStatus.CREADO);
        Order older = OrderFixtures.persisted(UUID.randomUUID(), beto, OrderStatus.DESPACHADO);

        AdminOrderService service = new AdminOrderService(ordersReturning(List.of(newest, older)),
                userIds -> Map.of(ana, "ana@amancay.com"), Admins.guard());

        List<AdminOrder> result = service.listAll(ADMIN_ID);

        assertThat(result).extracting(adminOrder -> adminOrder.order().getId())
                .containsExactly(newest.getId(), older.getId());
        assertThat(result).extracting(AdminOrder::buyerEmail).containsExactly("ana@amancay.com", null);
    }

    @Test
    void buyerCannotListEveryonesOrders() {
        AdminOrderService service = new AdminOrderService(ordersReturning(List.of()), userIds -> Map.of(),
                Admins.guard());

        assertThatThrownBy(() -> service.listAll(UUID.randomUUID())).isInstanceOf(AdminRequiredException.class);
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
