package com.amancay.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.amancay.application.port.in.AdvanceOrderStatusCommand;
import com.amancay.application.port.in.ApplyOrderStatusCommandUseCase.Result;
import com.amancay.application.service.fake.Admins;
import com.amancay.application.service.fake.InMemoryOrderRepository;
import com.amancay.domain.event.OrderStatusChangedEvent;
import com.amancay.domain.exception.AdminRequiredException;
import com.amancay.domain.exception.InvalidOrderStatusTransitionException;
import com.amancay.domain.exception.OrderNotFoundException;
import com.amancay.domain.model.OrderFixtures;
import com.amancay.domain.model.OrderStatus;
import com.amancay.domain.model.OrderStatusChange;

class OrderFulfillmentServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-14T12:00:00Z");

    private final InMemoryOrderRepository orders = new InMemoryOrderRepository();
    private final List<AdvanceOrderStatusCommand> sentCommands = new ArrayList<>();
    private final List<OrderStatusChangedEvent> publishedEvents = new ArrayList<>();

    private final OrderFulfillmentService service = new OrderFulfillmentService(orders, sentCommands::add,
            publishedEvents::add, Admins.guard(), Clock.fixed(NOW, ZoneOffset.UTC));

    @Test
    void adminRequestQueuesTheCommandWithoutChangingTheOrder() {
        UUID orderId = order(OrderStatus.EN_PREPARACION);

        AdvanceOrderStatusCommand command = service.requestAdvance(Admins.ADMIN_ID, orderId, OrderStatus.DESPACHADO);

        assertThat(command.commandId()).isNotNull();
        assertThat(command.orderId()).isEqualTo(orderId);
        assertThat(command.targetStatus()).isEqualTo(OrderStatus.DESPACHADO);
        assertThat(command.requestedBy()).isEqualTo("admin:" + Admins.ADMIN_ID);
        assertThat(command.requestedAt()).isEqualTo(NOW);
        assertThat(sentCommands).containsExactly(command);
        assertThat(orders.findById(orderId).orElseThrow().getStatus()).isEqualTo(OrderStatus.EN_PREPARACION);
        assertThat(publishedEvents).isEmpty();
    }

    @Test
    void adminCannotQueueTheStepThatBelongsToThePayment() {
        UUID orderId = order(OrderStatus.CREADO);

        assertThatThrownBy(() -> service.requestAdvance(Admins.ADMIN_ID, orderId, OrderStatus.EN_PREPARACION))
                .isInstanceOf(InvalidOrderStatusTransitionException.class);
        assertThat(sentCommands).isEmpty();
    }

    @Test
    void adminCannotQueueAReturn() {
        UUID orderId = order(OrderStatus.ENTREGADO);

        assertThatThrownBy(() -> service.requestAdvance(Admins.ADMIN_ID, orderId, OrderStatus.DEVUELTO))
                .isInstanceOf(InvalidOrderStatusTransitionException.class);
        assertThat(sentCommands).isEmpty();
    }

    @Test
    void adminCannotQueueASkippedStep() {
        UUID orderId = order(OrderStatus.EN_PREPARACION);

        assertThatThrownBy(() -> service.requestAdvance(Admins.ADMIN_ID, orderId, OrderStatus.ENTREGADO))
                .isInstanceOf(InvalidOrderStatusTransitionException.class);
        assertThat(sentCommands).isEmpty();
    }

    @Test
    void buyerCannotQueueAStatusChange() {
        UUID orderId = order(OrderStatus.EN_PREPARACION);

        assertThatThrownBy(() -> service.requestAdvance(UUID.randomUUID(), orderId, OrderStatus.DESPACHADO))
                .isInstanceOf(AdminRequiredException.class);
        assertThat(sentCommands).isEmpty();
    }

    @Test
    void requestForAnUnknownOrderIsNotFound() {
        assertThatThrownBy(() -> service.requestAdvance(Admins.ADMIN_ID, UUID.randomUUID(), OrderStatus.DESPACHADO))
                .isInstanceOf(OrderNotFoundException.class);
    }

    @Test
    void applyingTheCommandPersistsTheStatusAndPublishesTheEvent() {
        UUID buyerId = UUID.randomUUID();
        UUID orderId = order(OrderStatus.EN_PREPARACION, buyerId);

        Result result = service.apply(command(orderId, OrderStatus.DESPACHADO));

        assertThat(result).isEqualTo(Result.APPLIED);
        assertThat(orders.findById(orderId).orElseThrow().getStatusHistory())
                .extracting(OrderStatusChange::status).containsExactly(OrderStatus.DESPACHADO);
        assertThat(publishedEvents).singleElement().satisfies(event -> {
            assertThat(event.orderId()).isEqualTo(orderId);
            assertThat(event.buyerId()).isEqualTo(buyerId);
            assertThat(event.previousStatus()).isEqualTo(OrderStatus.EN_PREPARACION);
            assertThat(event.newStatus()).isEqualTo(OrderStatus.DESPACHADO);
            assertThat(event.occurredAt()).isEqualTo(NOW);
        });
    }

    @Test
    void aRedeliveredCommandIsIgnored() {
        UUID orderId = order(OrderStatus.EN_PREPARACION);
        AdvanceOrderStatusCommand command = command(orderId, OrderStatus.DESPACHADO);

        service.apply(command);
        Result second = service.apply(command);

        assertThat(second).isEqualTo(Result.ALREADY_APPLIED);
        assertThat(orders.findById(orderId).orElseThrow().getStatusHistory()).hasSize(1);
        assertThat(publishedEvents).hasSize(1);
    }

    @Test
    void theQueueCannotMarkAnOrderAsPaid() {
        UUID orderId = order(OrderStatus.CREADO);

        assertThatThrownBy(() -> service.apply(command(orderId, OrderStatus.EN_PREPARACION)))
                .isInstanceOf(InvalidOrderStatusTransitionException.class);
        assertThat(orders.findById(orderId).orElseThrow().getStatus()).isEqualTo(OrderStatus.CREADO);
        assertThat(publishedEvents).isEmpty();
    }

    @Test
    void theQueueCannotSkipAStep() {
        UUID orderId = order(OrderStatus.EN_PREPARACION);

        assertThatThrownBy(() -> service.apply(command(orderId, OrderStatus.ENTREGADO)))
                .isInstanceOf(InvalidOrderStatusTransitionException.class);
        assertThat(publishedEvents).isEmpty();
    }

    @Test
    void applyingACommandForAnUnknownOrderIsNotFound() {
        assertThatThrownBy(() -> service.apply(command(UUID.randomUUID(), OrderStatus.DESPACHADO)))
                .isInstanceOf(OrderNotFoundException.class);
    }

    private UUID order(OrderStatus status) {
        return order(status, UUID.randomUUID());
    }

    private UUID order(OrderStatus status, UUID buyerId) {
        UUID orderId = UUID.randomUUID();
        orders.put(OrderFixtures.persisted(orderId, buyerId, status));
        return orderId;
    }

    private static AdvanceOrderStatusCommand command(UUID orderId, OrderStatus target) {
        return new AdvanceOrderStatusCommand(UUID.randomUUID(), orderId, target, "logistics-console", NOW);
    }
}
