package com.amancay.service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.amancay.dto.AdminPendingPaymentDto;
import com.amancay.dto.ConfirmPaymentRequest;
import com.amancay.dto.CreatePaymentRequest;
import com.amancay.dto.PaymentDto;
import com.amancay.entity.Order;
import com.amancay.entity.OrderItem;
import com.amancay.entity.OrderStatus;
import com.amancay.entity.Payment;
import com.amancay.entity.PaymentMethod;
import com.amancay.entity.PaymentStatus;
import com.amancay.entity.User;
import com.amancay.exceptions.InsufficientStockException;
import com.amancay.exceptions.OrderNotFoundException;
import com.amancay.exceptions.PaymentNotFoundException;
import com.amancay.repository.OrderRepository;
import com.amancay.repository.PaymentRepository;
import com.amancay.repository.ProductVariantRepository;
import com.amancay.repository.UserRepository;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final OrderService orderService;
    private final PaymentProcessorResolver processorResolver;
    private final ProductVariantRepository productVariantRepository;
    private final UserRepository userRepository;

    public PaymentService(PaymentRepository paymentRepository, OrderRepository orderRepository,
            OrderService orderService, PaymentProcessorResolver processorResolver,
            ProductVariantRepository productVariantRepository, UserRepository userRepository) {
        this.paymentRepository = paymentRepository;
        this.orderRepository = orderRepository;
        this.orderService = orderService;
        this.processorResolver = processorResolver;
        this.productVariantRepository = productVariantRepository;
        this.userRepository = userRepository;
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

    // El dueno de la tienda viendo que transferencias tiene que chequear contra su
    // cuenta bancaria: pagos PENDIENTE de TODOS los usuarios (nunca solo los propios,
    // a diferencia del resto de este servicio). Solo llega aca via AdminPaymentController,
    // que ya exige ADMIN.
    @Transactional(readOnly = true)
    public List<AdminPendingPaymentDto> listPendingForAdmin() {
        List<Payment> pending = paymentRepository.findByStatusOrderByCreatedAtAsc(PaymentStatus.PENDIENTE);
        Set<UUID> buyerIds = pending.stream().map(payment -> payment.getOrder().getUserId()).collect(Collectors.toSet());
        Map<UUID, String> emailsByUserId = userRepository.findAllById(buyerIds).stream()
                .collect(Collectors.toMap(User::getId, User::getEmail));
        return pending.stream()
                .map(payment -> new AdminPendingPaymentDto(payment.getId(), payment.getOrder().getId(),
                        payment.getAmount(), payment.getMethod(), payment.getTransferReference(),
                        payment.getCreatedAt(), emailsByUserId.get(payment.getOrder().getUserId())))
                .toList();
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
            decrementStockFor(payment.getOrder());
            orderService.changeStatus(payment.getOrder().getId(), OrderStatus.EN_PREPARACION);
        }

        return toDto(saved, null);
    }

    // El comprador la carga desde el detalle de la orden mientras el pago de
    // transferencia sigue PENDIENTE; se puede reemplazar si se vuelve a enviar antes
    // de que el admin confirme. No dispara nada mas: el stock recien se descuenta
    // cuando el admin aprueba con confirmPayment.
    @Transactional
    public PaymentDto attachTransferReference(UUID requesterId, UUID paymentId, String transferReference) {
        Payment payment = findPayment(paymentId);
        orderService.getOrder(requesterId, payment.getOrder().getId(), null);
        if (payment.getMethod() != PaymentMethod.TRANSFERENCIA) {
            throw new IllegalArgumentException("Only bank transfer payments accept a reference code");
        }
        if (payment.getStatus() != PaymentStatus.PENDIENTE) {
            throw new IllegalStateException("Only a pending payment can receive a reference code");
        }
        payment.setTransferReference(transferReference);
        Payment saved = paymentRepository.saveAndFlush(payment);
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
            decrementStockFor(order);
            orderService.changeStatus(order.getId(), OrderStatus.EN_PREPARACION);
        }

        return toDto(saved, result.reason());
    }

    // Unico punto donde se toca stock de verdad: se llama recien cuando un pago (tarjeta
    // al toque, o transferencia via confirmPayment) queda APROBADO, nunca al crear la
    // orden. Si algo se agoto mientras tanto (por ejemplo, una transferencia tardo dias
    // en confirmarse), esto tira InsufficientStockException y revierte toda la transaccion:
    // el pago no queda guardado como aprobado sin el stock que respalda esa aprobacion.
    private void decrementStockFor(Order order) {
        for (OrderItem item : order.getItems()) {
            UUID variantId = item.getProductVariant().getId();
            if (productVariantRepository.decrementStock(variantId, item.getQuantity()) == 0) {
                throw new InsufficientStockException(variantId);
            }
        }
    }

    private Order findOrder(UUID id) {
        return orderRepository.findById(id).orElseThrow(() -> new OrderNotFoundException(id));
    }

    private Payment findPayment(UUID id) {
        return paymentRepository.findById(id).orElseThrow(() -> new PaymentNotFoundException(id));
    }

    private PaymentDto toDto(Payment payment, String reason) {
        return new PaymentDto(payment.getId(), payment.getOrder().getId(), payment.getAmount(), payment.getMethod(),
                payment.getStatus(), reason, payment.getTransferReference(), payment.getCreatedAt());
    }
}
