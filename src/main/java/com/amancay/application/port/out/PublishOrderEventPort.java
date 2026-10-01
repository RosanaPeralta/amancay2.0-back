package com.amancay.application.port.out;

import com.amancay.domain.event.OrderStatusChangedEvent;

public interface PublishOrderEventPort {
    void publish(OrderStatusChangedEvent event);
}
