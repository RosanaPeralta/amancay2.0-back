package com.amancay.infrastructure.adapters.out.persistence;

import java.util.UUID;

import org.springframework.stereotype.Component;

import com.amancay.domain.model.OrderLine;
import com.amancay.domain.model.OrderState;
import com.amancay.domain.model.PurchaseOrder;
import com.amancay.domain.ports.out.PaymentOrderPort;
import com.amancay.dto.OrderDto;
import com.amancay.exceptions.OrderNotFoundException;
import com.amancay.infrastructure.adapters.out.persistence.mapper.OrderPersistenceMapper;
import com.amancay.infrastructure.adapters.out.persistence.repository.OrderRepository;
import com.amancay.service.OrderService;

@Component
public class PaymentOrderAdapter implements PaymentOrderPort {
    private final OrderService orderService;
    private final OrderRepository orderRepository;

    public PaymentOrderAdapter(OrderService orderService, OrderRepository orderRepository) {
        this.orderService = orderService;
        this.orderRepository = orderRepository;
    }

    @Override
    public void authorize(UUID requesterId, UUID orderId) {
        orderService.getOrder(requesterId, orderId, null);
    }

    @Override
    public PurchaseOrder getOrder(UUID orderId) {
        return orderRepository.findById(orderId).map(OrderPersistenceMapper::toDomain)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
    }

    @Override
    public void changeStatus(UUID orderId, OrderState status) {
        orderService.changeStatus(orderId, com.amancay.entity.OrderStatus.valueOf(status.name()));
    }
}