
package com.amancay.infrastructure.adapter.out.persistence.mapper;

import org.springframework.stereotype.Component;

import com.amancay.domain.model.ReturnRequest;
import com.amancay.infrastructure.adapter.out.persistence.entity.OrderJpaEntity;
import com.amancay.infrastructure.adapter.out.persistence.entity.ReturnRequestJpaEntity;
import com.amancay.infrastructure.adapter.out.persistence.repository.SpringDataOrderRepository;

@Component
public class ReturnRequestPersistenceMapper {

    private final SpringDataOrderRepository orderRepository;

    public ReturnRequestPersistenceMapper(SpringDataOrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    public ReturnRequest toDomain(ReturnRequestJpaEntity entity) {
        return new ReturnRequest(
                entity.getId(),
                entity.getOrder().getId(),
                entity.getReason(),
                entity.getComment(),
                entity.getStatus(),
                entity.getRequestedAt(),
                entity.getResolvedAt(),
                entity.getResolvedBy(),
                entity.getResolutionNote());
    }

    public ReturnRequestJpaEntity toNewEntity(ReturnRequest request) {
        ReturnRequestJpaEntity entity = new ReturnRequestJpaEntity();

        OrderJpaEntity order = orderRepository.findById(request.getOrderId())
                .orElseThrow(() -> new IllegalStateException(
                        "Order not found: " + request.getOrderId()));

        entity.setOrder(order);
        entity.setReason(request.getReason());
        entity.setComment(request.getComment());
        entity.setStatus(request.getStatus());
        entity.setRequestedAt(request.getRequestedAt());
        entity.setResolvedAt(request.getResolvedAt());
        entity.setResolvedBy(request.getResolvedBy());
        entity.setResolutionNote(request.getResolutionNote());

        return entity;
    }

    public void applyChanges(ReturnRequest request, ReturnRequestJpaEntity entity) {
        entity.setStatus(request.getStatus());
        entity.setResolvedAt(request.getResolvedAt());
        entity.setResolvedBy(request.getResolvedBy());
        entity.setResolutionNote(request.getResolutionNote());
    }
}