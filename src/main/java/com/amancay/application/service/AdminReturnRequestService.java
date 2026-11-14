
package com.amancay.application.service;

import java.time.Clock;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.amancay.application.port.in.ApproveReturnRequestUseCase;
import com.amancay.application.port.in.ListReturnRequestsUseCase;
import com.amancay.application.port.in.RejectReturnRequestUseCase;
import com.amancay.application.port.out.OrderRepositoryPort;
import com.amancay.application.port.out.PublishOrderEventPort;
import com.amancay.application.port.out.ReturnRequestRepositoryPort;
import com.amancay.domain.event.OrderStatusChangedEvent;
import com.amancay.domain.model.Order;
import com.amancay.domain.model.OrderStatus;
import com.amancay.domain.model.ReturnRequest;
import com.amancay.domain.model.ReturnRequestStatus;
import java.util.List;

@Service
public class AdminReturnRequestService
        implements ApproveReturnRequestUseCase,
        RejectReturnRequestUseCase,
        ListReturnRequestsUseCase {

    private final ReturnRequestRepositoryPort returnRequestRepository;
    private final OrderRepositoryPort orderRepository;
    private final AdminGuard adminGuard;
    private final PublishOrderEventPort publishOrderEventPort;
    private final Clock clock;

    public AdminReturnRequestService(
            ReturnRequestRepositoryPort returnRequestRepository,
            OrderRepositoryPort orderRepository,
            AdminGuard adminGuard,
            PublishOrderEventPort publishOrderEventPort,
            Clock clock) {
        this.returnRequestRepository = returnRequestRepository;
        this.orderRepository = orderRepository;
        this.adminGuard = adminGuard;
        this.publishOrderEventPort = publishOrderEventPort;
        this.clock = clock;
    }

    @Override
    @Transactional
    public ReturnRequest approve(UUID adminId, UUID requestId) {
        adminGuard.requireAdmin(adminId);

        ReturnRequest request = findRequest(requestId);
        Order order = findOrder(request.getOrderId());

        OrderStatus previousStatus = order.getStatus();

        if (previousStatus != OrderStatus.ENTREGADO) {
            throw new IllegalStateException(
                    "Only delivered orders can have an approved return");
        }

        request.approve(adminId, clock.instant());
        order.changeStatus(OrderStatus.DEVUELTO);

        ReturnRequest savedRequest = returnRequestRepository.save(request);
        Order savedOrder = orderRepository.save(order);

        publishOrderEventPort.publish(
                new OrderStatusChangedEvent(
                        savedOrder.getId(),
                        previousStatus,
                        OrderStatus.DEVUELTO));

        return savedRequest;
    }

    @Override
    @Transactional
    public ReturnRequest reject(
            UUID adminId,
            UUID requestId,
            String resolutionNote) {
        adminGuard.requireAdmin(adminId);

        ReturnRequest request = findRequest(requestId);

        request.reject(adminId, resolutionNote, clock.instant());

        return returnRequestRepository.save(request);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReturnRequest> listPending(UUID adminId) {
        adminGuard.requireAdmin(adminId);

        return returnRequestRepository.findByStatus(
                ReturnRequestStatus.PENDIENTE);
    }

    private ReturnRequest findRequest(UUID requestId) {
        return returnRequestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Return request not found: " + requestId));
    }

    private Order findOrder(UUID orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Order not found: " + orderId));
    }
}