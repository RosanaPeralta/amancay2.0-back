package com.amancay.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.amancay.dto.AddressDto;
import com.amancay.dto.CreateAddressRequest;
import com.amancay.dto.UpdateAddressRequest;
import com.amancay.entity.Address;
import com.amancay.exceptions.AddressLimitReachedException;
import com.amancay.exceptions.AddressNotFoundException;
import com.amancay.repository.AddressRepository;

@Service
public class AddressService {
    static final int MAX_ADDRESSES_PER_USER = 10;

    private final AddressRepository addressRepository;

    public AddressService(AddressRepository addressRepository) {
        this.addressRepository = addressRepository;
    }

    @Transactional(readOnly = true)
    public List<AddressDto> list(UUID userId) {
        return addressRepository.findByUserIdOrderByCreatedAtAsc(userId).stream().map(this::toDto).toList();
    }

    /** La primera dirección del usuario queda como predeterminada sin que tenga que marcarla. */
    @Transactional
    public AddressDto create(UUID userId, CreateAddressRequest request) {
        long existing = addressRepository.countByUserId(userId);
        if (existing >= MAX_ADDRESSES_PER_USER) {
            throw new AddressLimitReachedException(MAX_ADDRESSES_PER_USER);
        }
        Address address = Address.create(userId, request.street(), request.number(), request.floorApt(),
                request.city(), request.province(), request.country(), request.postalCode());
        if (existing == 0) {
            address.markDefault();
        }
        return toDto(addressRepository.saveAndFlush(address));
    }

    @Transactional
    public AddressDto update(UUID userId, UUID addressId, UpdateAddressRequest request) {
        Address address = findOwnedAddress(userId, addressId);
        address.edit(request.street(), request.number(), request.floorApt(), request.city(), request.province(),
                request.country(), request.postalCode());
        return toDto(addressRepository.saveAndFlush(address));
    }

    /** Borrar la predeterminada no promueve otra: el usuario elige la siguiente explícitamente. */
    @Transactional
    public void delete(UUID userId, UUID addressId) {
        addressRepository.delete(findOwnedAddress(userId, addressId));
    }

    @Transactional
    public AddressDto setDefault(UUID userId, UUID addressId) {
        addressRepository.clearDefaultByUserId(userId);
        // Se busca después del UPDATE: si no existe, la excepción revierte también el clear.
        Address address = findOwnedAddress(userId, addressId);
        address.markDefault();
        return toDto(addressRepository.saveAndFlush(address));
    }

    private Address findOwnedAddress(UUID userId, UUID addressId) {
        return addressRepository.findByIdAndUserId(addressId, userId)
                .orElseThrow(() -> new AddressNotFoundException(addressId));
    }

    private AddressDto toDto(Address address) {
        return new AddressDto(address.getId(), address.getStreet(), address.getNumber(), address.getFloorApt(),
                address.getCity(), address.getProvince(), address.getCountry(), address.getPostalCode(),
                address.isDefaultAddress(), address.getCreatedAt(), address.getUpdatedAt());
    }
}
