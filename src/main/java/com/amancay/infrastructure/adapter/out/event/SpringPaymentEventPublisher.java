package com.amancay.infrastructure.adapter.out.event;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import com.amancay.application.port.out.PublishPaymentEventPort;
import com.amancay.domain.event.PaymentStatusChangedEvent;

// Publica en memoria; lo consume PaymentStatusNotificationListener. Mismo mecanismo que
// SpringOrderEventPublisher: el dia que exista la cola real (con reintentos), este
// adaptador se reemplaza por uno que llame a esa API, sin tocar PaymentService.
@Component
class SpringPaymentEventPublisher implements PublishPaymentEventPort {

    private final ApplicationEventPublisher applicationEventPublisher;

    SpringPaymentEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
        this.applicationEventPublisher = applicationEventPublisher;
    }

    @Override
    public void publish(PaymentStatusChangedEvent event) {
        applicationEventPublisher.publishEvent(event);
    }
}
