package com.amancay.application.service;

import java.time.Clock;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.amancay.application.port.in.AdvanceOrderStatusCommand;
import com.amancay.application.port.in.ApplyOrderStatusCommandUseCase;
import com.amancay.application.port.out.OrderRepositoryPort;
import com.amancay.application.port.out.PublishOrderEventPort;
import com.amancay.domain.event.OrderStatusChangedEvent;
import com.amancay.domain.exception.OrderNotFoundException;
import com.amancay.domain.model.Order;
import com.amancay.domain.model.OrderStatus;
import com.amancay.domain.model.OrderTransitionTrigger;

// La logistica del pedido (desde EN_PREPARACION) la dispara un sistema externo publicando
// comandos en RabbitMQ (por ejemplo la consola de logistica); aca solo se aplican. Hasta
// EN_PREPARACION lo maneja la app (OrderService.markAsPaid).
@Service
public class OrderFulfillmentService implements ApplyOrderStatusCommandUseCase {

    private final OrderRepositoryPort orderRepository;
    private final PublishOrderEventPort publishOrderEventPort;
    private final Clock clock;

    public OrderFulfillmentService(OrderRepositoryPort orderRepository, PublishOrderEventPort publishOrderEventPort,
            Clock clock) {
        this.orderRepository = orderRepository;
        this.publishOrderEventPort = publishOrderEventPort;
        this.clock = clock;
    }

    @Override
    @Transactional
    public Result apply(AdvanceOrderStatusCommand command) {
        Order order = findOrder(command.orderId());
        if (order.getStatus() == command.targetStatus()) {
            return Result.ALREADY_APPLIED;
        }

        OrderStatus previousStatus = order.getStatus();
        order.changeStatus(command.targetStatus(), OrderTransitionTrigger.LOGISTICS);
        Order saved = orderRepository.save(order);
        publishOrderEventPort.publish(OrderStatusChangedEvent.of(saved, previousStatus, clock.instant()));
        return Result.APPLIED;
    }

    private Order findOrder(UUID id) {
        return orderRepository.findById(id).orElseThrow(() -> new OrderNotFoundException(id));
    }
}
