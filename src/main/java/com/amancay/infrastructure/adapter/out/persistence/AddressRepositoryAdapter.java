package com.amancay.infrastructure.adapter.out.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.amancay.application.port.out.AddressRepositoryPort;
import com.amancay.domain.model.Address;
import com.amancay.infrastructure.adapter.out.persistence.entity.AddressJpaEntity;
import com.amancay.infrastructure.adapter.out.persistence.mapper.AddressPersistenceMapper;
import com.amancay.infrastructure.adapter.out.persistence.repository.SpringDataAddressRepository;

@Component
class AddressRepositoryAdapter implements AddressRepositoryPort {

    private final SpringDataAddressRepository repository;
    private final AddressPersistenceMapper mapper;

    AddressRepositoryAdapter(SpringDataAddressRepository repository, AddressPersistenceMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public Optional<Address> findById(UUID id) {
        return repository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<Address> findByUserId(UUID userId) {
        return repository.findByUserIdOrderByCreatedAtAsc(userId).stream().map(mapper::toDomain).toList();
    }

    @Override
    public long countByUserId(UUID userId) {
        return repository.countByUserId(userId);
    }

    @Override
    public Optional<Address> findByIdAndUserId(UUID id, UUID userId) {
        return repository.findByIdAndUserId(id, userId).map(mapper::toDomain);
    }

    @Override
    public void clearDefaultByUserId(UUID userId) {
        repository.clearDefaultByUserId(userId);
    }

    @Override
    public Address save(Address address) {
        AddressJpaEntity entity = repository.findById(address.getId()).orElseGet(AddressJpaEntity::new);
        mapper.copyToEntity(address, entity);
        return mapper.toDomain(repository.saveAndFlush(entity));
    }

    @Override
    public void delete(Address address) {
        repository.deleteById(address.getId());
    }
}
