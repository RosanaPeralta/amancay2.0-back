
package com.amancay.application.service;

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

import com.amancay.application.port.out.LoadRequesterPort;
import com.amancay.application.port.out.OrderRepositoryPort;
import com.amancay.application.port.out.ReturnRequestRepositoryPort;
import com.amancay.domain.exception.OrderAccessDeniedException;
import com.amancay.domain.exception.OrderNotFoundException;
import com.amancay.domain.exception.UserNotFoundException;
import com.amancay.domain.model.Order;
import com.amancay.domain.model.OrderFixtures;
import com.amancay.domain.model.OrderItem;
import com.amancay.domain.model.OrderStatus;
import com.amancay.domain.model.OrderStatusChange;
import com.amancay.domain.model.ReturnReason;
import com.amancay.domain.model.ReturnRequest;
import com.amancay.domain.model.ReturnRequestStatus;
import com.amancay.domain.model.ShippingAddress;

class ReturnRequestServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-10T12:00:00Z");

    private final InMemoryOrders orders = new InMemoryOrders();
    private final InMemoryReturnRequests returnRequests = new InMemoryReturnRequests();
    private final Map<UUID, LoadRequesterPort.Requester> users = new HashMap<>();

    private ReturnRequestService service;

    @BeforeEach
    void setUp() {
        LoadRequesterPort requesterPort = id -> Optional.ofNullable(users.get(id))
                .orElseThrow(() -> new UserNotFoundException(id));

        Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);

        service = new ReturnRequestService(
                orders,
                returnRequests,
                requesterPort,
                clock);
    }

    @Test
    void createsReturnRequestForDeliveredOrderWithinReturnPeriod() {
        UUID buyerId = buyer();
        Order order = deliveredOrder(buyerId, NOW.minusSeconds(5 * 24 * 60 * 60L));

        ReturnRequest result = service.create(
                buyerId, order.getId(), ReturnReason.WRONG_SIZE, "Me quedó grande");

        assertThat(result.getId()).isNotNull();
        assertThat(result.getOrderId()).isEqualTo(order.getId());
        assertThat(result.getReason()).isEqualTo(ReturnReason.WRONG_SIZE);
        assertThat(result.getComment()).isEqualTo("Me quedó grande");
        assertThat(result.getStatus()).isEqualTo(ReturnRequestStatus.PENDIENTE);
        assertThat(result.getRequestedAt()).isEqualTo(NOW);
        assertThat(returnRequests.findByOrderId(order.getId())).isPresent();
    }

    @Test
    void allowsReturnRequestAtExactlyTenDaysAfterDelivery() {
        UUID buyerId = buyer();
        Order order = deliveredOrder(buyerId, NOW.minusSeconds(10 * 24 * 60 * 60L));

        ReturnRequest result = service.create(
                buyerId, order.getId(), ReturnReason.OTHER, null);

        assertThat(result.getStatus()).isEqualTo(ReturnRequestStatus.PENDIENTE);
    }

    @Test
    void rejectsReturnRequestAfterTenDaysHaveExpired() {
        UUID buyerId = buyer();
        Order order = deliveredOrder(
                buyerId,
                NOW.minusSeconds(10 * 24 * 60 * 60L + 1));

        assertThatThrownBy(() -> service.create(
                buyerId, order.getId(), ReturnReason.OTHER, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("10 days");

        assertThat(returnRequests.findByOrderId(order.getId())).isEmpty();
    }

    @Test
    void rejectsReturnRequestWhenOrderIsNotDelivered() {
        UUID buyerId = buyer();
        Order order = orderWithStatus(buyerId, OrderStatus.EN_PREPARACION, List.of());

        assertThatThrownBy(() -> service.create(
                buyerId, order.getId(), ReturnReason.OTHER, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("delivered");

        assertThat(returnRequests.findByOrderId(order.getId())).isEmpty();
    }

    @Test
    void rejectsDeliveredOrderWithoutDeliveryTimestamp() {
        UUID buyerId = buyer();
        Order order = orderWithStatus(buyerId, OrderStatus.ENTREGADO, List.of());

        assertThatThrownBy(() -> service.create(
                buyerId, order.getId(), ReturnReason.OTHER, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("delivery date");

        assertThat(returnRequests.findByOrderId(order.getId())).isEmpty();
    }

    @Test
    void buyerCannotRequestReturnForAnotherUsersOrder() {
        UUID buyerId = buyer();
        Order order = deliveredOrder(UUID.randomUUID(), NOW.minusSeconds(5 * 24 * 60 * 60L));

        assertThatThrownBy(() -> service.create(
                buyerId, order.getId(), ReturnReason.OTHER, null))
                .isInstanceOf(OrderAccessDeniedException.class);

        assertThat(returnRequests.findByOrderId(order.getId())).isEmpty();
    }

    @Test
    void rejectsReturnRequestWhenOrderDoesNotExist() {
        UUID buyerId = buyer();

        assertThatThrownBy(() -> service.create(
                buyerId, UUID.randomUUID(), ReturnReason.OTHER, null))
                .isInstanceOf(OrderNotFoundException.class);
    }

    @Test
    void rejectsSecondReturnRequestForSameOrder() {
        UUID buyerId = buyer();
        Order order = deliveredOrder(buyerId, NOW.minusSeconds(5 * 24 * 60 * 60L));

        service.create(buyerId, order.getId(), ReturnReason.OTHER, null);

        assertThatThrownBy(() -> service.create(
                buyerId, order.getId(), ReturnReason.DAMAGED, null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already exists");

        assertThat(returnRequests.findByOrderId(order.getId())).isPresent();
    }

    @Test
    void rejectsRequestFromUnknownUser() {
        assertThatThrownBy(() -> service.create(
                UUID.randomUUID(), UUID.randomUUID(), ReturnReason.OTHER, null))
                .isInstanceOf(UserNotFoundException.class);
    }

    private UUID buyer() {
        UUID id = UUID.randomUUID();
        users.put(id, new LoadRequesterPort.Requester(id, false));
        return id;
    }

    private Order deliveredOrder(UUID userId, Instant deliveredAt) {
        return orderWithStatus(userId, OrderStatus.ENTREGADO, List.of(
                new OrderStatusChange(
                        UUID.randomUUID(),
                        OrderStatus.ENTREGADO,
                        deliveredAt)));
    }

    private Order orderWithStatus(
            UUID userId,
            OrderStatus status,
            List<OrderStatusChange> history) {

        UUID orderId = UUID.randomUUID();
        Order order = new Order(
                orderId,
                userId,
                UUID.randomUUID(),
                OrderFixtures.ADDRESS,
                status,
                new BigDecimal("100.00"),
                BigDecimal.ZERO,
                new BigDecimal("100.00"),
                NOW.minusSeconds(20 * 24 * 60 * 60L),
                NOW,
                List.of(OrderItem.create(UUID.randomUUID(), 1, new BigDecimal("100.00"))),
                history);

        orders.put(order);
        return order;
    }

    private static final class InMemoryOrders implements OrderRepositoryPort {

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
            return store.values().stream()
                    .filter(order -> order.belongsTo(userId))
                    .toList();
        }

        @Override
        public List<Order> findAllById(Collection<UUID> ids) {
            return ids.stream()
                    .map(store::get)
                    .filter(order -> order != null)
                    .toList();
        }

        @Override
        public List<Order> findAllNewestFirst() {
            return List.copyOf(store.values());
        }

        @Override
        public Order save(Order order) {
            store.put(order.getId(), order);
            return order;
        }
    }

    private static final class InMemoryReturnRequests implements ReturnRequestRepositoryPort {

        private final Map<UUID, ReturnRequest> store = new HashMap<>();

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
            UUID id = request.getId() == null ? UUID.randomUUID() : request.getId();

            ReturnRequest saved = new ReturnRequest(
                    id,
                    request.getOrderId(),
                    request.getReason(),
                    request.getComment(),
                    request.getStatus(),
                    request.getRequestedAt(),
                    request.getResolvedAt(),
                    request.getResolvedBy(),
                    request.getResolutionNote());

            store.put(id, saved);
            return saved;
        }
    }
}