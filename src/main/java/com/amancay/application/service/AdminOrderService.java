package com.amancay.application.service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.amancay.application.port.in.ListAllOrdersQuery;
import com.amancay.application.port.out.BuyerEmailPort;
import com.amancay.domain.model.Order;
import com.amancay.domain.port.OrderRepositoryPort;

// Separado de OrderService porque no pasa por el chequeo de permisos por usuario:
// la autorizacion (solo ADMIN) la hace el controller.
@Service
public class AdminOrderService implements ListAllOrdersQuery {

    private final OrderRepositoryPort orderRepository;
    private final BuyerEmailPort buyerEmailPort;

    public AdminOrderService(OrderRepositoryPort orderRepository, BuyerEmailPort buyerEmailPort) {
        this.orderRepository = orderRepository;
        this.buyerEmailPort = buyerEmailPort;
    }

    @Override
    @Transactional(readOnly = true)
    public List<AdminOrder> listAll() {
        List<Order> orders = orderRepository.findAllNewestFirst();
        Set<UUID> buyerIds = orders.stream().map(Order::getUserId).collect(Collectors.toSet());
        Map<UUID, String> emailsByUserId = buyerEmailPort.findEmailsByUserId(buyerIds);
        return orders.stream()
                .map(order -> new AdminOrder(order, emailsByUserId.get(order.getUserId())))
                .toList();
    }
}
