package com.amancay.application.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.amancay.application.port.in.AddressData;
import com.amancay.application.port.in.CreateAddressUseCase;
import com.amancay.application.port.in.DeleteAddressUseCase;
import com.amancay.application.port.in.ListAddressesQuery;
import com.amancay.application.port.in.SetDefaultAddressUseCase;
import com.amancay.application.port.in.UpdateAddressUseCase;
import com.amancay.domain.exception.AddressLimitReachedException;
import com.amancay.domain.exception.AddressNotFoundException;
import com.amancay.domain.exception.UserNotFoundException;
import com.amancay.domain.model.Address;
import com.amancay.domain.port.AddressRepositoryPort;
import com.amancay.domain.port.UserRepositoryPort;

@Service
public class AddressService implements ListAddressesQuery, CreateAddressUseCase, UpdateAddressUseCase,
        DeleteAddressUseCase, SetDefaultAddressUseCase {

    public static final int MAX_ADDRESSES_PER_USER = 10;

    private final AddressRepositoryPort addressRepository;
    private final UserRepositoryPort userRepository;

    public AddressService(AddressRepositoryPort addressRepository, UserRepositoryPort userRepository) {
        this.addressRepository = addressRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Address> list(UUID userId) {
        return addressRepository.findByUserId(userId);
    }

    @Override
    @Transactional
    public Address create(UUID userId, AddressData data) {
        lockUser(userId);
        long existing = addressRepository.countByUserId(userId);
        if (existing >= MAX_ADDRESSES_PER_USER) {
            throw new AddressLimitReachedException(MAX_ADDRESSES_PER_USER);
        }
        Address address = Address.create(userId, data.street(), data.number(), data.floorApt(), data.city(),
                data.province(), data.country(), data.postalCode());
        if (existing == 0) {
            address.markDefault();
        }
        return addressRepository.save(address);
    }

    @Override
    @Transactional
    public Address update(UUID userId, UUID addressId, AddressData data) {
        Address address = findOwnedAddress(userId, addressId);
        address.update(data.street(), data.number(), data.floorApt(), data.city(), data.province(), data.country(),
                data.postalCode());
        return addressRepository.save(address);
    }

    @Override
    @Transactional
    public void delete(UUID userId, UUID addressId) {
        lockUser(userId);
        addressRepository.delete(findOwnedAddress(userId, addressId));
    }

    @Override
    @Transactional
    public Address setDefault(UUID userId, UUID addressId) {
        lockUser(userId);
        addressRepository.clearDefaultByUserId(userId);
        // Se busca después del UPDATE: si no existe, la excepción revierte también el clear.
        Address address = findOwnedAddress(userId, addressId);
        address.markDefault();
        return addressRepository.save(address);
    }

    private void lockUser(UUID userId) {
        // All address-count/default changes lock the same existing parent row until commit.
        userRepository.findByIdForUpdate(userId).orElseThrow(() -> new UserNotFoundException(userId));
    }

    private Address findOwnedAddress(UUID userId, UUID addressId) {
        return addressRepository.findByIdAndUserId(addressId, userId)
                .orElseThrow(() -> new AddressNotFoundException(addressId));
    }
}
