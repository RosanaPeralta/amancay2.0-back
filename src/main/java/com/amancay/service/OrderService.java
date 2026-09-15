package com.amancay.service;

import java.util.UUID;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.amancay.entity.Order;
import com.amancay.entity.OrderStatus;
import com.amancay.event.OrderStatusChangedEvent;
import com.amancay.exceptions.OrderNotFoundException;
import com.amancay.repository.OrderRepository;

@Service
public class OrderService {
    private final OrderRepository orderRepository;
    private final ApplicationEventPublisher eventPublisher;

    public OrderService(OrderRepository orderRepository, ApplicationEventPublisher eventPublisher) {
        this.orderRepository = orderRepository;
        this.eventPublisher = eventPublisher;
    }

    // Unico punto de entrada para mover el estado de un pedido: lo va a usar
    // tanto una accion sincrona (admin marca "despachado" desde el panel) como,
    // mas adelante, un consumer de la cola de mensajes (ej: el webhook de un
    // pago aprobado). La validacion de la transicion vive en OrderStatus (State).
    @Transactional
    public Order changeStatus(UUID orderId, OrderStatus newStatus) {
        Order order = findOrder(orderId);
        OrderStatus previousStatus = order.getStatus();

        order.changeStatus(newStatus);
        Order saved = orderRepository.save(order);

        eventPublisher.publishEvent(new OrderStatusChangedEvent(saved.getId(), previousStatus, newStatus));

        return saved;
    }

    private Order findOrder(UUID id) {
        return orderRepository.findById(id).orElseThrow(() -> new OrderNotFoundException(id));
    }
}
