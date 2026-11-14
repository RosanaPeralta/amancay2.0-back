package com.amancay.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.time.Instant;

import org.junit.jupiter.api.Test;

import com.amancay.domain.exception.InvalidOrderStatusTransitionException;

class OrderTest {

    @Test
    void newOrderStartsAsCreadoWithNoHistory() {
        Order order = OrderFixtures.newOrder();

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CREADO);
        assertThat(order.getStatusHistory()).isEmpty();
    }

    @Test
    void createComputesSubtotalAndTotalFromItemsAndShipping() {
        Order order = Order.create(UUID.randomUUID(), UUID.randomUUID(), OrderFixtures.ADDRESS, List.of(
                OrderItem.create(UUID.randomUUID(), 2, new BigDecimal("50.00")),
                OrderItem.create(UUID.randomUUID(), 1, new BigDecimal("25.50"))), new BigDecimal("9.75"));

        assertThat(order.getSubtotal()).isEqualByComparingTo("125.50");
        assertThat(order.getShippingCost()).isEqualByComparingTo("9.75");
        assertThat(order.getTotal()).isEqualByComparingTo("135.25");
    }

    @Test
    void createRejectsAnOrderWithoutItems() {
        assertThatThrownBy(() -> Order.create(UUID.randomUUID(), UUID.randomUUID(), OrderFixtures.ADDRESS, List.of(),
                BigDecimal.ZERO))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void createRejectsNegativeShippingCost() {
        assertThatThrownBy(() -> Order.create(UUID.randomUUID(), UUID.randomUUID(), OrderFixtures.ADDRESS,
                List.of(OrderItem.create(UUID.randomUUID(), 1, BigDecimal.TEN)), new BigDecimal("-1")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void itemRejectsNonPositiveQuantity() {
        assertThatThrownBy(() -> OrderItem.create(UUID.randomUUID(), 0, BigDecimal.TEN))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void changeStatusFollowsTheFullHappyPath() {
        Order order = OrderFixtures.newOrder();

        order.changeStatus(OrderStatus.EN_PREPARACION);
        order.changeStatus(OrderStatus.DESPACHADO);
        order.changeStatus(OrderStatus.ENTREGADO);
        order.changeStatus(OrderStatus.DEVUELTO);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.DEVUELTO);
        assertThat(order.getStatusHistory()).extracting(OrderStatusChange::status)
                .containsExactly(OrderStatus.EN_PREPARACION, OrderStatus.DESPACHADO, OrderStatus.ENTREGADO,
                        OrderStatus.DEVUELTO);
        assertThat(order.getStatusHistory()).allMatch(OrderStatusChange::isNew);
    }

    @Test
    void changeStatusRejectsSkippingAStep() {
        Order order = OrderFixtures.newOrder();

        assertThatThrownBy(() -> order.changeStatus(OrderStatus.DESPACHADO))
                .isInstanceOf(InvalidOrderStatusTransitionException.class);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CREADO);
        assertThat(order.getStatusHistory()).isEmpty();
    }

    @Test
    void changeStatusRejectsGoingBackwards() {
        Order order = OrderFixtures.newOrder();
        order.changeStatus(OrderStatus.EN_PREPARACION);
        order.changeStatus(OrderStatus.DESPACHADO);

        assertThatThrownBy(() -> order.changeStatus(OrderStatus.CREADO))
                .isInstanceOf(InvalidOrderStatusTransitionException.class);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.DESPACHADO);
    }

    @Test
    void devueltoIsATerminalState() {
        Order order = OrderFixtures.newOrder();
        order.changeStatus(OrderStatus.EN_PREPARACION);
        order.changeStatus(OrderStatus.DESPACHADO);
        order.changeStatus(OrderStatus.ENTREGADO);
        order.changeStatus(OrderStatus.DEVUELTO);

        assertThatThrownBy(() -> order.changeStatus(OrderStatus.ENTREGADO))
                .isInstanceOf(InvalidOrderStatusTransitionException.class);
    }

    @Test
    void getDeliveredAtReturnsTheDeliveryTimestamp() {
        Instant deliveredAt = Instant.parse("2026-11-05T16:30:00Z");

        Order order = orderWithHistory(List.of(
                new OrderStatusChange(UUID.randomUUID(), OrderStatus.EN_PREPARACION,
                        Instant.parse("2026-11-02T09:30:00Z")),
                new OrderStatusChange(UUID.randomUUID(), OrderStatus.DESPACHADO,
                        Instant.parse("2026-11-03T14:00:00Z")),
                new OrderStatusChange(UUID.randomUUID(), OrderStatus.ENTREGADO, deliveredAt)));

        assertThat(order.getDeliveredAt()).contains(deliveredAt);
    }

    @Test
    void getDeliveredAtReturnsEmptyWhenOrderWasNotDelivered() {
        Order order = orderWithHistory(List.of(
                new OrderStatusChange(UUID.randomUUID(), OrderStatus.EN_PREPARACION,
                        Instant.parse("2026-11-02T09:30:00Z")),
                new OrderStatusChange(UUID.randomUUID(), OrderStatus.DESPACHADO,
                        Instant.parse("2026-11-03T14:00:00Z"))));

        assertThat(order.getDeliveredAt()).isEmpty();
    }

    @Test
    void getDeliveredAtReturnsEmptyWhenDeliveryTimestampIsNull() {
        Order order = orderWithHistory(List.of(
                new OrderStatusChange(UUID.randomUUID(), OrderStatus.ENTREGADO, null)));

        assertThat(order.getDeliveredAt()).isEmpty();
    }

    private Order orderWithHistory(List<OrderStatusChange> history) {
        return new Order(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                OrderFixtures.ADDRESS,
                OrderStatus.ENTREGADO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                Instant.now(),
                Instant.now(),
                List.of(OrderItem.create(UUID.randomUUID(), 1, BigDecimal.TEN)),
                history);
    }

}
