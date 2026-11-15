package com.amancay.application.port.out;

import com.amancay.domain.event.PaymentStatusChangedEvent;

public interface PublishPaymentEventPort {
    void publish(PaymentStatusChangedEvent event);
}
