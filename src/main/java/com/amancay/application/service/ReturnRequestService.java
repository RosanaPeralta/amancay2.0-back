package com.amancay.application.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.amancay.application.port.in.CreateReturnRequestUseCase;
import com.amancay.application.port.out.LoadRequesterPort;
import com.amancay.application.port.out.OrderRepositoryPort;
import com.amancay.application.port.out.ReturnRequestRepositoryPort;
import com.amancay.domain.exception.OrderAccessDeniedException;
import com.amancay.domain.exception.OrderNotFoundException;
import com.amancay.domain.model.Order;
import com.amancay.domain.model.OrderStatus;
import com.amancay.domain.model.ReturnReason;
import com.amancay.domain.model.ReturnRequest;

@Service
public class ReturnRequestService implements CreateReturnRequestUseCase {

    private static final Duration RETURN_WINDOW = Duration.ofDays(10);

    private final OrderRepositoryPort orderRepository;
    private final ReturnRequestRepositoryPort returnRequestRepository;
    private final LoadRequesterPort loadRequesterPort;
    private final Clock clock;

    public ReturnRequestService(
            OrderRepositoryPort orderRepository,
            ReturnRequestRepositoryPort returnRequestRepository,
            LoadRequesterPort loadRequesterPort,
            Clock clock) {
        this.orderRepository = orderRepository;
        this.returnRequestRepository = returnRequestRepository;
        this.loadRequesterPort = loadRequesterPort;
        this.clock = clock;
    }

    @Override
    @Transactional
    public ReturnRequest create(
            UUID requesterId,
            UUID orderId,
            ReturnReason reason,
            String comment) {

        // Verifica que el usuario autenticado exista.
        loadRequesterPort.load(requesterId);

        // Busca el pedido.
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));

        // Un cliente solamente puede solicitar la devolución de sus propios pedidos.
        if (!order.belongsTo(requesterId)) {
            throw new OrderAccessDeniedException(
                    "No puedes solicitar la devolución de un pedido que no te pertenece");
        }

        // El pedido debe estar entregado.
        if (order.getStatus() != OrderStatus.ENTREGADO) {
            throw new IllegalArgumentException(
                    "Only delivered orders can be returned");
        }

        // Obtiene la fecha real de entrega desde el historial.
        Instant deliveredAt = order.getDeliveredAt()
                .orElseThrow(() -> new IllegalArgumentException(
                        "The order has no valid delivery date"));

        // El plazo incluye el instante exacto en que se cumplen los diez días.
        Instant now = clock.instant();
        Instant deadline = deliveredAt.plus(RETURN_WINDOW);

        if (now.isAfter(deadline)) {
            throw new IllegalArgumentException(
                    "The return period of 10 days has expired");
        }

        // Solo se admite una solicitud por pedido.
        if (returnRequestRepository.findByOrderId(orderId).isPresent()) {
            throw new IllegalStateException(
                    "A return request already exists for this order");
        }

        ReturnRequest request = ReturnRequest.create(
                orderId,
                reason,
                comment,
                now);

        return returnRequestRepository.save(request);
    }
}