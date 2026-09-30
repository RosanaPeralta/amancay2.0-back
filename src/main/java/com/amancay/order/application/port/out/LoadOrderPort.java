package com.amancay.order.application.port.out;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.amancay.order.domain.model.Order;

public interface LoadOrderPort {
    Optional<Order> findById(UUID id);

    List<Order> findByUserId(UUID userId);

    List<Order> findAllById(Collection<UUID> ids);
}
