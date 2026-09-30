package com.amancay.order.adapter.out.persistence;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.amancay.order.application.port.out.LoadOrderPort;
import com.amancay.order.application.port.out.SaveOrderPort;
import com.amancay.order.domain.exception.OrderNotFoundException;
import com.amancay.order.domain.model.Order;

@Component
class OrderPersistenceAdapter implements LoadOrderPort, SaveOrderPort {

    private final SpringDataOrderRepository repository;
    private final OrderPersistenceMapper mapper;

    OrderPersistenceAdapter(SpringDataOrderRepository repository, OrderPersistenceMapper mapper) {
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

    // Dentro de la misma transaccion, el findById devuelve la entidad que ya esta en
    // el contexto de persistencia (la cargo el servicio antes), sin ir de nuevo a la base.
    // saveAndFlush para que la respuesta ya traiga ids y timestamps asignados.
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
