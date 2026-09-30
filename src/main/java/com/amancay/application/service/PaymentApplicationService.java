package com.amancay.application.service;

import java.util.List;
import java.util.UUID;

import org.springframework.transaction.annotation.Transactional;

import com.amancay.domain.model.OrderLine;
import com.amancay.domain.model.OrderState;
import com.amancay.domain.model.PaymentMethodType;
import com.amancay.domain.model.PaymentOutcome;
import com.amancay.domain.model.PaymentRecord;
import com.amancay.domain.model.PaymentState;
import com.amancay.domain.model.PendingPayment;
import com.amancay.domain.model.PurchaseOrder;
import com.amancay.domain.ports.in.PaymentUseCases;
import com.amancay.domain.ports.out.PaymentCatalogPort;
import com.amancay.domain.ports.out.PaymentOrderPort;
import com.amancay.domain.ports.out.PaymentProcessorPort;
import com.amancay.domain.ports.out.PaymentStockPort;
import com.amancay.exceptions.InsufficientStockException;
import com.amancay.exceptions.OrderNotFoundException;
import com.amancay.exceptions.PaymentNotFoundException;

public class PaymentApplicationService implements PaymentUseCases {
    private final PaymentCatalogPort payments;
    private final PaymentOrderPort orders;
    private final PaymentProcessorPort processors;
    private final PaymentStockPort stock;

    public PaymentApplicationService(PaymentCatalogPort payments, PaymentOrderPort orders,
            PaymentProcessorPort processors, PaymentStockPort stock) {
        this.payments = payments;
        this.orders = orders;
        this.processors = processors;
        this.stock = stock;
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaymentRecord> listByOrder(UUID requesterId, UUID orderId) {
        orders.authorize(requesterId, orderId);
        return payments.findByOrderId(orderId);
    }

    @Override
    @Transactional
    public PaymentRecord create(UUID requesterId, UUID orderId, PaymentRequest request) {
        orders.authorize(requesterId, orderId);
        PurchaseOrder order = orders.getOrder(orderId);
        if (payments.hasApprovedPayment(orderId)) {
            throw new IllegalStateException("Order already has an approved payment");
        }
        return attemptPayment(order, request);
    }

    @Override
    @Transactional
    public PaymentRecord retry(UUID requesterId, UUID paymentId, PaymentRequest request) {
        PaymentRecord failed = findPayment(paymentId);
        orders.authorize(requesterId, failed.orderId());
        if (failed.state() != PaymentState.RECHAZADO) {
            throw new IllegalStateException("Only a rejected payment can be retried");
        }
        PurchaseOrder order = orders.getOrder(failed.orderId());
        return attemptPayment(order, request);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PendingPayment> listPendingForAdmin() {
        return payments.findPendingForAdmin();
    }

    @Override
    @Transactional
    public PaymentRecord confirm(UUID paymentId, PaymentState state) {
        if (state != PaymentState.APROBADO && state != PaymentState.RECHAZADO) {
            throw new IllegalArgumentException("status must be APROBADO or RECHAZADO");
        }
        PaymentRecord payment = findPayment(paymentId);
        if (payment.state() != PaymentState.PENDIENTE) {
            throw new IllegalStateException("Only a pending payment can be confirmed");
        }
        PaymentRecord saved = payments.save(withState(payment, state));
        if (state == PaymentState.APROBADO) {
            PurchaseOrder order = orders.getOrder(payment.orderId());
            decrementStockFor(order.items());
            orders.changeStatus(order.id(), OrderState.EN_PREPARACION);
        }
        return saved;
    }

    @Override
    @Transactional
    public PaymentRecord attachTransferReference(UUID requesterId, UUID paymentId, String transferReference) {
        PaymentRecord payment = findPayment(paymentId);
        orders.authorize(requesterId, payment.orderId());
        if (payment.method() != PaymentMethodType.TRANSFERENCIA) {
            throw new IllegalArgumentException("Only bank transfer payments accept a reference code");
        }
        if (payment.state() != PaymentState.PENDIENTE) {
            throw new IllegalStateException("Only a pending payment can receive a reference code");
        }
        PaymentRecord updated = new PaymentRecord(payment.id(), payment.orderId(), payment.buyerId(), payment.amount(),
                payment.method(), payment.state(), transferReference, payment.createdAt(), payment.reason());
        return payments.save(updated);
    }

    private PaymentRecord attemptPayment(PurchaseOrder order, PaymentRequest request) {
        PaymentOutcome outcome = processors.process(request.method(), order, request);
        PaymentRecord payment = new PaymentRecord(null, order.id(), order.userId(), order.total(), request.method(),
                outcome.state(), null, null, outcome.reason());
        PaymentRecord saved = payments.save(payment);
        if (outcome.state() == PaymentState.APROBADO) {
            decrementStockFor(order.items());
            orders.changeStatus(order.id(), OrderState.EN_PREPARACION);
        }
        return new PaymentRecord(saved.id(), saved.orderId(), saved.buyerId(), saved.amount(), saved.method(),
            saved.state(), saved.transferReference(), saved.createdAt(), outcome.reason());
    }

    private void decrementStockFor(List<OrderLine> items) {
        for (OrderLine item : items) {
            if (!stock.decrement(item.productVariantId(), item.quantity())) {
                throw new InsufficientStockException(item.productVariantId());
            }
        }
    }

    private PaymentRecord findPayment(UUID id) {
        return payments.findById(id).orElseThrow(() -> new PaymentNotFoundException(id));
    }

    private PaymentRecord withState(PaymentRecord payment, PaymentState state) {
        return new PaymentRecord(payment.id(), payment.orderId(), payment.buyerId(), payment.amount(), payment.method(),
                state, payment.transferReference(), payment.createdAt(), payment.reason());
    }
}