package com.amancay.application.service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.amancay.application.port.in.AttachTransferReferenceUseCase;
import com.amancay.application.port.in.ConfirmPaymentUseCase;
import com.amancay.application.port.in.CreatePaymentUseCase;
import com.amancay.application.port.in.ListOrderPaymentsQuery;
import com.amancay.application.port.in.ListPendingPaymentsQuery;
import com.amancay.application.port.in.PaymentOutcome;
import com.amancay.application.port.in.RetryPaymentUseCase;
import com.amancay.application.port.out.BuyerEmailPort;
import com.amancay.application.port.out.PayableOrderPort.PayableOrder;
import com.amancay.application.port.out.PayableOrderPort;
import com.amancay.application.port.out.PaymentProcessorPort;
import com.amancay.application.port.out.PaymentRepositoryPort;
import com.amancay.application.port.out.PublishPaymentEventPort;
import com.amancay.application.port.out.StockPort;
import com.amancay.domain.event.PaymentStatusChangedEvent;
import com.amancay.domain.exception.InsufficientStockException;
import com.amancay.domain.exception.PaymentNotFoundException;
import com.amancay.domain.model.Payment;
import com.amancay.domain.model.PaymentDetails;
import com.amancay.domain.model.PaymentResult;
import com.amancay.domain.model.PaymentStatus;

@Service
public class PaymentService implements CreatePaymentUseCase, RetryPaymentUseCase, ConfirmPaymentUseCase,
        AttachTransferReferenceUseCase, ListOrderPaymentsQuery, ListPendingPaymentsQuery {

    private final PaymentRepositoryPort paymentRepository;
    private final PayableOrderPort payableOrderPort;
    private final PaymentProcessorPort paymentProcessorPort;
    private final StockPort stockPort;
    private final BuyerEmailPort buyerEmailPort;
    private final PublishPaymentEventPort publishPaymentEventPort;
    private final AdminGuard adminGuard;

    public PaymentService(PaymentRepositoryPort paymentRepository,
            PayableOrderPort payableOrderPort, PaymentProcessorPort paymentProcessorPort, StockPort stockPort,
            BuyerEmailPort buyerEmailPort, PublishPaymentEventPort publishPaymentEventPort, AdminGuard adminGuard) {
        this.paymentRepository = paymentRepository;
        this.payableOrderPort = payableOrderPort;
        this.paymentProcessorPort = paymentProcessorPort;
        this.stockPort = stockPort;
        this.buyerEmailPort = buyerEmailPort;
        this.publishPaymentEventPort = publishPaymentEventPort;
        this.adminGuard = adminGuard;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Payment> listByOrder(UUID requesterId, UUID orderId) {
        payableOrderPort.loadForRequester(requesterId, orderId);
        return paymentRepository.findByOrderId(orderId);
    }

    @Override
    @Transactional
    public PaymentOutcome create(UUID requesterId, UUID orderId, PaymentDetails details) {
        PayableOrder order = payableOrderPort.loadForRequester(requesterId, orderId);
        if (paymentRepository.existsByOrderIdAndStatus(orderId, PaymentStatus.APROBADO)) {
            throw new IllegalStateException("Order already has an approved payment");
        }
        return attemptPayment(order, details);
    }

    @Override
    @Transactional
    public PaymentOutcome retry(UUID requesterId, UUID paymentId, PaymentDetails details) {
        Payment failedPayment = findPayment(paymentId);
        PayableOrder order = payableOrderPort.loadForRequester(requesterId, failedPayment.getOrderId());
        failedPayment.ensureRetryable();
        return attemptPayment(order, details);
    }

    // Unico lugar que muestra pagos de todos los usuarios (nunca solo los propios, a
    // diferencia del resto de este servicio), por eso exige ADMIN.
    @Override
    @Transactional(readOnly = true)
    public List<PendingPayment> listPending(UUID requesterId) {
        adminGuard.requireAdmin(requesterId);
        List<Payment> pending = paymentRepository.findByStatus(PaymentStatus.PENDIENTE);
        Set<UUID> orderIds = pending.stream().map(Payment::getOrderId).collect(Collectors.toSet());
        Map<UUID, PayableOrder> ordersById = payableOrderPort.loadAll(orderIds).stream()
                .collect(Collectors.toMap(PayableOrder::id, Function.identity()));
        Set<UUID> buyerIds = ordersById.values().stream().map(PayableOrder::buyerId).collect(Collectors.toSet());
        Map<UUID, String> emailsByUserId = buyerEmailPort.findEmailsByUserId(buyerIds);
        return pending.stream()
                .map(payment -> new PendingPayment(payment,
                        emailsByUserId.get(ordersById.get(payment.getOrderId()).buyerId())))
                .toList();
    }

    @Override
    @Transactional
    public Payment confirm(UUID requesterId, UUID paymentId, PaymentStatus decision) {
        adminGuard.requireAdmin(requesterId);
        Payment.requireFinalDecision(decision);
        Payment payment = findPayment(paymentId);
        PaymentStatus previousStatus = payment.getStatus();
        payment.confirm(decision);
        Payment saved = paymentRepository.save(payment);
        publishPaymentEventPort.publish(new PaymentStatusChangedEvent(saved.getId(), previousStatus, saved.getStatus()));

        if (saved.isApproved()) {
            fulfill(payableOrderPort.load(saved.getOrderId()));
        }
        return saved;
    }

    @Override
    @Transactional
    public Payment attachTransferReference(UUID requesterId, UUID paymentId, String transferReference) {
        Payment payment = findPayment(paymentId);
        payableOrderPort.loadForRequester(requesterId, payment.getOrderId());
        payment.attachTransferReference(transferReference);
        return paymentRepository.save(payment);
    }

    private PaymentOutcome attemptPayment(PayableOrder order, PaymentDetails details) {
        PaymentResult result = paymentProcessorPort.process(order, details);
        Payment saved = paymentRepository.save(Payment.attempt(order.id(), order.total(), details.method(), result));
        publishPaymentEventPort
                .publish(new PaymentStatusChangedEvent(saved.getId(), PaymentStatus.PENDIENTE, saved.getStatus()));

        if (saved.isApproved()) {
            fulfill(order);
        }
        return new PaymentOutcome(saved, result.reason());
    }

    // Unico lugar que descuenta stock, solo con el pago APROBADO. Si no alcanza, revierte la transaccion.
    private void fulfill(PayableOrder order) {
        for (PayableOrder.Line line : order.lines()) {
            if (!stockPort.decrement(line.productVariantId(), line.quantity())) {
                throw new InsufficientStockException(line.productVariantId());
            }
        }
        payableOrderPort.markAsPaid(order.id());
    }

    private Payment findPayment(UUID id) {
        return paymentRepository.findById(id).orElseThrow(() -> new PaymentNotFoundException(id));
    }
}
