package com.amancay.domain.ports.out;

import com.amancay.domain.event.OrderStateChanged;

public interface OrderEventPort {
    void publish(OrderStateChanged event);
}