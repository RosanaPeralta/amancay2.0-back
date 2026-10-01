package com.amancay.application.port.in;

import java.util.List;
import java.util.UUID;

import com.amancay.domain.model.Address;

public interface ListAddressesQuery {
    List<Address> list(UUID userId);
}
