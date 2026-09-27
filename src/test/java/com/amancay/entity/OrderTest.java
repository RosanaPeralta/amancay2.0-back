package com.amancay.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import com.amancay.exceptions.InvalidOrderStatusTransitionException;

class OrderTest {

    @Test
    void newOrderStartsAsCreadoWithNoHistory() {
        Order order = new Order();

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CREADO);
        assertThat(order.getStatusHistory()).isEmpty();
    }

    @Test
    void changeStatusFollowsTheFullHappyPath() {
        Order order = new Order();

        order.changeStatus(OrderStatus.EN_PREPARACION);
        order.changeStatus(OrderStatus.DESPACHADO);
        order.changeStatus(OrderStatus.ENTREGADO);
        order.changeStatus(OrderStatus.DEVUELTO);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.DEVUELTO);
        assertThat(order.getStatusHistory()).extracting(OrderStatusHistory::getStatus)
                .containsExactly(OrderStatus.EN_PREPARACION, OrderStatus.DESPACHADO, OrderStatus.ENTREGADO,
                        OrderStatus.DEVUELTO);
    }

    @Test
    void changeStatusLinksEachHistoryEntryBackToTheOrder() {
        Order order = new Order();

        order.changeStatus(OrderStatus.EN_PREPARACION);

        assertThat(order.getStatusHistory()).hasSize(1);
        assertThat(order.getStatusHistory().getFirst().getOrder()).isSameAs(order);
    }

    @Test
    void changeStatusRejectsSkippingAStep() {
        Order order = new Order();

        assertThatThrownBy(() -> order.changeStatus(OrderStatus.DESPACHADO))
                .isInstanceOf(InvalidOrderStatusTransitionException.class);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CREADO);
        assertThat(order.getStatusHistory()).isEmpty();
    }

    @Test
    void changeStatusRejectsGoingBackwards() {
        Order order = new Order();
        order.changeStatus(OrderStatus.EN_PREPARACION);
        order.changeStatus(OrderStatus.DESPACHADO);

        assertThatThrownBy(() -> order.changeStatus(OrderStatus.CREADO))
                .isInstanceOf(InvalidOrderStatusTransitionException.class);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.DESPACHADO);
    }

    @Test
    void devueltoIsATerminalState() {
        Order order = new Order();
        order.changeStatus(OrderStatus.EN_PREPARACION);
        order.changeStatus(OrderStatus.DESPACHADO);
        order.changeStatus(OrderStatus.ENTREGADO);
        order.changeStatus(OrderStatus.DEVUELTO);

        assertThatThrownBy(() -> order.changeStatus(OrderStatus.ENTREGADO))
                .isInstanceOf(InvalidOrderStatusTransitionException.class);
    }
}
