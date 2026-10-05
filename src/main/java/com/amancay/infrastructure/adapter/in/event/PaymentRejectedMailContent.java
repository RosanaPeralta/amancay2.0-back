package com.amancay.infrastructure.adapter.in.event;

import org.springframework.stereotype.Component;

import com.amancay.application.port.out.PayableOrderPort.PayableOrder;
import com.amancay.domain.model.Payment;

@Component
public class PaymentRejectedMailContent {

    public MailContent build(PayableOrder order, Payment payment) {
        return new MailContent(subject(order), body(order, payment));
    }

    private String subject(PayableOrder order) {
        return "Tu pago para la compra #" + order.id() + " fue rechazado";
    }

    private String body(PayableOrder order, Payment payment) {
        return "Tu pago de " + payment.getAmount().toPlainString() + " (" + payment.getMethod()
                + ") para la compra #" + order.id() + " fue rechazado.\n\n"
                + "Tu compra no va a estar lista hasta que reintentes el pago.";
    }
}
