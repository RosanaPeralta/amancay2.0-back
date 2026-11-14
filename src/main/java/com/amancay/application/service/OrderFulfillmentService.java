package com.amancay.application.service;

import java.time.Clock;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.amancay.application.port.in.AdvanceOrderStatusCommand;
import com.amancay.application.port.in.ApplyOrderStatusCommandUseCase;
import com.amancay.application.port.in.RequestOrderAdvanceUseCase;
import com.amancay.application.port.out.OrderRepositoryPort;
import com.amancay.application.port.out.PublishOrderEventPort;
import com.amancay.application.port.out.SendOrderCommandPort;
import com.amancay.domain.event.OrderStatusChangedEvent;
import com.amancay.domain.exception.InvalidOrderStatusTransitionException;
import com.amancay.domain.exception.OrderNotFoundException;
import com.amancay.domain.model.Order;
import com.amancay.domain.model.OrderStatus;
import com.amancay.domain.model.OrderTransitionTrigger;

// La logistica del pedido (desde EN_PREPARACION) avanza por cola: aca se arma el comando
// y aca se aplica cuando llega. Hasta EN_PREPARACION lo maneja la app (OrderService.markAsPaid).
@Service
public class OrderFulfillmentService implements RequestOrderAdvanceUseCase, ApplyOrderStatusCommandUseCase {

    private final OrderRepositoryPort orderRepository;
    private final SendOrderCommandPort sendOrderCommandPort;
    private final PublishOrderEventPort publishOrderEventPort;
    private final AdminGuard adminGuard;
    private final Clock clock;

    public OrderFulfillmentService(OrderRepositoryPort orderRepository, SendOrderCommandPort sendOrderCommandPort,
            PublishOrderEventPort publishOrderEventPort, AdminGuard adminGuard, Clock clock) {
        this.orderRepository = orderRepository;
        this.sendOrderCommandPort = sendOrderCommandPort;
        this.publishOrderEventPort = publishOrderEventPort;
        this.adminGuard = adminGuard;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public AdvanceOrderStatusCommand requestAdvance(UUID requesterId, UUID orderId, OrderStatus targetStatus) {
        adminGuard.requireAdmin(requesterId);
        Order order = findOrder(orderId);
        // El consumidor vuelve a validar, pero asi el admin ve el error en el momento.
        requireFulfillmentTransition(order.getStatus(), targetStatus);

        AdvanceOrderStatusCommand command = new AdvanceOrderStatusCommand(UUID.randomUUID(), orderId, targetStatus,
                "admin:" + requesterId, clock.instant());
        sendOrderCommandPort.send(command);
        return command;
    }

    @Override
    @Transactional
    public Result apply(AdvanceOrderStatusCommand command) {
        Order order = findOrder(command.orderId());
        if (order.getStatus() == command.targetStatus()) {
            return Result.ALREADY_APPLIED;
        }

        OrderStatus previousStatus = order.getStatus();
        order.changeStatus(command.targetStatus(), OrderTransitionTrigger.FULFILLMENT);
        Order saved = orderRepository.save(order);
        publishOrderEventPort.publish(OrderStatusChangedEvent.of(saved, previousStatus, clock.instant()));
        return Result.APPLIED;
    }

    private static void requireFulfillmentTransition(OrderStatus current, OrderStatus target) {
        if (!current.isAdvancedBy(OrderTransitionTrigger.FULFILLMENT) || !current.canTransitionTo(target)) {
            throw new InvalidOrderStatusTransitionException(current, target, OrderTransitionTrigger.FULFILLMENT);
        }
    }

    private Order findOrder(UUID id) {
        return orderRepository.findById(id).orElseThrow(() -> new OrderNotFoundException(id));
    }
}
