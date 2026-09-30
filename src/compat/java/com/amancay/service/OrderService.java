package com.amancay.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.amancay.application.service.OrderApplicationService;
import com.amancay.domain.model.OrderState;
import com.amancay.domain.model.PurchaseOrder;
import com.amancay.domain.ports.in.OrderUseCases;
import com.amancay.domain.ports.in.OrderUseCases.OrderItemInput;
import com.amancay.domain.ports.out.OrderAddressPort;
import com.amancay.domain.ports.out.OrderCatalogPort;
import com.amancay.domain.ports.out.OrderEventPort;
import com.amancay.domain.ports.out.OrderUserPort;
import com.amancay.domain.ports.out.VariantStockPort;
import com.amancay.dto.CreateOrderRequest;
import com.amancay.dto.OrderDto;
import com.amancay.entity.OrderStatus;
import com.amancay.infrastructure.adapters.in.rest.mapper.OrderApiMapper;
import com.amancay.infrastructure.adapters.out.events.SpringOrderEventAdapter;
import com.amancay.infrastructure.adapters.out.persistence.OrderPersistenceAdapter;
import com.amancay.repository.OrderRepository;
import com.amancay.repository.AddressRepository;
import com.amancay.repository.ProductVariantRepository;
import com.amancay.repository.UserRepository;
import org.springframework.context.ApplicationEventPublisher;

@Service
public class OrderService {
    private final OrderUseCases orderUseCases;

    @Autowired
    public OrderService(OrderUseCases orderUseCases) {
        this.orderUseCases = orderUseCases;
    }

    public OrderService(OrderRepository orderRepository, UserRepository userRepository,
            ProductVariantRepository productVariantRepository, AddressRepository addressRepository,
            ApplicationEventPublisher eventPublisher) {
        OrderPersistenceAdapter persistence = new OrderPersistenceAdapter(orderRepository, userRepository,
                productVariantRepository, addressRepository);
        OrderEventPort events = eventPublisher == null ? null
                : new SpringOrderEventAdapter(eventPublisher);
        this.orderUseCases = new OrderApplicationService(persistence, (OrderUserPort) persistence,
                (OrderAddressPort) persistence, (VariantStockPort) persistence, events);
    }

    public OrderService(OrderRepository orderRepository, UserRepository userRepository,
            org.springframework.context.ApplicationEventPublisher eventPublisher) {
        this(orderRepository, userRepository, null, null, eventPublisher);
    }

    @Transactional(readOnly = true)
    public List<OrderDto> listOrders(UUID requesterId, UUID requestedUserId) {
        return orderUseCases.list(requesterId, requestedUserId).stream().map(OrderApiMapper::toDto).toList();
    }

    @Transactional(readOnly = true)
    public OrderDto getOrder(UUID requesterId, UUID orderId, UUID requestedUserId) {
        return OrderApiMapper.toDto(orderUseCases.get(requesterId, orderId, requestedUserId));
    }

    // No reserva stock: es solo un aviso temprano de "esto ya no esta disponible" con el
    // valor que hay en este instante. El stock real se descuenta cuando un pago se aprueba
    // (PaymentService), para no bloquear unidades por una orden que nunca se paga.
    @Transactional
    public OrderDto createOrder(UUID requesterId, CreateOrderRequest request) {
        List<OrderItemInput> items = request.items().stream()
                .map(item -> new OrderItemInput(item.productVariantId(), item.quantity())).toList();
        return OrderApiMapper.toDto(orderUseCases.create(requesterId, request.shippingAddressId(), items));
    }

    // Unico punto de entrada para mover el estado de un pedido: lo va a usar
    // tanto una accion sincrona (admin marca "despachado" desde el panel) como,
    // mas adelante, un consumer de la cola de mensajes (ej: el webhook de un
    // pago aprobado). La validacion de la transicion vive en OrderStatus (State).
    @Transactional
    public OrderDto changeStatus(UUID requesterId, UUID orderId, OrderStatus newStatus) {
        return OrderApiMapper.toDto(orderUseCases.changeStatus(requesterId, orderId, OrderState.valueOf(newStatus.name())));
    }

    @Transactional
    public OrderDto changeStatus(UUID orderId, OrderStatus newStatus) {
        return OrderApiMapper.toDto(orderUseCases.changeStatus(orderId, OrderState.valueOf(newStatus.name())));
    }
}
