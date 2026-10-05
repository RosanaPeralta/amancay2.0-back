package com.amancay.infrastructure.adapter.out.event;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import com.amancay.application.port.out.PublishPaymentEventPort;
import com.amancay.domain.event.PaymentStatusChangedEvent;

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
