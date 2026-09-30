package com.amancay.order.adapter.out.address;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.amancay.entity.Address;
import com.amancay.order.application.port.out.LoadShippingAddressPort;
import com.amancay.order.domain.model.ShippingAddress;
import com.amancay.repository.AddressRepository;

@Component
class ShippingAddressAdapter implements LoadShippingAddressPort {

    private final AddressRepository addressRepository;

    ShippingAddressAdapter(AddressRepository addressRepository) {
        this.addressRepository = addressRepository;
    }

    @Override
    public Optional<ShippingAddress> findById(UUID addressId) {
        return addressRepository.findById(addressId).map(this::toShippingAddress);
    }

    private ShippingAddress toShippingAddress(Address address) {
        return new ShippingAddress(address.getStreet(), String.valueOf(address.getNumber()), address.getFloorApt(),
                address.getCity(), address.getProvince(), address.getCountry(), address.getPostalCode());
    }
}
