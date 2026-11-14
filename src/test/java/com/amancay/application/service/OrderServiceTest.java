package com.amancay.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
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
import com.amancay.application.service.fake.InMemoryOrderRepository;
import com.amancay.application.port.out.LoadProductVariantPort;
import com.amancay.application.port.out.LoadRequesterPort;
import com.amancay.application.port.out.LoadShippingAddressPort;
import com.amancay.application.port.out.OrderRepositoryPort;
import com.amancay.application.port.out.PublishOrderEventPort;
import com.amancay.application.port.out.ShippingQuotePort;
import com.amancay.domain.event.OrderStatusChangedEvent;
import com.amancay.domain.exception.AdminRequiredException;
import com.amancay.domain.exception.InsufficientStockException;
import com.amancay.domain.exception.InvalidOrderStatusTransitionException;
import com.amancay.domain.exception.OrderAccessDeniedException;
import com.amancay.domain.exception.OrderNotFoundException;
import com.amancay.domain.exception.ShippingQuoteUnavailableException;
import com.amancay.domain.exception.UserNotFoundException;
import com.amancay.domain.model.Order;
import com.amancay.domain.model.OrderFixtures;
import com.amancay.domain.model.OrderStatus;
import com.amancay.domain.model.OrderStatusChange;
import com.amancay.domain.model.ShippingAddress;
import com.amancay.domain.model.ShippingQuote;

// Sin Mockito ni Spring: cada puerto de salida se reemplaza por un fake en memoria.
class OrderServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-14T12:00:00Z");

    private final InMemoryOrderRepository orders = new InMemoryOrderRepository();
    private final Map<UUID, LoadProductVariantPort.ProductVariantInfo> variants = new HashMap<>();
    private final Map<UUID, ShippingAddress> addresses = new HashMap<>();
    private final Map<UUID, LoadRequesterPort.Requester> users = new HashMap<>();
    private final List<OrderStatusChangedEvent> publishedEvents = new ArrayList<>();
    private final List<ShippingAddress> quotedAddresses = new ArrayList<>();
    private boolean shippingUnavailable;

    private OrderService orderService;

    @BeforeEach
    void setUp() {
        LoadProductVariantPort variantPort = id -> Optional.ofNullable(variants.get(id));
        LoadShippingAddressPort addressPort = id -> Optional.ofNullable(addresses.get(id));
        LoadRequesterPort requesterPort = id -> Optional.ofNullable(users.get(id))
                .orElseThrow(() -> new UserNotFoundException(id));
        PublishOrderEventPort eventPort = publishedEvents::add;
        ShippingQuotePort shippingPort = destination -> {
            if (shippingUnavailable) {
                throw new ShippingQuoteUnavailableException("down");
            }
            quotedAddresses.add(destination);
            return new ShippingQuote(new BigDecimal("15.0"), new BigDecimal("12.50"));
        };
        orderService = new OrderService(orders, variantPort, addressPort, requesterPort, eventPort, shippingPort,
                Clock.fixed(NOW, ZoneOffset.UTC));
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
        assertThat(result.getSubtotal()).isEqualByComparingTo("100.00");
        assertThat(result.getShippingCost()).isEqualByComparingTo("12.50");
        assertThat(result.getTotal()).isEqualByComparingTo("112.50");
        assertThat(result.getItems()).singleElement()
                .satisfies(item -> assertThat(item.unitPrice()).isEqualByComparingTo("50.00"));
        assertThat(result.getShippingAddress()).isEqualTo(addresses.get(addressId));
        assertThat(orders.findById(result.getId())).isPresent();
    }

    @Test
    void createOrderFailsWithoutSavingWhenShippingCannotBeQuoted() {
        UUID buyerId = buyer();
        UUID variantId = variant("50.00", 10);
        shippingUnavailable = true;

        assertThatThrownBy(() -> orderService.create(command(buyerId, address(), variantId, 1)))
                .isInstanceOf(ShippingQuoteUnavailableException.class);
        assertThat(orders.findByUserId(buyerId)).isEmpty();
    }

    @Test
    void quoteUsesTheShippingAddressSnapshot() {
        UUID addressId = address();

        ShippingQuote quote = orderService.quote(addressId);

        assertThat(quote.cost()).isEqualByComparingTo("12.50");
        assertThat(quotedAddresses).containsExactly(addresses.get(addressId));
    }

    @Test
    void quoteFailsWhenAddressDoesNotExist() {
        assertThatThrownBy(() -> orderService.quote(UUID.randomUUID()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageStartingWith("Shipping address not found");
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
    void markAsPaidMovesTheOrderWithoutAnAdmin() {
        UUID orderId = UUID.randomUUID();
        UUID buyerId = UUID.randomUUID();
        orders.put(OrderFixtures.persisted(orderId, buyerId, OrderStatus.CREADO));

        orderService.markAsPaid(orderId);

        assertThat(orders.findById(orderId).orElseThrow().getStatus()).isEqualTo(OrderStatus.EN_PREPARACION);
        assertSinglePublishedEvent(orderId, buyerId, OrderStatus.CREADO, OrderStatus.EN_PREPARACION);
    }

    private void assertSinglePublishedEvent(UUID orderId, UUID buyerId, OrderStatus previous, OrderStatus next) {
        assertThat(publishedEvents).singleElement().satisfies(event -> {
            assertThat(event.eventId()).isNotNull();
            assertThat(event.orderId()).isEqualTo(orderId);
            assertThat(event.buyerId()).isEqualTo(buyerId);
            assertThat(event.previousStatus()).isEqualTo(previous);
            assertThat(event.newStatus()).isEqualTo(next);
            assertThat(event.occurredAt()).isEqualTo(NOW);
        });
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
}
