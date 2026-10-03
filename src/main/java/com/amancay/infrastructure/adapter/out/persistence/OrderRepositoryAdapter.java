package com.amancay.infrastructure.adapter.out.persistence;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.amancay.application.port.out.OrderRepositoryPort;
import com.amancay.domain.exception.OrderNotFoundException;
import com.amancay.domain.model.Order;
import com.amancay.infrastructure.adapter.out.persistence.entity.OrderJpaEntity;
import com.amancay.infrastructure.adapter.out.persistence.mapper.OrderPersistenceMapper;
import com.amancay.infrastructure.adapter.out.persistence.repository.SpringDataOrderRepository;

@Component
class OrderRepositoryAdapter implements OrderRepositoryPort {

    private final SpringDataOrderRepository repository;
    private final OrderPersistenceMapper mapper;

    OrderRepositoryAdapter(SpringDataOrderRepository repository, OrderPersistenceMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public Optional<Order> findById(UUID id) {
        return repository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<Order> findByUserId(UUID userId) {
        return repository.findByUserId(userId).stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<Order> findAllById(Collection<UUID> ids) {
        return repository.findAllById(ids).stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<Order> findAllNewestFirst() {
        return repository.findAllByOrderByCreatedAtDesc().stream().map(mapper::toDomain).toList();
    }

    // saveAndFlush para que la respuesta ya traiga ids y timestamps.
    @Override
    public Order save(Order order) {
        OrderJpaEntity entity;
        if (order.getId() == null) {
            entity = mapper.toNewEntity(order);
        } else {
            entity = repository.findById(order.getId())
                    .orElseThrow(() -> new OrderNotFoundException(order.getId()));
            mapper.applyChanges(order, entity);
        }
        return mapper.toDomain(repository.saveAndFlush(entity));
    }
}
