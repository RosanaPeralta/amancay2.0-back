package com.amancay.infrastructure.adapter.out.persistence.mapper;

import org.springframework.stereotype.Component;

import com.amancay.domain.model.Address;
import com.amancay.infrastructure.adapter.out.persistence.entity.AddressJpaEntity;

@Component
public class AddressPersistenceMapper {

    public Address toDomain(AddressJpaEntity entity) {
        return new Address(entity.getId(), entity.getUserId(), entity.getStreet(), entity.getNumber(),
                entity.getFloorApt(), entity.getCity(), entity.getProvince(), entity.getCountry(),
                entity.getPostalCode(), entity.isDefaultAddress(), entity.getCreatedAt(), entity.getUpdatedAt());
    }

    public void copyToEntity(Address address, AddressJpaEntity entity) {
        entity.setId(address.getId());
        entity.setUserId(address.getUserId());
        entity.setStreet(address.getStreet());
        entity.setNumber(address.getNumber());
        entity.setFloorApt(address.getFloorApt());
        entity.setCity(address.getCity());
        entity.setProvince(address.getProvince());
        entity.setCountry(address.getCountry());
        entity.setPostalCode(address.getPostalCode());
        entity.setDefaultAddress(address.isDefaultAddress());
    }
}
