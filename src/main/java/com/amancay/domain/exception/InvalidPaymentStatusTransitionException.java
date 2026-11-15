package com.amancay.domain.exception;

import com.amancay.domain.model.PaymentStatus;

public class InvalidPaymentStatusTransitionException extends RuntimeException {
    public InvalidPaymentStatusTransitionException(PaymentStatus from, PaymentStatus to) {
        super("Cannot transition payment from " + from + " to " + to);
    }
}
