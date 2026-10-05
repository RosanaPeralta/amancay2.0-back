package com.amancay.infrastructure.adapter.in.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.amancay.domain.event.PaymentStatusChangedEvent;

// Placeholder: por ahora solo loguea. Mismo mecanismo que OrderStatusNotificationListener:
// AFTER_COMMIT para correr solo sobre un pago ya persistido, y punto de enganche para el
// futuro consumer de la cola real con reintentos.
@Component
public class PaymentStatusNotificationListener {
    private static final Logger log = LoggerFactory.getLogger(PaymentStatusNotificationListener.class);

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPaymentStatusChanged(PaymentStatusChangedEvent event) {
        log.info("Payment {} changed status: {} -> {}", event.paymentId(), event.previousStatus(), event.newStatus());
    }
}
