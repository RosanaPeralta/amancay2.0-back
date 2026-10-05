package com.amancay.infrastructure.adapter.in.event;

import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.amancay.application.port.out.BuyerEmailPort;
import com.amancay.application.port.out.MailSenderPort;
import com.amancay.application.port.out.PayableOrderPort;
import com.amancay.application.port.out.PayableOrderPort.PayableOrder;
import com.amancay.application.port.out.PaymentRepositoryPort;
import com.amancay.domain.event.PaymentStatusChangedEvent;
import com.amancay.domain.model.Payment;
import com.amancay.domain.model.PaymentStatus;

// Mismo mecanismo que OrderStatusNotificationListener: AFTER_COMMIT para correr solo sobre
// un pago ya persistido, y punto de enganche para el futuro consumer de la cola real con
// reintentos.
@Component
public class PaymentStatusNotificationListener {
    private static final Logger log = LoggerFactory.getLogger(PaymentStatusNotificationListener.class);

    private final PaymentRepositoryPort paymentRepository;
    private final PayableOrderPort payableOrderPort;
    private final BuyerEmailPort buyerEmailPort;
    private final MailSenderPort mailSender;

    public PaymentStatusNotificationListener(PaymentRepositoryPort paymentRepository,
            PayableOrderPort payableOrderPort, BuyerEmailPort buyerEmailPort, MailSenderPort mailSender) {
        this.paymentRepository = paymentRepository;
        this.payableOrderPort = payableOrderPort;
        this.buyerEmailPort = buyerEmailPort;
        this.mailSender = mailSender;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPaymentStatusChanged(PaymentStatusChangedEvent event) {
        log.info("Payment {} changed status: {} -> {}", event.paymentId(), event.previousStatus(), event.newStatus());

        if (event.newStatus() == PaymentStatus.RECHAZADO) {
            notifyPaymentRejected(event.paymentId());
        }
    }

    // Aprobado o no, el intento ya quedo resuelto por quien publico el evento: este metodo
    // solo avisa, no vuelve a validar ni a reintentar el cobro.
    private void notifyPaymentRejected(UUID paymentId) {
        Payment payment = paymentRepository.findById(paymentId).orElse(null);
        if (payment == null) {
            log.warn("No se pudo mandar el mail de pago rechazado: pago {} no encontrado", paymentId);
            return;
        }
        PayableOrder order = payableOrderPort.load(payment.getOrderId());
        String buyerEmail = buyerEmailPort.findEmailsByUserId(Set.of(order.buyerId())).get(order.buyerId());
        if (buyerEmail == null) {
            log.warn("No se pudo mandar el mail de pago rechazado: sin email para el usuario {}", order.buyerId());
            return;
        }
        mailSender.send(buyerEmail, subject(order), body(order, payment));
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
