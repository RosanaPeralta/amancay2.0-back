package com.amancay.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.amancay.application.port.in.CreateOrderCommand;
import com.amancay.application.port.out.LoadProductVariantPort;
import com.amancay.application.port.out.LoadRequesterPort;
import com.amancay.application.port.out.LoadShippingAddressPort;
import com.amancay.application.port.out.OrderRepositoryPort;
import com.amancay.application.port.out.PublishOrderEventPort;
import com.amancay.domain.event.OrderStatusChangedEvent;
import com.amancay.domain.exception.AdminRequiredException;
import com.amancay.domain.exception.InsufficientStockException;
import com.amancay.domain.exception.InvalidOrderStatusTransitionException;
import com.amancay.domain.exception.OrderAccessDeniedException;
import com.amancay.domain.exception.OrderNotFoundException;
import com.amancay.domain.exception.UserNotFoundException;
import com.amancay.domain.model.Order;
import com.amancay.domain.model.OrderFixtures;
import com.amancay.domain.model.OrderStatus;
import com.amancay.domain.model.OrderStatusChange;
import com.amancay.domain.model.ShippingAddress;

// Sin Mockito ni Spring: cada puerto de salida se reemplaza por un fake en memoria.
class OrderServiceTest {

    private final InMemoryOrders orders = new InMemoryOrders();
    private final Map<UUID, LoadProductVariantPort.ProductVariantInfo> variants = new HashMap<>();
    private final Map<UUID, ShippingAddress> addresses = new HashMap<>();
    private final Map<UUID, LoadRequesterPort.Requester> users = new HashMap<>();
    private final List<OrderStatusChangedEvent> publishedEvents = new ArrayList<>();

    private OrderService orderService;

    @BeforeEach
    void setUp() {
        LoadProductVariantPort variantPort = id -> Optional.ofNullable(variants.get(id));
        LoadShippingAddressPort addressPort = id -> Optional.ofNullable(addresses.get(id));
        LoadRequesterPort requesterPort = id -> Optional.ofNullable(users.get(id))
                .orElseThrow(() -> new UserNotFoundException(id));
        PublishOrderEventPort eventPort = publishedEvents::add;
        orderService = new OrderService(orders, variantPort, addressPort, requesterPort, eventPort,
                new AdminGuard(requesterPort));
    }

    @Test
    void createOrderSnapshotsPricesAndAddress() {
        UUID buyerId = buyer();
        UUID addressId = address();
        UUID variantId = variant("50.00", 10);

        Order result = orderService.create(command(buyerId, addressId, variantId, 2));

        assertThat(result.getId()).isNotNull();
        assertThat(result.getUserId()).isEqualTo(buyerId);
        assertThat(result.getStatus()).isEqualTo(OrderStatus.CREADO);
        assertThat(result.getTotal()).isEqualByComparingTo("100.00");
        assertThat(result.getItems()).singleElement()
                .satisfies(item -> assertThat(item.unitPrice()).isEqualByComparingTo("50.00"));
        assertThat(result.getShippingAddress()).isEqualTo(addresses.get(addressId));
        assertThat(orders.findById(result.getId())).isPresent();
    }

    @Test
    void createOrderFailsWithoutSavingWhenStockIsInsufficient() {
        UUID buyerId = buyer();
        UUID variantId = variant("50.00", 3);

        assertThatThrownBy(() -> orderService.create(command(buyerId, address(), variantId, 5)))
                .isInstanceOf(InsufficientStockException.class);
        assertThat(orders.findByUserId(buyerId)).isEmpty();
    }

