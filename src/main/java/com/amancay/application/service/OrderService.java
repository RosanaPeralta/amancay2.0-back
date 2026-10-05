package com.amancay.application.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.amancay.application.port.in.ChangeOrderStatusUseCase;
import com.amancay.application.port.in.CreateOrderCommand;
import com.amancay.application.port.in.CreateOrderUseCase;
import com.amancay.application.port.in.GetOrderQuery;
import com.amancay.application.port.in.ListOrdersQuery;
import com.amancay.application.port.in.MarkOrderAsPaidUseCase;
import com.amancay.application.port.out.LoadProductVariantPort.ProductVariantInfo;
import com.amancay.application.port.out.LoadProductVariantPort;
import com.amancay.application.port.out.LoadRequesterPort.Requester;
import com.amancay.application.port.out.LoadRequesterPort;
import com.amancay.application.port.out.LoadShippingAddressPort;
import com.amancay.application.port.out.OrderRepositoryPort;
import com.amancay.application.port.out.PublishOrderEventPort;
import com.amancay.domain.event.OrderStatusChangedEvent;
import com.amancay.domain.exception.InsufficientStockException;
import com.amancay.domain.exception.OrderAccessDeniedException;
import com.amancay.domain.exception.OrderNotFoundException;
import com.amancay.domain.model.Order;
import com.amancay.domain.model.OrderItem;
import com.amancay.domain.model.OrderStatus;
import com.amancay.domain.model.ShippingAddress;

@Service
public class OrderService implements CreateOrderUseCase, ChangeOrderStatusUseCase, MarkOrderAsPaidUseCase, GetOrderQuery,
        ListOrdersQuery {

    private final OrderRepositoryPort orderRepository;
    private final LoadProductVariantPort loadProductVariantPort;
    private final LoadShippingAddressPort loadShippingAddressPort;
    private final LoadRequesterPort loadRequesterPort;
    private final PublishOrderEventPort publishOrderEventPort;
    private final AdminGuard adminGuard;

    public OrderService(OrderRepositoryPort orderRepository,
            LoadProductVariantPort loadProductVariantPort, LoadShippingAddressPort loadShippingAddressPort,
            LoadRequesterPort loadRequesterPort, PublishOrderEventPort publishOrderEventPort, AdminGuard adminGuard) {
        this.orderRepository = orderRepository;
        this.loadProductVariantPort = loadProductVariantPort;
        this.loadShippingAddressPort = loadShippingAddressPort;
        this.loadRequesterPort = loadRequesterPort;
        this.publishOrderEventPort = publishOrderEventPort;
        this.adminGuard = adminGuard;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Order> list(UUID requesterId, UUID requestedUserId) {
        UUID targetUserId = resolveTargetUserId(requesterId, requestedUserId);
        return orderRepository.findByUserId(targetUserId);
    }

    @Override
    @Transactional(readOnly = true)
    public Order get(UUID requesterId, UUID orderId, UUID requestedUserId) {
        UUID targetUserId = resolveTargetUserId(requesterId, requestedUserId);
        Order order = findOrder(orderId);
        if (!order.belongsTo(targetUserId)) {
            throw new OrderAccessDeniedException("No tienes permisos para ver esta orden");
        }
        return order;
    }

    @Override
    @Transactional(readOnly = true)
    public Order getById(UUID orderId) {
        return findOrder(orderId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Order> getAllById(Collection<UUID> orderIds) {
        return orderRepository.findAllById(orderIds);
    }

    // Aviso temprano, no reserva: el stock se descuenta cuando se aprueba el pago.
    @Override
    @Transactional
    public Order create(CreateOrderCommand command) {
        ShippingAddress shippingAddress = loadShippingAddressPort.findById(command.shippingAddressId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Shipping address not found: " + command.shippingAddressId()));

        List<OrderItem> items = new ArrayList<>();
        for (CreateOrderCommand.Item itemCommand : command.items()) {
            ProductVariantInfo variant = loadProductVariantPort.findById(itemCommand.productVariantId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Product variant not found: " + itemCommand.productVariantId()));
            if (variant.stockQuantity() < itemCommand.quantity()) {
                throw new InsufficientStockException(variant.id());
            }
            items.add(OrderItem.create(variant.id(), itemCommand.quantity(), variant.price()));
        }

        Order order = Order.create(command.requesterId(), command.shippingAddressId(), shippingAddress, items);
        return orderRepository.save(order);
    }

    @Override
    @Transactional
    public Order changeStatus(UUID requesterId, UUID orderId, OrderStatus newStatus) {
        adminGuard.requireAdmin(requesterId);
        return applyStatus(orderId, newStatus);
    }

    @Override
    @Transactional
    public void markAsPaid(UUID orderId) {
        applyStatus(orderId, OrderStatus.EN_PREPARACION);
    }

    private Order applyStatus(UUID orderId, OrderStatus newStatus) {
        Order order = findOrder(orderId);
        OrderStatus previousStatus = order.getStatus();
        order.changeStatus(newStatus);
        Order saved = orderRepository.save(order);

        publishOrderEventPort.publish(new OrderStatusChangedEvent(saved.getId(), previousStatus, newStatus));
        return saved;
    }

    private UUID resolveTargetUserId(UUID requesterId, UUID requestedUserId) {
        Requester requester = loadRequesterPort.load(requesterId);
        if (requester.admin()) {
            return requestedUserId == null ? requesterId : requestedUserId;
        }
        if (requestedUserId != null && !requestedUserId.equals(requesterId)) {
            throw new OrderAccessDeniedException("No puedes consultar órdenes de otro usuario");
        }
        return requesterId;
    }

    private Order findOrder(UUID id) {
        return orderRepository.findById(id).orElseThrow(() -> new OrderNotFoundException(id));
    }
}
