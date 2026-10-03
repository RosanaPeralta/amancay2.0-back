package com.amancay.application.port.out;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.amancay.domain.model.Order;

public interface OrderRepositoryPort {
    Optional<Order> findById(UUID id);

    List<Order> findByUserId(UUID userId);

    List<Order> findAllById(Collection<UUID> ids);

    // Todas las ordenes de todos los usuarios, mas nuevas primero (panel de admin).
    List<Order> findAllNewestFirst();

    // Devuelve la orden tal como quedo persistida (con ids y timestamps asignados).
    Order save(Order order);
}
