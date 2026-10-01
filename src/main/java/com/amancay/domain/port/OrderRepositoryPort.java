package com.amancay.domain.port;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.amancay.domain.model.Order;

// Puerto de salida para persistir ordenes. La implementacion (OrderRepositoryAdapter,
// sobre Spring Data JPA) vive en infrastructure; el dominio y la aplicacion solo
// conocen esta interfaz.
public interface OrderRepositoryPort {
    Optional<Order> findById(UUID id);

    List<Order> findByUserId(UUID userId);

    List<Order> findAllById(Collection<UUID> ids);

    // Devuelve la orden tal como quedo persistida (con ids y timestamps asignados).
    Order save(Order order);
}
