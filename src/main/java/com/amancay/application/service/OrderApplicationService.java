package com.amancay.application.service;

import java.util.List;
import java.util.UUID;

import org.springframework.security.access.AccessDeniedException;

import com.amancay.domain.event.OrderStateChanged;
import com.amancay.domain.model.CustomerAddress;
import com.amancay.domain.model.OrderLine;
import com.amancay.domain.model.OrderState;
import com.amancay.domain.model.PurchaseOrder;
import com.amancay.domain.model.UserProfile;
import com.amancay.domain.model.UserRole;
import com.amancay.domain.model.VariantStock;
import com.amancay.domain.ports.in.OrderUseCases;
import com.amancay.domain.ports.out.OrderAddressPort;
import com.amancay.domain.ports.out.OrderCatalogPort;
import com.amancay.domain.ports.out.OrderEventPort;
import com.amancay.domain.ports.out.OrderUserPort;
import com.amancay.domain.ports.out.VariantStockPort;
import com.amancay.exceptions.InsufficientStockException;
import com.amancay.exceptions.OrderNotFoundException;
import com.amancay.exceptions.UserNotFoundException;

public class OrderApplicationService implements OrderUseCases {
    private final OrderCatalogPort orderCatalog;
    private final OrderUserPort users;
    private final OrderAddressPort addresses;
    private final VariantStockPort variants;
    private final OrderEventPort events;

    public OrderApplicationService(OrderCatalogPort orderCatalog, OrderUserPort users, OrderAddressPort addresses,
            VariantStockPort variants, OrderEventPort events) {
        this.orderCatalog = orderCatalog;
        this.users = users;
        this.addresses = addresses;
        this.variants = variants;
        this.events = events;
    }

    @Override
    public List<PurchaseOrder> list(UUID requesterId, UUID requestedUserId) {
        return orderCatalog.findByUserId(resolveTargetUserId(requesterId, requestedUserId));
    }

    @Override
    public PurchaseOrder get(UUID requesterId, UUID orderId, UUID requestedUserId) {
        UUID targetUserId = resolveTargetUserId(requesterId, requestedUserId);
        PurchaseOrder order = findOrder(orderId);
        if (!order.userId().equals(targetUserId)) {
            throw new AccessDeniedException("No tienes permisos para ver esta orden");
        }
        return order;
    }

    @Override
    public PurchaseOrder create(UUID requesterId, UUID addressId, List<OrderItemInput> requestedItems) {
        if (addressId == null) {
            throw new IllegalArgumentException("shippingAddressId is required");
        }
        CustomerAddress address = addresses.findAddressById(addressId)
                .orElseThrow(() -> new IllegalArgumentException("Shipping address not found: " + addressId));
        List<OrderLine> items = requestedItems.stream().map(item -> {
            if (item.quantity() == null || item.quantity() <= 0) {
                throw new IllegalArgumentException("quantity must be greater than zero");
            }
            VariantStock variant = variants.findVariantById(item.productVariantId())
                    .orElseThrow(() -> new IllegalArgumentException("Product variant not found: " + item.productVariantId()));
            if (variant.quantityAvailable() < item.quantity()) {
                throw new InsufficientStockException(variant.id());
            }
            return new OrderLine(null, variant.id(), item.quantity(), variant.price());
        }).toList();
        return orderCatalog.save(PurchaseOrder.create(requesterId, addressId,
                new com.amancay.domain.model.ShippingAddressSnapshot(address.street(), String.valueOf(address.number()),
                        address.floorApt(), address.city(), address.province(), address.country(), address.postalCode()),
                items));
    }

    @Override
    public PurchaseOrder changeStatus(UUID requesterId, UUID orderId, OrderState newState) {
        findUser(requesterId);
        return changeStatus(findOrder(orderId), newState);
    }

    @Override
    public PurchaseOrder changeStatus(UUID orderId, OrderState newState) {
        return changeStatus(findOrder(orderId), newState);
    }

    private PurchaseOrder changeStatus(PurchaseOrder order, OrderState newState) {
        OrderState previous = order.state();
        PurchaseOrder updated;
        try {
            updated = order.transitionTo(newState);
        } catch (IllegalStateException exception) {
            throw new com.amancay.exceptions.InvalidOrderStatusTransitionException(
                    com.amancay.entity.OrderStatus.valueOf(previous.name()),
                    com.amancay.entity.OrderStatus.valueOf(newState.name()));
        }
        PurchaseOrder saved = orderCatalog.save(updated);
        if (events != null) {
            events.publish(new OrderStateChanged(saved.id(), previous, newState));
        }
        return saved;
    }

    private UUID resolveTargetUserId(UUID requesterId, UUID requestedUserId) {
        UserProfile requester = findUser(requesterId);
        if (requester.role() == UserRole.ADMIN) {
            return requestedUserId == null ? requesterId : requestedUserId;
        }
        if (requestedUserId != null && !requestedUserId.equals(requesterId)) {
            throw new AccessDeniedException("No puedes consultar órdenes de otro usuario");
        }
        return requesterId;
    }

    private PurchaseOrder findOrder(UUID id) {
        return orderCatalog.findById(id).orElseThrow(() -> new OrderNotFoundException(id));
    }

    private UserProfile findUser(UUID id) {
        return users.findUserById(id).orElseThrow(() -> new UserNotFoundException(id));
    }
}