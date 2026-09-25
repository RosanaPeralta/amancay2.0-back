package com.amancay.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.amancay.event.OrderStatusChangedEvent;

// Placeholder del consumer async: hoy solo loguea. Cuando exista la cola de
// mensajes real, este listener (o uno nuevo) pasa a publicar en el broker
// (ej: evento "PedidoCreado" -> cola -> servicio de mail) en vez de loguear.
@Component
public class OrderStatusNotificationListener {
    private static final Logger log = LoggerFactory.getLogger(OrderStatusNotificationListener.class);

    @EventListener
    public void onOrderStatusChanged(OrderStatusChangedEvent event) {
        log.info("Order {} changed status: {} -> {}", event.orderId(), event.previousStatus(), event.newStatus());
    }
}
