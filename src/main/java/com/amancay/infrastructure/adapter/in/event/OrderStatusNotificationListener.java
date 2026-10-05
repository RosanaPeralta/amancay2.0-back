package com.amancay.infrastructure.adapter.in.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.amancay.domain.event.OrderStatusChangedEvent;

// Placeholder: por ahora solo loguea. AFTER_COMMIT: corre recien cuando el cambio de estado
// ya quedo persistido (nunca antes, nunca si la transaccion se revierte). Es el punto de
// enganche para el futuro consumer de la cola real con reintentos: cuando exista, esta clase
// (u otra que llame a la misma logica) pasa a ser lo que la cola invoca por cada mensaje, y
// una excepcion aca es la senal de "reintentame esta entrega".
@Component
public class OrderStatusNotificationListener {
    private static final Logger log = LoggerFactory.getLogger(OrderStatusNotificationListener.class);

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onOrderStatusChanged(OrderStatusChangedEvent event) {
        log.info("Order {} changed status: {} -> {}", event.orderId(), event.previousStatus(), event.newStatus());
    }
}
