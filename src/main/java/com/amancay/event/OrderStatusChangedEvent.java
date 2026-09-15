package com.amancay.event;

import java.util.UUID;

import com.amancay.entity.OrderStatus;

// Se publica en memoria via ApplicationEventPublisher despues de persistir el
// cambio de estado. Es el punto de enganche para el requerimiento de cola de
// mensajes: el dia que exista un broker real (RabbitMQ, etc.), un @Component
// que escuche este mismo evento pasa a publicarlo ahi en vez de solo loguear,
// sin tocar OrderService ni la entidad Order.
public record OrderStatusChangedEvent(UUID orderId, OrderStatus previousStatus, OrderStatus newStatus) {
}
