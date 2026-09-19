package com.amancay.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.amancay.dto.CreateOrderRequest;
import com.amancay.dto.OrderDto;
import com.amancay.dto.OrderItemDto;
import com.amancay.entity.Address;
import com.amancay.entity.Order;
import com.amancay.entity.OrderItem;
import com.amancay.entity.OrderStatus;
import com.amancay.entity.Role;
import com.amancay.entity.ProductVariant;
import com.amancay.entity.User;
import com.amancay.event.OrderStatusChangedEvent;
import com.amancay.exceptions.OrderNotFoundException;
import com.amancay.exceptions.UserNotFoundException;
import com.amancay.repository.OrderRepository;
import com.amancay.repository.AddressRepository;
import com.amancay.repository.ProductVariantRepository;
import com.amancay.repository.UserRepository;

@Service
public class OrderService {
    private final OrderRepository orderRepository;
    private final AddressRepository addressRepository;
    private final UserRepository userRepository;
    private final ProductVariantRepository productVariantRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Autowired
    public OrderService(OrderRepository orderRepository, UserRepository userRepository,
            ProductVariantRepository productVariantRepository, AddressRepository addressRepository,
            ApplicationEventPublisher eventPublisher) {
        this.orderRepository = orderRepository;
        this.addressRepository = addressRepository;
        this.userRepository = userRepository;
        this.productVariantRepository = productVariantRepository;
        this.eventPublisher = eventPublisher;
    }

    public OrderService(OrderRepository orderRepository, UserRepository userRepository,
            ApplicationEventPublisher eventPublisher) {
        this(orderRepository, userRepository, null, null, eventPublisher);
    }

    @Transactional(readOnly = true)
    public List<OrderDto> listOrders(UUID requesterId, UUID requestedUserId) {
        UUID targetUserId = resolveTargetUserId(requesterId, requestedUserId);
        return orderRepository.findByUserId(targetUserId).stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<OrderDto> listOrders(UUID userId) {
        if (userId == null) {
            return orderRepository.findAll().stream()
                    .map(this::toDto)
                    .toList();
        }
        return orderRepository.findByUserId(userId).stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public OrderDto getOrder(UUID requesterId, UUID orderId, UUID requestedUserId) {
        UUID targetUserId = resolveTargetUserId(requesterId, requestedUserId);
        Order order = findOrder(orderId);
        if (!order.getUserId().equals(targetUserId)) {
            throw new AccessDeniedException("No tienes permisos para ver esta orden");
        }
        return toDto(order);
    }

    @Transactional(readOnly = true)
    public OrderDto getOrder(UUID orderId, UUID userId) {
        Order order = findOrder(orderId);
        if (userId != null && !order.getUserId().equals(userId)) {
            throw new AccessDeniedException("The order does not belong to the requested user");
        }
        return toDto(order);
    }

    @Transactional
    public OrderDto createOrder(CreateOrderRequest request) {
        Order order = new Order();
        order.setUserId(request.userId());
        order.setShippingAddressId(request.shippingAddressId());
        order.setShippingAddress(toShippingAddress(findAddress(request.shippingAddressId())));
        order.setSubtotal(BigDecimal.ZERO);
        order.setShippingCost(BigDecimal.ZERO);
        order.setTotal(BigDecimal.ZERO);

        BigDecimal subtotal = BigDecimal.ZERO;
        for (CreateOrderRequest.ItemRequest itemRequest : request.items()) {
            if (itemRequest.quantity() == null || itemRequest.quantity() <= 0) {
                throw new IllegalArgumentException("quantity must be greater than zero");
            }

            ProductVariant variant = findProductVariant(itemRequest.productVariantId());
            OrderItem item = new OrderItem();
            item.setProductVariant(variant);
            item.setQuantity(itemRequest.quantity());
            item.setUnitPrice(variant.getPrice());
            order.addItem(item);
            subtotal = subtotal.add(item.getSubtotal());
        }

        order.setSubtotal(subtotal);
        order.recalculateTotal();

        Order saved = orderRepository.saveAndFlush(order);
        return toDto(saved);
    }

    // Unico punto de entrada para mover el estado de un pedido: lo va a usar
    // tanto una accion sincrona (admin marca "despachado" desde el panel) como,
    // mas adelante, un consumer de la cola de mensajes (ej: el webhook de un
    // pago aprobado). La validacion de la transicion vive en OrderStatus (State).
    @Transactional
    public OrderDto changeStatus(UUID requesterId, UUID orderId, OrderStatus newStatus) {
        User requester = findUser(requesterId);
        Order order = findOrder(orderId);

        return changeStatus(order, newStatus);
    }

    @Transactional
    public OrderDto changeStatus(UUID orderId, OrderStatus newStatus) {
        return changeStatus(findOrder(orderId), newStatus);
    }

    private OrderDto changeStatus(Order order, OrderStatus newStatus) {

        OrderStatus previousStatus = order.getStatus();
        order.changeStatus(newStatus);
        Order saved = orderRepository.save(order);

        if (eventPublisher != null) {
            eventPublisher.publishEvent(new OrderStatusChangedEvent(saved.getId(), previousStatus, newStatus));
        }

        return toDto(saved);
    }

    private UUID resolveTargetUserId(UUID requesterId, UUID requestedUserId) {
        User requester = findUser(requesterId);
        if (requester.getRole() == Role.ADMIN) {
            return requestedUserId == null ? requesterId : requestedUserId;
        }
        if (requestedUserId != null && !requestedUserId.equals(requesterId)) {
            throw new AccessDeniedException("No puedes consultar órdenes de otro usuario");
        }
        return requesterId;
    }

    private Order findOrder(UUID id) {
        return orderRepository.findById(id).orElseThrow(() -> new OrderNotFoundException(id));
    }

    private ProductVariant findProductVariant(UUID id) {
        if (productVariantRepository == null) {
            throw new IllegalStateException("Product variant repository is not configured");
        }
        return productVariantRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Product variant not found: " + id));
    }

    private Address findAddress(UUID id) {
        if (id == null) {
            throw new IllegalArgumentException("shippingAddressId is required");
        }
        if (addressRepository == null) {
            throw new IllegalStateException("Address repository is not configured");
        }
        return addressRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Shipping address not found: " + id));
    }

    private com.amancay.entity.ShippingAddress toShippingAddress(Address address) {
        com.amancay.entity.ShippingAddress snapshot = new com.amancay.entity.ShippingAddress();
        snapshot.setStreet(address.getStreet());
        snapshot.setNumber(String.valueOf(address.getNumber()));
        snapshot.setFloorApt(address.getFloorApt());
        snapshot.setCity(address.getCity());
        snapshot.setProvince(address.getProvince());
        snapshot.setCountry(address.getCountry());
        snapshot.setPostalCode(address.getPostalCode());
        return snapshot;
    }

    private User findUser(UUID id) {
        return userRepository.findById(id).orElseThrow(() -> new UserNotFoundException(id));
    }

    private OrderDto toDto(Order order) {
        List<OrderItemDto> items = order.getItems() == null ? List.of()
                : order.getItems().stream()
                        .map(this::toDto)
                        .toList();
        return new OrderDto(order.getId(), order.getUserId(), order.getShippingAddressId(), order.getStatus(),
                order.getSubtotal(), order.getShippingCost(), order.getTotal(), order.getCreatedAt(),
                order.getUpdatedAt(), items);
    }

    private OrderItemDto toDto(OrderItem item) {
        return new OrderItemDto(item.getId(), item.getProductVariant() == null ? null : item.getProductVariant().getId(),
                item.getQuantity(), item.getUnitPrice(), item.getSubtotal());
    }
}
