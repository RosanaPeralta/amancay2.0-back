package com.amancay.service;

import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.amancay.application.service.AddressApplicationService;
import com.amancay.domain.model.CustomerAddress;
import com.amancay.domain.ports.in.AddressUseCases;
import com.amancay.domain.ports.in.AddressUseCases.AddressData;
import com.amancay.domain.ports.out.AddressCatalogPort;
import com.amancay.domain.ports.out.AddressUserLockPort;
import com.amancay.dto.AddressDto;
import com.amancay.dto.CreateAddressRequest;
import com.amancay.dto.UpdateAddressRequest;
import com.amancay.infrastructure.adapters.out.persistence.AddressPersistenceAdapter;
import com.amancay.repository.AddressRepository;
import com.amancay.repository.UserRepository;

@Service
public class AddressService {
    static final int MAX_ADDRESSES_PER_USER = 10;

    private final AddressUseCases addressUseCases;

    @Autowired
    public AddressService(AddressUseCases addressUseCases) {
        this.addressUseCases = addressUseCases;
    }

    public AddressService(AddressRepository addressRepository, UserRepository userRepository) {
        AddressPersistenceAdapter adapter = new AddressPersistenceAdapter(addressRepository, userRepository);
        this.addressUseCases = new AddressApplicationService(adapter, (AddressUserLockPort) adapter);
    }

    @Transactional(readOnly = true)
    public List<AddressDto> list(UUID userId) {
        return addressUseCases.list(userId).stream().map(this::toDto).toList();
    }

    /** La primera dirección del usuario queda como predeterminada sin que tenga que marcarla. */
    @Transactional
    public AddressDto create(UUID userId, CreateAddressRequest request) {
        return toDto(addressUseCases.create(userId, data(request)));
    }

    @Transactional
    public AddressDto update(UUID userId, UUID addressId, UpdateAddressRequest request) {
        return toDto(addressUseCases.update(userId, addressId, data(request)));
    }

    /** Borrar la predeterminada no promueve otra: el usuario elige la siguiente explícitamente. */
    @Transactional
    public void delete(UUID userId, UUID addressId) {
        addressUseCases.delete(userId, addressId);
    }

    @Transactional
    public AddressDto setDefault(UUID userId, UUID addressId) {
        return toDto(addressUseCases.setDefault(userId, addressId));
    }

    private AddressData data(CreateAddressRequest request) {
        return new AddressData(request.street(), request.number(), request.floorApt(), request.city(), request.province(),
                request.country(), request.postalCode());
    }

    private AddressData data(UpdateAddressRequest request) {
        return new AddressData(request.street(), request.number(), request.floorApt(), request.city(), request.province(),
                request.country(), request.postalCode());
    }

    private AddressDto toDto(CustomerAddress address) {
        return new AddressDto(address.id(), address.street(), address.number(), address.floorApt(), address.city(),
                address.province(), address.country(), address.postalCode(), address.defaultAddress(),
                address.createdAt(), address.updatedAt());
    }
}
