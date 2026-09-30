package com.amancay.infrastructure.adapters.out.events;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import com.amancay.domain.event.OrderStateChanged;
import com.amancay.domain.ports.out.OrderEventPort;
import com.amancay.event.OrderStatusChangedEvent;

@Component
public class SpringOrderEventAdapter implements OrderEventPort {
    private final ApplicationEventPublisher eventPublisher;

    public SpringOrderEventAdapter(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    @Override
    public void publish(OrderStateChanged event) {
        eventPublisher.publishEvent(new OrderStatusChangedEvent(event.orderId(),
                com.amancay.entity.OrderStatus.valueOf(event.previousState().name()),
                com.amancay.entity.OrderStatus.valueOf(event.newState().name())));
    }
}