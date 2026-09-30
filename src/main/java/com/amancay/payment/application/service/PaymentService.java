package com.amancay.payment.application.service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.amancay.exceptions.InsufficientStockException;
import com.amancay.payment.application.port.in.AttachTransferReferenceUseCase;
import com.amancay.payment.application.port.in.ConfirmPaymentUseCase;
import com.amancay.payment.application.port.in.CreatePaymentUseCase;
import com.amancay.payment.application.port.in.ListOrderPaymentsQuery;
import com.amancay.payment.application.port.in.ListPendingPaymentsQuery;
import com.amancay.payment.application.port.in.PaymentOutcome;
import com.amancay.payment.application.port.in.RetryPaymentUseCase;
import com.amancay.payment.application.port.out.BuyerEmailPort;
import com.amancay.payment.application.port.out.LoadPaymentPort;
import com.amancay.payment.application.port.out.PayableOrderPort;
import com.amancay.payment.application.port.out.PayableOrderPort.PayableOrder;
import com.amancay.payment.application.port.out.PaymentProcessorPort;
import com.amancay.payment.application.port.out.SavePaymentPort;
import com.amancay.payment.application.port.out.StockPort;
import com.amancay.payment.domain.exception.PaymentNotFoundException;
import com.amancay.payment.domain.model.Payment;
import com.amancay.payment.domain.model.PaymentDetails;
import com.amancay.payment.domain.model.PaymentResult;
import com.amancay.payment.domain.model.PaymentStatus;

@Service
public class PaymentService implements CreatePaymentUseCase, RetryPaymentUseCase, ConfirmPaymentUseCase,
        AttachTransferReferenceUseCase, ListOrderPaymentsQuery, ListPendingPaymentsQuery {

    private final LoadPaymentPort loadPaymentPort;
    private final SavePaymentPort savePaymentPort;
    private final PayableOrderPort payableOrderPort;
    private final PaymentProcessorPort paymentProcessorPort;
    private final StockPort stockPort;
    private final BuyerEmailPort buyerEmailPort;

    public PaymentService(LoadPaymentPort loadPaymentPort, SavePaymentPort savePaymentPort,
            PayableOrderPort payableOrderPort, PaymentProcessorPort paymentProcessorPort, StockPort stockPort,
            BuyerEmailPort buyerEmailPort) {
        this.loadPaymentPort = loadPaymentPort;
        this.savePaymentPort = savePaymentPort;
        this.payableOrderPort = payableOrderPort;
        this.paymentProcessorPort = paymentProcessorPort;
        this.stockPort = stockPort;
        this.buyerEmailPort = buyerEmailPort;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Payment> listByOrder(UUID requesterId, UUID orderId) {
        payableOrderPort.loadForRequester(requesterId, orderId);
        return loadPaymentPort.findByOrderId(orderId);
    }

    @Override
    @Transactional
    public PaymentOutcome create(UUID requesterId, UUID orderId, PaymentDetails details) {
        PayableOrder order = payableOrderPort.loadForRequester(requesterId, orderId);
        if (loadPaymentPort.existsByOrderIdAndStatus(orderId, PaymentStatus.APROBADO)) {
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
    // diferencia del resto de este servicio). Solo llega aca via AdminPaymentController,
    // que ya exige ADMIN.
    @Override
    @Transactional(readOnly = true)
    public List<PendingPayment> listPending() {
        List<Payment> pending = loadPaymentPort.findByStatus(PaymentStatus.PENDIENTE);
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
    public Payment confirm(UUID paymentId, PaymentStatus decision) {
        Payment.requireFinalDecision(decision);
        Payment payment = findPayment(paymentId);
        payment.confirm(decision);
        Payment saved = savePaymentPort.save(payment);

        if (saved.isApproved()) {
            fulfill(payableOrderPort.load(saved.getOrderId()));
        }
        return saved;
    }

    // El comprador la carga desde el detalle de la orden mientras el pago de
    // transferencia sigue PENDIENTE. No dispara nada mas: el stock recien se descuenta
    // cuando el admin aprueba con confirm.
    @Override
    @Transactional
    public Payment attachTransferReference(UUID requesterId, UUID paymentId, String transferReference) {
        Payment payment = findPayment(paymentId);
        payableOrderPort.loadForRequester(requesterId, payment.getOrderId());
        payment.attachTransferReference(transferReference);
        return savePaymentPort.save(payment);
    }

    private PaymentOutcome attemptPayment(PayableOrder order, PaymentDetails details) {
        PaymentResult result = paymentProcessorPort.process(order, details);
        Payment saved = savePaymentPort.save(Payment.attempt(order.id(), order.total(), details.method(), result));

        if (saved.isApproved()) {
            fulfill(order);
        }
        return new PaymentOutcome(saved, result.reason());
    }

    // Unico punto donde se toca stock de verdad: se llama recien cuando un pago (tarjeta
    // al toque, o transferencia via confirm) queda APROBADO, nunca al crear la orden. Si
    // algo se agoto mientras tanto (por ejemplo, una transferencia tardo dias en
    // confirmarse), esto tira InsufficientStockException y revierte toda la transaccion:
    // el pago no queda guardado como aprobado sin el stock que respalda esa aprobacion.
    private void fulfill(PayableOrder order) {
        for (PayableOrder.Line line : order.lines()) {
            if (!stockPort.decrement(line.productVariantId(), line.quantity())) {
                throw new InsufficientStockException(line.productVariantId());
            }
        }
        payableOrderPort.markAsPaid(order.id());
    }

    private Payment findPayment(UUID id) {
        return loadPaymentPort.findById(id).orElseThrow(() -> new PaymentNotFoundException(id));
    }
}
