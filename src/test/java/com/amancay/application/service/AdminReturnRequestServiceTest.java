
package com.amancay.application.service;

import static com.amancay.application.service.fake.Admins.ADMIN_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.amancay.application.port.out.OrderRepositoryPort;
import com.amancay.application.port.out.PublishOrderEventPort;
import com.amancay.application.port.out.ReturnRequestRepositoryPort;
import com.amancay.domain.event.OrderStatusChangedEvent;
import com.amancay.domain.exception.AdminRequiredException;
import com.amancay.application.service.fake.Admins;
import com.amancay.domain.model.Order;
import com.amancay.domain.model.OrderStatus;
import com.amancay.domain.model.OrderStatusChange;
import com.amancay.domain.model.ReturnReason;
import com.amancay.domain.model.ReturnRequest;
import com.amancay.domain.model.ReturnRequestStatus;
import com.amancay.domain.model.ShippingAddress;

class AdminReturnRequestServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-10T12:00:00Z");

    private final InMemoryOrders orders = new InMemoryOrders();
    private final InMemoryReturnRequests requests = new InMemoryReturnRequests();
    private final List<OrderStatusChangedEvent> publishedEvents =
            new java.util.ArrayList<>();

    private AdminReturnRequestService service;

    @BeforeEach
    void setUp() {
        PublishOrderEventPort eventPort = publishedEvents::add;

        service = new AdminReturnRequestService(
                requests,
                orders,
                Admins.guard(),
                eventPort,
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void adminCanApproveReturnAndOrderBecomesDevuelto() {
        UUID orderId = UUID.randomUUID();
        UUID requestId = UUID.randomUUID();

        orders.put(deliveredOrder(orderId));
        requests.put(pendingRequest(requestId, orderId));

        ReturnRequest result = service.approve(ADMIN_ID, requestId);

        assertThat(result.getStatus()).isEqualTo(ReturnRequestStatus.APROBADA);
        assertThat(result.getResolvedBy()).isEqualTo(ADMIN_ID);
        assertThat(result.getResolvedAt()).isEqualTo(NOW);

        assertThat(orders.findById(orderId).orElseThrow().getStatus())
                .isEqualTo(OrderStatus.DEVUELTO);

        assertThat(publishedEvents).containsExactly(
                new OrderStatusChangedEvent(
                        orderId,
                        OrderStatus.ENTREGADO,
                        OrderStatus.DEVUELTO));
    }

    @Test
    void adminCanRejectReturnWithResolutionNote() {
        UUID orderId = UUID.randomUUID();
        UUID requestId = UUID.randomUUID();

        orders.put(deliveredOrder(orderId));
        requests.put(pendingRequest(requestId, orderId));

        ReturnRequest result = service.reject(
                ADMIN_ID, requestId, "El producto no cumple las condiciones de devolución.");

        assertThat(result.getStatus()).isEqualTo(ReturnRequestStatus.RECHAZADA);
        assertThat(result.getResolutionNote())
                .isEqualTo("El producto no cumple las condiciones de devolución.");
        assertThat(result.getResolvedBy()).isEqualTo(ADMIN_ID);
        assertThat(result.getResolvedAt()).isEqualTo(NOW);

        assertThat(orders.findById(orderId).orElseThrow().getStatus())
                .isEqualTo(OrderStatus.ENTREGADO);
        assertThat(publishedEvents).isEmpty();
    }

    @Test
    void rejectionWithoutNoteFails() {
        UUID orderId = UUID.randomUUID();
        UUID requestId = UUID.randomUUID();

        orders.put(deliveredOrder(orderId));
        requests.put(pendingRequest(requestId, orderId));

        assertThatThrownBy(() -> service.reject(ADMIN_ID, requestId, " "))
                .isInstanceOf(IllegalArgumentException.class);

        assertThat(requests.findById(requestId).orElseThrow().getStatus())
                .isEqualTo(ReturnRequestStatus.PENDIENTE);
    }

    @Test
    void buyerCannotApproveReturn() {
        UUID orderId = UUID.randomUUID();
        UUID requestId = UUID.randomUUID();

        orders.put(deliveredOrder(orderId));
        requests.put(pendingRequest(requestId, orderId));

        assertThatThrownBy(() -> service.approve(UUID.randomUUID(), requestId))
                .isInstanceOf(AdminRequiredException.class);

        assertThat(requests.findById(requestId).orElseThrow().getStatus())
                .isEqualTo(ReturnRequestStatus.PENDIENTE);
        assertThat(publishedEvents).isEmpty();
    }

    @Test
    void buyerCannotRejectReturn() {
        UUID orderId = UUID.randomUUID();
        UUID requestId = UUID.randomUUID();

        orders.put(deliveredOrder(orderId));
        requests.put(pendingRequest(requestId, orderId));

        assertThatThrownBy(() -> service.reject(
                UUID.randomUUID(), requestId, "Motivo de rechazo"))
                .isInstanceOf(AdminRequiredException.class);

        assertThat(requests.findById(requestId).orElseThrow().getStatus())
                .isEqualTo(ReturnRequestStatus.PENDIENTE);
    }

    @Test
    void cannotApproveReturnWhenOrderIsNotDelivered() {
        UUID orderId = UUID.randomUUID();
        UUID requestId = UUID.randomUUID();

        orders.put(orderWithStatus(orderId, OrderStatus.DESPACHADO));
        requests.put(pendingRequest(requestId, orderId));

        assertThatThrownBy(() -> service.approve(ADMIN_ID, requestId))
                .isInstanceOf(IllegalStateException.class);

        assertThat(requests.findById(requestId).orElseThrow().getStatus())
                .isEqualTo(ReturnRequestStatus.PENDIENTE);
        assertThat(publishedEvents).isEmpty();
    }

    @Test
    void cannotResolveReturnTwice() {
        UUID orderId = UUID.randomUUID();
        UUID requestId = UUID.randomUUID();

        orders.put(deliveredOrder(orderId));
        requests.put(pendingRequest(requestId, orderId));

        service.reject(ADMIN_ID, requestId, "No corresponde la devolución.");

        assertThatThrownBy(() -> service.approve(ADMIN_ID, requestId))
                .isInstanceOf(IllegalStateException.class);

        assertThat(requests.findById(requestId).orElseThrow().getStatus())
                .isEqualTo(ReturnRequestStatus.RECHAZADA);
        assertThat(publishedEvents).isEmpty();
    }

    private static ReturnRequest pendingRequest(UUID requestId, UUID orderId) {
        return new ReturnRequest(
                requestId,
                orderId,
                ReturnReason.OTHER,
                "Solicitud de devolución",
                ReturnRequestStatus.PENDIENTE,
                NOW.minusSeconds(3600),
                null,
                null,
                null);
    }

    private static Order deliveredOrder(UUID orderId) {
        return orderWithStatus(orderId, OrderStatus.ENTREGADO);
    }

    private static Order orderWithStatus(UUID orderId, OrderStatus status) {
        UUID userId = UUID.randomUUID();
        Instant changedAt = NOW.minusSeconds(86400);

        List<OrderStatusChange> history = status == OrderStatus.ENTREGADO
                ? List.of(new OrderStatusChange(
                        UUID.randomUUID(), OrderStatus.ENTREGADO, changedAt))
                : List.of();

        ShippingAddress address = new ShippingAddress(
                "Calle", "1", null, "Ciudad", null, "Pais", null);

        return new Order(
                orderId,
                userId,
                UUID.randomUUID(),
                address,
                status,
                new BigDecimal("100.00"),
                BigDecimal.ZERO,
                new BigDecimal("100.00"),
                NOW.minusSeconds(172800),
                NOW,
                List.of(),
                history);
    }

    private static class InMemoryOrders implements OrderRepositoryPort {

        private final Map<UUID, Order> store = new HashMap<>();

        void put(Order order) {
            store.put(order.getId(), order);
        }

        @Override
        public Optional<Order> findById(UUID id) {
            return Optional.ofNullable(store.get(id));
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
            return List.of();
        }

        @Override
        public Order save(Order order) {
            store.put(order.getId(), order);
            return order;
        }
    }

    private static class InMemoryReturnRequests implements ReturnRequestRepositoryPort {

        private final Map<UUID, ReturnRequest> store = new HashMap<>();

        void put(ReturnRequest request) {
            store.put(request.getId(), request);
        }

        @Override
        public Optional<ReturnRequest> findById(UUID id) {
            return Optional.ofNullable(store.get(id));
        }

        @Override
        public Optional<ReturnRequest> findByOrderId(UUID orderId) {
            return store.values().stream()
                    .filter(request -> request.getOrderId().equals(orderId))
                    .findFirst();
        }

        @Override
        public List<ReturnRequest> findByStatus(ReturnRequestStatus status) {
            return store.values().stream()
                    .filter(request -> request.getStatus() == status)
                    .toList();
        }

        @Override
        public ReturnRequest save(ReturnRequest request) {
            store.put(request.getId(), request);
            return request;
        }
    }
}