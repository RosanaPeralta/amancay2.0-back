package com.amancay.order.application.port.out;

import java.util.Optional;
import java.util.UUID;

import com.amancay.order.domain.model.ShippingAddress;

public interface LoadShippingAddressPort {
    // Devuelve el snapshot de la direccion, listo para guardarse en la orden.
    Optional<ShippingAddress> findById(UUID addressId);
}
