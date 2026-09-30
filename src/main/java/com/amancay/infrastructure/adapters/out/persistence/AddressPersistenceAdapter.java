package com.amancay.infrastructure.adapters.out.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.amancay.domain.model.CustomerAddress;
import com.amancay.domain.ports.out.AddressCatalogPort;
import com.amancay.domain.ports.out.AddressUserLockPort;
import com.amancay.infrastructure.adapters.out.persistence.mapper.AddressPersistenceMapper;
import com.amancay.infrastructure.adapters.out.persistence.repository.AddressRepository;
import com.amancay.infrastructure.adapters.out.persistence.repository.UserRepository;
import com.amancay.exceptions.UserNotFoundException;

@Repository
public class AddressPersistenceAdapter implements AddressCatalogPort, AddressUserLockPort {
    private final AddressRepository addressRepository;
    private final UserRepository userRepository;

    public AddressPersistenceAdapter(AddressRepository addressRepository, UserRepository userRepository) {
        this.addressRepository = addressRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CustomerAddress> findByUserId(UUID userId) {
        return addressRepository.findByUserIdOrderByCreatedAtAsc(userId).stream()
                .map(AddressPersistenceMapper::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public long countByUserId(UUID userId) {
        return addressRepository.countByUserId(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<CustomerAddress> findByIdAndUserId(UUID addressId, UUID userId) {
        return addressRepository.findByIdAndUserId(addressId, userId).map(AddressPersistenceMapper::toDomain);
    }

    @Override
    @Transactional
    public CustomerAddress save(CustomerAddress address) {
        com.amancay.entity.Address existing = address.id() == null ? null
                : addressRepository.findByIdAndUserId(address.id(), address.userId()).orElse(null);
        return AddressPersistenceMapper.toDomain(addressRepository.saveAndFlush(
                AddressPersistenceMapper.toPersistence(address, existing)));
    }

    @Override
    @Transactional
    public void delete(CustomerAddress address) {
        addressRepository.deleteById(address.id());
    }

    @Override
    @Transactional
    public void clearDefaultByUserId(UUID userId) {
        addressRepository.clearDefaultByUserId(userId);
    }

    @Override
    @Transactional
    public void lockExistingUser(UUID userId) {
        userRepository.findByIdForUpdate(userId).orElseThrow(() -> new UserNotFoundException(userId));
    }
}