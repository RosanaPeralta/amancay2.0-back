package com.amancay.service;

import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.amancay.application.service.PaymentApplicationService;
import com.amancay.domain.model.PaymentRecord;
import com.amancay.domain.model.PendingPayment;
import com.amancay.domain.ports.in.PaymentUseCases;
import com.amancay.domain.ports.in.PaymentUseCases.CardData;
import com.amancay.domain.ports.in.PaymentUseCases.PaymentRequest;
import com.amancay.domain.ports.out.PaymentCatalogPort;
import com.amancay.domain.ports.out.PaymentOrderPort;
import com.amancay.domain.ports.out.PaymentProcessorPort;
import com.amancay.domain.ports.out.PaymentStockPort;
import com.amancay.dto.AdminPendingPaymentDto;
import com.amancay.dto.ConfirmPaymentRequest;
import com.amancay.dto.CreatePaymentRequest;
import com.amancay.dto.PaymentDto;
import com.amancay.entity.PaymentMethod;
import com.amancay.entity.PaymentStatus;
import com.amancay.infrastructure.adapters.out.payments.LegacyPaymentProcessorAdapter;
import com.amancay.infrastructure.adapters.out.payments.ProductVariantStockAdapter;
import com.amancay.infrastructure.adapters.out.persistence.PaymentOrderAdapter;
import com.amancay.infrastructure.adapters.out.persistence.PaymentPersistenceAdapter;
import com.amancay.repository.OrderRepository;
import com.amancay.repository.PaymentRepository;
import com.amancay.repository.ProductVariantRepository;
import com.amancay.repository.UserRepository;

@Service
public class PaymentService {
    private final PaymentUseCases paymentUseCases;

    @Autowired
    public PaymentService(PaymentUseCases paymentUseCases) {
        this.paymentUseCases = paymentUseCases;
    }

    public PaymentService(PaymentRepository paymentRepository, OrderRepository orderRepository,
            OrderService orderService, PaymentProcessorResolver processorResolver,
            ProductVariantRepository productVariantRepository, UserRepository userRepository) {
        PaymentCatalogPort catalog = new PaymentPersistenceAdapter(paymentRepository, orderRepository, userRepository);
        PaymentOrderPort orders = new PaymentOrderAdapter(orderService, orderRepository);
        PaymentProcessorPort processors = new LegacyPaymentProcessorAdapter(processorResolver, orderRepository);
        PaymentStockPort stock = new ProductVariantStockAdapter(productVariantRepository);
        this.paymentUseCases = new PaymentApplicationService(catalog, orders, processors, stock);
    }

    @Transactional(readOnly = true)
    public List<PaymentDto> listByOrder(UUID requesterId, UUID orderId) {
        return paymentUseCases.listByOrder(requesterId, orderId).stream().map(payment -> toDto(payment, null)).toList();
    }

    @Transactional
    public PaymentDto createPayment(UUID requesterId, UUID orderId, CreatePaymentRequest request) {
        return toDto(paymentUseCases.create(requesterId, orderId, toRequest(request)), null);
    }

    @Transactional
    public PaymentDto retryPayment(UUID requesterId, UUID paymentId, CreatePaymentRequest request) {
        return toDto(paymentUseCases.retry(requesterId, paymentId, toRequest(request)), null);
    }

    @Transactional(readOnly = true)
    public List<AdminPendingPaymentDto> listPendingForAdmin() {
        return paymentUseCases.listPendingForAdmin().stream().map(this::toAdminDto).toList();
    }

    @Transactional
    public PaymentDto confirmPayment(UUID paymentId, ConfirmPaymentRequest request) {
        return toDto(paymentUseCases.confirm(paymentId,
                com.amancay.domain.model.PaymentState.valueOf(request.status().name())), null);
    }

    @Transactional
    public PaymentDto attachTransferReference(UUID requesterId, UUID paymentId, String transferReference) {
        return toDto(paymentUseCases.attachTransferReference(requesterId, paymentId, transferReference), null);
    }

    private PaymentRequest toRequest(CreatePaymentRequest request) {
        CardData card = request.card() == null ? null
                : new CardData(request.card().number(), request.card().holderName(), request.card().expiry(),
                        request.card().cvv());
        return new PaymentRequest(com.amancay.domain.model.PaymentMethodType.valueOf(request.method().name()), card);
    }

    private PaymentDto toDto(PaymentRecord payment, String reason) {
        return new PaymentDto(payment.id(), payment.orderId(), payment.amount(), PaymentMethod.valueOf(payment.method().name()),
                PaymentStatus.valueOf(payment.state().name()), reason == null ? payment.reason() : reason,
                payment.transferReference(), payment.createdAt());
    }

    private AdminPendingPaymentDto toAdminDto(PendingPayment payment) {
        return new AdminPendingPaymentDto(payment.paymentId(), payment.orderId(), payment.amount(),
                PaymentMethod.valueOf(payment.method().name()), payment.transferReference(), payment.createdAt(),
                payment.buyerEmail());
    }
}