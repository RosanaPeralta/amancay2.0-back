package com.amancay.order.adapter.out.event;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import com.amancay.order.application.port.out.PublishOrderEventPort;
import com.amancay.order.domain.event.OrderStatusChangedEvent;

// Hoy publica en memoria (lo consume OrderStatusNotificationListener). El dia que
// exista un broker real (RabbitMQ, etc.), se agrega otro adaptador de este puerto
// que publique ahi, sin tocar OrderService.
@Component
class SpringOrderEventPublisher implements PublishOrderEventPort {

    private final ApplicationEventPublisher applicationEventPublisher;

    SpringOrderEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
        this.applicationEventPublisher = applicationEventPublisher;
    }

    @Override
    public void publish(OrderStatusChangedEvent event) {
        applicationEventPublisher.publishEvent(event);
    }
}
