package com.amancay.order.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.amancay.order.domain.exception.InvalidOrderStatusTransitionException;

class OrderTest {

    @Test
    void newOrderStartsAsCreadoWithNoHistory() {
        Order order = OrderFixtures.newOrder();

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CREADO);
        assertThat(order.getStatusHistory()).isEmpty();
    }

    @Test
    void createComputesSubtotalAndTotalFromItems() {
        Order order = Order.create(UUID.randomUUID(), UUID.randomUUID(), OrderFixtures.ADDRESS, List.of(
                OrderItem.create(UUID.randomUUID(), 2, new BigDecimal("50.00")),
                OrderItem.create(UUID.randomUUID(), 1, new BigDecimal("25.50"))));

        assertThat(order.getSubtotal()).isEqualByComparingTo("125.50");
        assertThat(order.getShippingCost()).isEqualByComparingTo("0");
        assertThat(order.getTotal()).isEqualByComparingTo("125.50");
    }

    @Test
    void createRejectsAnOrderWithoutItems() {
        assertThatThrownBy(() -> Order.create(UUID.randomUUID(), UUID.randomUUID(), OrderFixtures.ADDRESS, List.of()))
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
}