    @Test
    void createOrderFailsWhenAddressDoesNotExist() {
        UUID variantId = variant("50.00", 10);

        assertThatThrownBy(() -> orderService.create(command(buyer(), UUID.randomUUID(), variantId, 1)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageStartingWith("Shipping address not found");
    }

    @Test
    void createOrderFailsWhenVariantDoesNotExist() {
        assertThatThrownBy(() -> orderService.create(command(buyer(), address(), UUID.randomUUID(), 1)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageStartingWith("Product variant not found");
    }

    @Test
    void buyerListsOnlyOwnOrders() {
        UUID buyerId = buyer();
        orders.put(OrderFixtures.persisted(UUID.randomUUID(), buyerId, OrderStatus.CREADO));
        orders.put(OrderFixtures.persisted(UUID.randomUUID(), UUID.randomUUID(), OrderStatus.CREADO));

        List<Order> result = orderService.list(buyerId, buyerId);

        assertThat(result).singleElement().satisfies(order -> assertThat(order.getUserId()).isEqualTo(buyerId));
    }

    @Test
    void adminCanListAnotherUsersOrders() {
        UUID adminId = admin();
        UUID targetUserId = UUID.randomUUID();
        orders.put(OrderFixtures.persisted(UUID.randomUUID(), targetUserId, OrderStatus.EN_PREPARACION));

        List<Order> result = orderService.list(adminId, targetUserId);

        assertThat(result).singleElement()
                .satisfies(order -> assertThat(order.getUserId()).isEqualTo(targetUserId));
    }

    @Test
    void buyerCannotListAnotherUsersOrders() {
        UUID buyerId = buyer();

        assertThatThrownBy(() -> orderService.list(buyerId, UUID.randomUUID()))
                .isInstanceOf(OrderAccessDeniedException.class);
    }

    @Test
    void unknownRequesterIsReportedAsUserNotFound() {
        assertThatThrownBy(() -> orderService.list(UUID.randomUUID(), null))
                .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    void buyerGetsOwnOrder() {
        UUID buyerId = buyer();
        UUID orderId = UUID.randomUUID();
        orders.put(OrderFixtures.persisted(orderId, buyerId, OrderStatus.CREADO));

        assertThat(orderService.get(buyerId, orderId, null).getId()).isEqualTo(orderId);
    }

    @Test
    void buyerCannotGetAnotherUsersOrder() {
        UUID orderId = UUID.randomUUID();
        orders.put(OrderFixtures.persisted(orderId, UUID.randomUUID(), OrderStatus.CREADO));

        assertThatThrownBy(() -> orderService.get(buyer(), orderId, null))
                .isInstanceOf(OrderAccessDeniedException.class);
    }

    @Test
    void getUnknownOrderIsReportedAsNotFound() {
        assertThatThrownBy(() -> orderService.get(buyer(), UUID.randomUUID(), null))
                .isInstanceOf(OrderNotFoundException.class);
    }

    @Test
    void changeStatusPersistsTheTransitionAndPublishesAnEvent() {
        UUID orderId = UUID.randomUUID();
        orders.put(OrderFixtures.persisted(orderId, UUID.randomUUID(), OrderStatus.CREADO));

        Order result = orderService.changeStatus(admin(), orderId, OrderStatus.EN_PREPARACION);

        assertThat(result.getStatus()).isEqualTo(OrderStatus.EN_PREPARACION);
        assertThat(orders.findById(orderId).orElseThrow().getStatusHistory())
                .extracting(OrderStatusChange::status).containsExactly(OrderStatus.EN_PREPARACION);
        assertThat(publishedEvents).containsExactly(
                new OrderStatusChangedEvent(orderId, OrderStatus.CREADO, OrderStatus.EN_PREPARACION));
    }

    @Test
    void invalidTransitionIsNotSavedNorPublished() {
        UUID orderId = UUID.randomUUID();
        orders.put(OrderFixtures.persisted(orderId, UUID.randomUUID(), OrderStatus.CREADO));

        assertThatThrownBy(() -> orderService.changeStatus(admin(), orderId, OrderStatus.ENTREGADO))
                .isInstanceOf(InvalidOrderStatusTransitionException.class);
        assertThat(orders.findById(orderId).orElseThrow().getStatus()).isEqualTo(OrderStatus.CREADO);
        assertThat(publishedEvents).isEmpty();
    }

    @Test
    void buyerCannotChangeTheStatus() {
        UUID orderId = UUID.randomUUID();
        orders.put(OrderFixtures.persisted(orderId, UUID.randomUUID(), OrderStatus.CREADO));

        assertThatThrownBy(() -> orderService.changeStatus(buyer(), orderId, OrderStatus.EN_PREPARACION))
                .isInstanceOf(AdminRequiredException.class);
        assertThat(orders.findById(orderId).orElseThrow().getStatus()).isEqualTo(OrderStatus.CREADO);
        assertThat(publishedEvents).isEmpty();
    }

    @Test
    void markAsPaidMovesTheOrderWithoutAnAdmin() {
        UUID orderId = UUID.randomUUID();
        orders.put(OrderFixtures.persisted(orderId, UUID.randomUUID(), OrderStatus.CREADO));

        orderService.markAsPaid(orderId);

        assertThat(orders.findById(orderId).orElseThrow().getStatus()).isEqualTo(OrderStatus.EN_PREPARACION);
        assertThat(publishedEvents).containsExactly(
                new OrderStatusChangedEvent(orderId, OrderStatus.CREADO, OrderStatus.EN_PREPARACION));
    }

    private UUID buyer() {
        UUID id = UUID.randomUUID();
        users.put(id, new LoadRequesterPort.Requester(id, false));
        return id;
    }

    private UUID admin() {
        UUID id = UUID.randomUUID();
        users.put(id, new LoadRequesterPort.Requester(id, true));
        return id;
    }

    private UUID address() {
        UUID id = UUID.randomUUID();
        addresses.put(id, OrderFixtures.ADDRESS);
        return id;
    }

    private UUID variant(String price, int stock) {
        UUID id = UUID.randomUUID();
        variants.put(id, new LoadProductVariantPort.ProductVariantInfo(id, new BigDecimal(price), stock));
        return id;
    }

    private CreateOrderCommand command(UUID requesterId, UUID addressId, UUID variantId, int quantity) {
        return new CreateOrderCommand(requesterId, addressId, List.of(new CreateOrderCommand.Item(variantId, quantity)));
    }

    // Imita lo que hace la base: asigna ids y timestamps al guardar, y devuelve una
    // copia para que el test no comparta la instancia con el servicio.
    private static final class InMemoryOrders implements OrderRepositoryPort {

        private final Map<UUID, Order> store = new HashMap<>();

        void put(Order order) {
            store.put(order.getId(), order);
        }

        @Override
        public Optional<Order> findById(UUID id) {
            return Optional.ofNullable(store.get(id)).map(InMemoryOrders::copy);
        }

        @Override
        public List<Order> findByUserId(UUID userId) {
            return store.values().stream().filter(order -> order.belongsTo(userId)).map(InMemoryOrders::copy)
                    .toList();
        }

        @Override
        public List<Order> findAllById(Collection<UUID> ids) {
            return ids.stream().map(store::get).filter(order -> order != null).map(InMemoryOrders::copy).toList();
        }

        @Override
        public List<Order> findAllNewestFirst() {
            return store.values().stream().sorted(Comparator.comparing(Order::getCreatedAt).reversed())
                    .map(InMemoryOrders::copy).toList();
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
}
