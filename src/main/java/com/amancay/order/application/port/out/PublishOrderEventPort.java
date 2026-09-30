package com.amancay.order.application.port.out;

import com.amancay.order.domain.event.OrderStatusChangedEvent;

public interface PublishOrderEventPort {
    void publish(OrderStatusChangedEvent event);
}
