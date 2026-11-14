
package com.amancay.infrastructure.adapter.out.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.amancay.application.port.out.ReturnRequestRepositoryPort;
import com.amancay.domain.model.ReturnRequest;
import com.amancay.domain.model.ReturnRequestStatus;
import com.amancay.infrastructure.adapter.out.persistence.entity.ReturnRequestJpaEntity;
import com.amancay.infrastructure.adapter.out.persistence.mapper.ReturnRequestPersistenceMapper;
import com.amancay.infrastructure.adapter.out.persistence.repository.SpringDataReturnRequestRepository;

@Component
class ReturnRequestRepositoryAdapter implements ReturnRequestRepositoryPort {

    private final SpringDataReturnRequestRepository repository;
    private final ReturnRequestPersistenceMapper mapper;

    ReturnRequestRepositoryAdapter(
            SpringDataReturnRequestRepository repository,
            ReturnRequestPersistenceMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public Optional<ReturnRequest> findById(UUID id) {
        return repository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<ReturnRequest> findByOrderId(UUID orderId) {
        return repository.findByOrderId(orderId).map(mapper::toDomain);
    }

    @Override
    public List<ReturnRequest> findByStatus(ReturnRequestStatus status) {
        return repository.findByStatus(status).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public ReturnRequest save(ReturnRequest request) {
        ReturnRequestJpaEntity entity = request.getId() == null
                ? mapper.toNewEntity(request)
                : repository.findById(request.getId())
                        .orElseThrow(() -> new IllegalStateException(
                                "Return request not found during save: " + request.getId()));

        if (request.getId() != null) {
            mapper.applyChanges(request, entity);
        }

        return mapper.toDomain(repository.saveAndFlush(entity));
    }
}