package com.amancay.infrastructure.adapter.in.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.amancay.domain.event.OrderStatusChangedEvent;

// Placeholder: por ahora solo loguea.
@Component
public class OrderStatusNotificationListener {
    private static final Logger log = LoggerFactory.getLogger(OrderStatusNotificationListener.class);

    @EventListener
    public void onOrderStatusChanged(OrderStatusChangedEvent event) {
        log.info("Order {} changed status: {} -> {}", event.orderId(), event.previousStatus(), event.newStatus());
    }
}
