package com.amancay.infrastructure.adapters.out.persistence.mapper;

import com.amancay.domain.model.CustomerAddress;

public final class AddressPersistenceMapper {
    private AddressPersistenceMapper() {
    }

    public static CustomerAddress toDomain(com.amancay.entity.Address entity) {
        return new CustomerAddress(entity.getId(), entity.getUserId(), entity.getStreet(), entity.getNumber(),
                entity.getFloorApt(), entity.getCity(), entity.getProvince(), entity.getCountry(), entity.getPostalCode(),
                entity.isDefaultAddress(), entity.getCreatedAt(), entity.getUpdatedAt());
    }

    public static com.amancay.entity.Address toPersistence(CustomerAddress address,
            com.amancay.entity.Address entity) {
        if (entity == null) {
            entity = com.amancay.entity.Address.create(address.userId(), address.street(), address.number(),
                address.floorApt(), address.city(), address.province(), address.country(), address.postalCode());
        } else {
            entity.update(address.street(), address.number(), address.floorApt(), address.city(), address.province(),
                    address.country(), address.postalCode());
        }
        if (address.defaultAddress()) {
            entity.markDefault();
        } else {
            entity.clearDefault();
        }
        return entity;
    }
}