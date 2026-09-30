package com.amancay.application.service;

import java.util.List;
import java.util.UUID;

import com.amancay.domain.model.CustomerAddress;
import com.amancay.domain.ports.in.AddressUseCases;
import com.amancay.domain.ports.out.AddressCatalogPort;
import com.amancay.domain.ports.out.AddressUserLockPort;
import com.amancay.exceptions.AddressLimitReachedException;
import com.amancay.exceptions.AddressNotFoundException;

public class AddressApplicationService implements AddressUseCases {
    private final AddressCatalogPort addressCatalog;
    private final AddressUserLockPort userLock;

    public AddressApplicationService(AddressCatalogPort addressCatalog, AddressUserLockPort userLock) {
        this.addressCatalog = addressCatalog;
        this.userLock = userLock;
    }

    @Override
    public List<CustomerAddress> list(UUID userId) {
        return addressCatalog.findByUserId(userId);
    }

    @Override
    public CustomerAddress create(UUID userId, AddressData data) {
        userLock.lockExistingUser(userId);
        long existing = addressCatalog.countByUserId(userId);
        if (existing >= 10) {
            throw new AddressLimitReachedException(10);
        }
        CustomerAddress address = CustomerAddress.create(userId, data.street(), data.number(), data.floorApt(),
                data.city(), data.province(), data.country(), data.postalCode());
        if (existing == 0) {
            address = address.withDefault(true);
        }
        return addressCatalog.save(address);
    }

    @Override
    public CustomerAddress update(UUID userId, UUID addressId, AddressData data) {
        CustomerAddress address = findOwnedAddress(userId, addressId);
        return addressCatalog.save(address.update(data.street(), data.number(), data.floorApt(), data.city(),
                data.province(), data.country(), data.postalCode()));
    }

    @Override
    public void delete(UUID userId, UUID addressId) {
        userLock.lockExistingUser(userId);
        addressCatalog.delete(findOwnedAddress(userId, addressId));
    }

    @Override
    public CustomerAddress setDefault(UUID userId, UUID addressId) {
        userLock.lockExistingUser(userId);
        addressCatalog.clearDefaultByUserId(userId);
        CustomerAddress address = findOwnedAddress(userId, addressId);
        return addressCatalog.save(address.withDefault(true));
    }

    private CustomerAddress findOwnedAddress(UUID userId, UUID addressId) {
        return addressCatalog.findByIdAndUserId(addressId, userId)
                .orElseThrow(() -> new AddressNotFoundException(addressId));
    }
}