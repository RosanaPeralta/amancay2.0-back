package com.amancay.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.amancay.dto.ConfirmPaymentRequest;
import com.amancay.dto.CreatePaymentRequest;
import com.amancay.dto.PaymentDto;
import com.amancay.entity.Order;
import com.amancay.entity.OrderStatus;
import com.amancay.entity.Payment;
import com.amancay.entity.PaymentStatus;
import com.amancay.exceptions.OrderNotFoundException;
import com.amancay.exceptions.PaymentNotFoundException;
import com.amancay.repository.OrderRepository;
import com.amancay.repository.PaymentRepository;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final OrderService orderService;
    private final PaymentProcessorResolver processorResolver;

    public PaymentService(PaymentRepository paymentRepository, OrderRepository orderRepository,
            OrderService orderService, PaymentProcessorResolver processorResolver) {
        this.paymentRepository = paymentRepository;
        this.orderRepository = orderRepository;
        this.orderService = orderService;
        this.processorResolver = processorResolver;
    }

    @Transactional(readOnly = true)
    public List<PaymentDto> listByOrder(UUID requesterId, UUID orderId) {
        orderService.getOrder(requesterId, orderId, null);
        return paymentRepository.findByOrderIdOrderByCreatedAtDesc(orderId).stream()
                .map(payment -> toDto(payment, null))
                .toList();
    }

    @Transactional
    public PaymentDto createPayment(UUID requesterId, UUID orderId, CreatePaymentRequest request) {
        orderService.getOrder(requesterId, orderId, null);
        Order order = findOrder(orderId);

        boolean alreadyApproved = order.getPayments().stream()
                .anyMatch(payment -> payment.getStatus() == PaymentStatus.APROBADO);
        if (alreadyApproved) {
            throw new IllegalStateException("Order already has an approved payment");
        }

        return attemptPayment(order, request);
    }

    @Transactional
    public PaymentDto retryPayment(UUID requesterId, UUID paymentId, CreatePaymentRequest request) {
        Payment failedPayment = findPayment(paymentId);
        orderService.getOrder(requesterId, failedPayment.getOrder().getId(), null);
        if (failedPayment.getStatus() != PaymentStatus.RECHAZADO) {
            throw new IllegalStateException("Only a rejected payment can be retried");
        }
        return attemptPayment(failedPayment.getOrder(), request);
    }

    @Transactional
    public PaymentDto confirmPayment(UUID paymentId, ConfirmPaymentRequest request) {
        if (request.status() != PaymentStatus.APROBADO && request.status() != PaymentStatus.RECHAZADO) {
            throw new IllegalArgumentException("status must be APROBADO or RECHAZADO");
        }

        Payment payment = findPayment(paymentId);
        if (payment.getStatus() != PaymentStatus.PENDIENTE) {
            throw new IllegalStateException("Only a pending payment can be confirmed");
        }

        payment.setStatus(request.status());
        Payment saved = paymentRepository.saveAndFlush(payment);

        if (request.status() == PaymentStatus.APROBADO) {
            orderService.changeStatus(payment.getOrder().getId(), OrderStatus.EN_PREPARACION);
        }

        return toDto(saved, null);
    }

    private PaymentDto attemptPayment(Order order, CreatePaymentRequest request) {
        Payment payment = new Payment();
        payment.setOrder(order);
        payment.setAmount(order.getTotal());
        payment.setMethod(request.method());
        payment.setStatus(PaymentStatus.PENDIENTE);

        PaymentResult result = processorResolver.resolve(request.method()).process(order, request);
        payment.setStatus(result.status());

        Payment saved = paymentRepository.saveAndFlush(payment);

        if (result.status() == PaymentStatus.APROBADO) {
            orderService.changeStatus(order.getId(), OrderStatus.EN_PREPARACION);
        }

        return toDto(saved, result.reason());
    }

    private Order findOrder(UUID id) {
        return orderRepository.findById(id).orElseThrow(() -> new OrderNotFoundException(id));
    }

    private Payment findPayment(UUID id) {
        return paymentRepository.findById(id).orElseThrow(() -> new PaymentNotFoundException(id));
    }

    private PaymentDto toDto(Payment payment, String reason) {
        return new PaymentDto(payment.getId(), payment.getOrder().getId(), payment.getAmount(), payment.getMethod(),
                payment.getStatus(), reason, payment.getCreatedAt());
    }
}
