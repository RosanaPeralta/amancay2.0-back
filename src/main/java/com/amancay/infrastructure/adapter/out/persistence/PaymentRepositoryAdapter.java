package com.amancay.infrastructure.adapter.out.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.amancay.domain.exception.PaymentNotFoundException;
import com.amancay.domain.model.Payment;
import com.amancay.domain.model.PaymentStatus;
import com.amancay.domain.port.PaymentRepositoryPort;
import com.amancay.infrastructure.adapter.out.persistence.entity.PaymentJpaEntity;
import com.amancay.infrastructure.adapter.out.persistence.mapper.PaymentPersistenceMapper;
import com.amancay.infrastructure.adapter.out.persistence.repository.SpringDataPaymentRepository;

@Component
class PaymentRepositoryAdapter implements PaymentRepositoryPort {

    private final SpringDataPaymentRepository repository;
    private final PaymentPersistenceMapper mapper;

    PaymentRepositoryAdapter(SpringDataPaymentRepository repository, PaymentPersistenceMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public Optional<Payment> findById(UUID id) {
        return repository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<Payment> findByOrderId(UUID orderId) {
        return repository.findByOrderIdOrderByCreatedAtDesc(orderId).stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<Payment> findByStatus(PaymentStatus status) {
        return repository.findByStatusOrderByCreatedAtAsc(status).stream().map(mapper::toDomain).toList();
    }

    @Override
    public boolean existsByOrderIdAndStatus(UUID orderId, PaymentStatus status) {
        return repository.existsByOrderIdAndStatus(orderId, status);
    }

    // Igual que en ordenes: dentro de la transaccion el findById devuelve la entidad ya
    // cargada, y saveAndFlush para que la respuesta traiga id y timestamps.
    @Override
    public Payment save(Payment payment) {
        PaymentJpaEntity entity;
        if (payment.getId() == null) {
            entity = mapper.toNewEntity(payment);
        } else {
            entity = repository.findById(payment.getId())
                    .orElseThrow(() -> new PaymentNotFoundException(payment.getId()));
            mapper.applyChanges(payment, entity);
        }
        return mapper.toDomain(repository.saveAndFlush(entity));
    }
}
