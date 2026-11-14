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

        returnFully(order);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.DEVUELTO);
        assertThat(order.getStatusHistory()).extracting(OrderStatusChange::status)
                .containsExactly(OrderStatus.EN_PREPARACION, OrderStatus.LISTO_PARA_DESPACHAR, OrderStatus.DESPACHADO,
                        OrderStatus.ENTREGADO, OrderStatus.ESPERANDO_RETIRO, OrderStatus.RETIRADO,
                        OrderStatus.DEVUELTO);
        assertThat(order.getStatusHistory()).allMatch(OrderStatusChange::isNew);
    }

    @Test
    void changeStatusRejectsSkippingAStep() {
        Order order = OrderFixtures.newOrder();

        assertThatThrownBy(() -> order.changeStatus(OrderStatus.DESPACHADO, OrderTransitionTrigger.PAYMENT))
                .isInstanceOf(InvalidOrderStatusTransitionException.class);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CREADO);
        assertThat(order.getStatusHistory()).isEmpty();
    }

    @Test
    void onlyAnApprovedPaymentMovesACreatedOrderToPreparation() {
        Order order = OrderFixtures.newOrder();

        assertThatThrownBy(() -> order.changeStatus(OrderStatus.EN_PREPARACION, OrderTransitionTrigger.LOGISTICS))
                .isInstanceOf(InvalidOrderStatusTransitionException.class)
                .hasMessageContaining("LOGISTICS");
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CREADO);
        assertThat(order.getStatusHistory()).isEmpty();
    }

    @Test
    void onlyLogisticsPreparesAnOrderForDispatch() {
        Order order = OrderFixtures.newOrder();
        order.changeStatus(OrderStatus.EN_PREPARACION, OrderTransitionTrigger.PAYMENT);

        assertThatThrownBy(() -> order.changeStatus(OrderStatus.LISTO_PARA_DESPACHAR, OrderTransitionTrigger.PAYMENT))
                .isInstanceOf(InvalidOrderStatusTransitionException.class);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.EN_PREPARACION);
    }

    @Test
    void eachStatusDeclaresWhoAdvancesIt() {
        assertThat(OrderStatus.CREADO.isAdvancedBy(OrderTransitionTrigger.PAYMENT)).isTrue();
        for (OrderStatus status : List.of(OrderStatus.EN_PREPARACION, OrderStatus.LISTO_PARA_DESPACHAR,
                OrderStatus.DESPACHADO, OrderStatus.ESPERANDO_RETIRO, OrderStatus.RETIRADO)) {
            assertThat(status.isAdvancedBy(OrderTransitionTrigger.LOGISTICS)).as(status.name()).isTrue();
        }
        assertThat(OrderStatus.ENTREGADO.isAdvancedBy(OrderTransitionTrigger.RETURN_REQUEST)).isTrue();
        assertThat(OrderStatus.ENTREGADO.isAdvancedBy(OrderTransitionTrigger.LOGISTICS)).isFalse();
        for (OrderTransitionTrigger trigger : OrderTransitionTrigger.values()) {
            assertThat(OrderStatus.DEVUELTO.isAdvancedBy(trigger)).isFalse();
        }
    }

    @Test
    void changeStatusRejectsGoingBackwards() {
        Order order = OrderFixtures.newOrder();
        order.changeStatus(OrderStatus.EN_PREPARACION, OrderTransitionTrigger.PAYMENT);
        order.changeStatus(OrderStatus.LISTO_PARA_DESPACHAR, OrderTransitionTrigger.LOGISTICS);
        order.changeStatus(OrderStatus.DESPACHADO, OrderTransitionTrigger.LOGISTICS);

        assertThatThrownBy(() -> order.changeStatus(OrderStatus.LISTO_PARA_DESPACHAR, OrderTransitionTrigger.LOGISTICS))
                .isInstanceOf(InvalidOrderStatusTransitionException.class);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.DESPACHADO);
    }

    @Test
    void devueltoIsATerminalState() {
        Order order = OrderFixtures.newOrder();
        returnFully(order);

        assertThatThrownBy(() -> order.changeStatus(OrderStatus.ENTREGADO, OrderTransitionTrigger.LOGISTICS))
                .isInstanceOf(InvalidOrderStatusTransitionException.class);
    }

    @Test
    void onlyAnApprovedReturnSendsLogisticsToPickUpTheOrder() {
        Order order = OrderFixtures.newOrder();
        order.changeStatus(OrderStatus.EN_PREPARACION, OrderTransitionTrigger.PAYMENT);
        order.changeStatus(OrderStatus.LISTO_PARA_DESPACHAR, OrderTransitionTrigger.LOGISTICS);
        order.changeStatus(OrderStatus.DESPACHADO, OrderTransitionTrigger.LOGISTICS);
        order.changeStatus(OrderStatus.ENTREGADO, OrderTransitionTrigger.LOGISTICS);

        assertThatThrownBy(() -> order.changeStatus(OrderStatus.ESPERANDO_RETIRO, OrderTransitionTrigger.LOGISTICS))
                .isInstanceOf(InvalidOrderStatusTransitionException.class)
                .hasMessageContaining("LOGISTICS");
        assertThatThrownBy(() -> order.changeStatus(OrderStatus.DEVUELTO, OrderTransitionTrigger.RETURN_REQUEST))
                .isInstanceOf(InvalidOrderStatusTransitionException.class);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.ENTREGADO);
    }

    // Recorre todo el ciclo de vida: compra, envio y devolucion.
    private static void returnFully(Order order) {
        order.changeStatus(OrderStatus.EN_PREPARACION, OrderTransitionTrigger.PAYMENT);
        order.changeStatus(OrderStatus.LISTO_PARA_DESPACHAR, OrderTransitionTrigger.LOGISTICS);
        order.changeStatus(OrderStatus.DESPACHADO, OrderTransitionTrigger.LOGISTICS);
        order.changeStatus(OrderStatus.ENTREGADO, OrderTransitionTrigger.LOGISTICS);
        order.changeStatus(OrderStatus.ESPERANDO_RETIRO, OrderTransitionTrigger.RETURN_REQUEST);
        order.changeStatus(OrderStatus.RETIRADO, OrderTransitionTrigger.LOGISTICS);
        order.changeStatus(OrderStatus.DEVUELTO, OrderTransitionTrigger.LOGISTICS);
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
