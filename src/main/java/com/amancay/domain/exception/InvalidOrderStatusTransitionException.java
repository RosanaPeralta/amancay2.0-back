package com.amancay.domain.exception;

import com.amancay.domain.model.OrderStatus;
import com.amancay.domain.model.OrderTransitionTrigger;

public class InvalidOrderStatusTransitionException extends RuntimeException {
    public InvalidOrderStatusTransitionException(OrderStatus from, OrderStatus to) {
        super("Cannot transition order from " + from + " to " + to);
    }

    public InvalidOrderStatusTransitionException(OrderStatus from, OrderStatus to, OrderTransitionTrigger trigger) {
        super("Cannot transition order from " + from + " to " + to + " through " + trigger);
    }
}
