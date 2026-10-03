package com.amancay.application.port.in;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import com.amancay.domain.model.Order;

public interface GetOrderQuery {

    // Valida que el requester pueda ver la orden: un comprador solo las propias, un
    // admin las de requestedUserId (o las propias si viene null).
    Order get(UUID requesterId, UUID orderId, UUID requestedUserId);

    // Sin chequeo de permisos: solo para llamadores que ya autorizaron por su cuenta
    // (ej: el admin confirmando un pago).
    Order getById(UUID orderId);

    List<Order> getAllById(Collection<UUID> orderIds);
}
