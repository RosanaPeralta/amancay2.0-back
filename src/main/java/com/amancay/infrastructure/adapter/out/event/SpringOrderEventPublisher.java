package com.amancay.infrastructure.adapter.out.event;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import com.amancay.application.port.out.PublishOrderEventPort;
import com.amancay.domain.event.OrderStatusChangedEvent;

// Publica en memoria; lo consume OrderStatusNotificationListener.
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
