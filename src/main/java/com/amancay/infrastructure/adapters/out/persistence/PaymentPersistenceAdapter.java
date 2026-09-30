package com.amancay.infrastructure.adapters.out.persistence;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.amancay.domain.model.PaymentRecord;
import com.amancay.domain.model.PaymentState;
import com.amancay.domain.model.PendingPayment;
import com.amancay.domain.ports.out.PaymentCatalogPort;
import com.amancay.infrastructure.adapters.out.persistence.mapper.PaymentPersistenceMapper;
import com.amancay.infrastructure.adapters.out.persistence.repository.OrderRepository;
import com.amancay.infrastructure.adapters.out.persistence.repository.PaymentRepository;
import com.amancay.infrastructure.adapters.out.persistence.repository.UserRepository;

@Repository
public class PaymentPersistenceAdapter implements PaymentCatalogPort {
    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;

    public PaymentPersistenceAdapter(PaymentRepository paymentRepository, OrderRepository orderRepository,
            UserRepository userRepository) {
        this.paymentRepository = paymentRepository;
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaymentRecord> findByOrderId(UUID orderId) {
        return paymentRepository.findByOrderIdOrderByCreatedAtDesc(orderId).stream()
                .map(PaymentPersistenceMapper::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<PaymentRecord> findById(UUID paymentId) {
        return paymentRepository.findById(paymentId).map(PaymentPersistenceMapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasApprovedPayment(UUID orderId) {
        return orderRepository.findById(orderId).map(order -> order.getPayments().stream()
                .anyMatch(payment -> payment.getStatus() == com.amancay.entity.PaymentStatus.APROBADO)).orElse(false);
    }

    @Override
    @Transactional
    public PaymentRecord save(PaymentRecord payment) {
        com.amancay.entity.Payment entity = payment.id() == null ? null
                : paymentRepository.findById(payment.id()).orElse(null);
        com.amancay.entity.Order order = entity == null ? orderRepository.findById(payment.orderId()).orElseThrow()
                : entity.getOrder();
        return PaymentPersistenceMapper.toDomain(paymentRepository.saveAndFlush(
                PaymentPersistenceMapper.toPersistence(payment, entity, order)));
    }

    @Override
    @Transactional(readOnly = true)
    public List<PendingPayment> findPendingForAdmin() {
        List<com.amancay.entity.Payment> pending = paymentRepository
                .findByStatusOrderByCreatedAtAsc(com.amancay.entity.PaymentStatus.PENDIENTE);
        Set<UUID> buyerIds = pending.stream().map(payment -> payment.getOrder().getUserId()).collect(Collectors.toSet());
        Map<UUID, String> emails = userRepository.findAllById(buyerIds).stream()
                .collect(Collectors.toMap(com.amancay.entity.User::getId, com.amancay.entity.User::getEmail));
        return pending.stream().map(payment -> new PendingPayment(payment.getId(), payment.getOrder().getId(),
                payment.getAmount(), com.amancay.domain.model.PaymentMethodType.valueOf(payment.getMethod().name()),
                payment.getTransferReference(), payment.getCreatedAt(), emails.get(payment.getOrder().getUserId())))
                .toList();
    }
}