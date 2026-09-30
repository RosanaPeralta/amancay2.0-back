package com.amancay.order.application.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.amancay.exceptions.InsufficientStockException;
import com.amancay.order.application.port.in.ChangeOrderStatusUseCase;
import com.amancay.order.application.port.in.CreateOrderCommand;
import com.amancay.order.application.port.in.CreateOrderUseCase;
import com.amancay.order.application.port.in.GetOrderQuery;
import com.amancay.order.application.port.in.ListOrdersQuery;
import com.amancay.order.application.port.out.LoadOrderPort;
import com.amancay.order.application.port.out.LoadProductVariantPort;
import com.amancay.order.application.port.out.LoadProductVariantPort.ProductVariantInfo;
import com.amancay.order.application.port.out.LoadRequesterPort;
import com.amancay.order.application.port.out.LoadRequesterPort.Requester;
import com.amancay.order.application.port.out.LoadShippingAddressPort;
import com.amancay.order.application.port.out.PublishOrderEventPort;
import com.amancay.order.application.port.out.SaveOrderPort;
import com.amancay.order.domain.event.OrderStatusChangedEvent;
import com.amancay.order.domain.exception.OrderAccessDeniedException;
import com.amancay.order.domain.exception.OrderNotFoundException;
import com.amancay.order.domain.model.Order;
import com.amancay.order.domain.model.OrderItem;
import com.amancay.order.domain.model.OrderStatus;
import com.amancay.order.domain.model.ShippingAddress;

@Service
public class OrderService implements CreateOrderUseCase, ChangeOrderStatusUseCase, GetOrderQuery, ListOrdersQuery {

    private final LoadOrderPort loadOrderPort;
    private final SaveOrderPort saveOrderPort;
    private final LoadProductVariantPort loadProductVariantPort;
    private final LoadShippingAddressPort loadShippingAddressPort;
    private final LoadRequesterPort loadRequesterPort;
    private final PublishOrderEventPort publishOrderEventPort;

    public OrderService(LoadOrderPort loadOrderPort, SaveOrderPort saveOrderPort,
            LoadProductVariantPort loadProductVariantPort, LoadShippingAddressPort loadShippingAddressPort,
            LoadRequesterPort loadRequesterPort, PublishOrderEventPort publishOrderEventPort) {
        this.loadOrderPort = loadOrderPort;
        this.saveOrderPort = saveOrderPort;
        this.loadProductVariantPort = loadProductVariantPort;
        this.loadShippingAddressPort = loadShippingAddressPort;
        this.loadRequesterPort = loadRequesterPort;
        this.publishOrderEventPort = publishOrderEventPort;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Order> list(UUID requesterId, UUID requestedUserId) {
        UUID targetUserId = resolveTargetUserId(requesterId, requestedUserId);
        return loadOrderPort.findByUserId(targetUserId);
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
        return loadOrderPort.findAllById(orderIds);
    }

    // No reserva stock: es solo un aviso temprano de "esto ya no esta disponible" con el
    // valor que hay en este instante. El stock real se descuenta cuando un pago se aprueba
    // (PaymentService), para no bloquear unidades por una orden que nunca se paga.
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
        return saveOrderPort.save(order);
    }

    @Override
    @Transactional
    public Order changeStatus(UUID orderId, OrderStatus newStatus) {
        Order order = findOrder(orderId);
        OrderStatus previousStatus = order.getStatus();
        order.changeStatus(newStatus);
        Order saved = saveOrderPort.save(order);

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
        return loadOrderPort.findById(id).orElseThrow(() -> new OrderNotFoundException(id));
    }
}
