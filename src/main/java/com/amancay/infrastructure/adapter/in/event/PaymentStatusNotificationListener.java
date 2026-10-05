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
import com.amancay.domain.exception.BuyerEmailNotFoundException;
import com.amancay.domain.exception.PaymentNotFoundException;
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
    private final PaymentRejectedMailContent mailContent;

    public PaymentStatusNotificationListener(PaymentRepositoryPort paymentRepository,
            PayableOrderPort payableOrderPort, BuyerEmailPort buyerEmailPort, MailSenderPort mailSender,
            PaymentRejectedMailContent mailContent) {
        this.paymentRepository = paymentRepository;
        this.payableOrderPort = payableOrderPort;
        this.buyerEmailPort = buyerEmailPort;
        this.mailSender = mailSender;
        this.mailContent = mailContent;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPaymentStatusChanged(PaymentStatusChangedEvent event) {
        log.info("Payment {} changed status: {} -> {}", event.paymentId(), event.previousStatus(), event.newStatus());

        if (event.newStatus() == PaymentStatus.RECHAZADO) {
            notifyPaymentRejected(event.paymentId());
        }
    }

    // Aprobado o no, el intento ya quedo resuelto por quien publico el evento: este metodo
    // solo avisa, no vuelve a validar ni a reintentar el cobro. Tira excepcion (en vez de
    // solo loguear) ante datos faltantes: via Spring, AFTER_COMMIT ya la loguea como error;
    // via el webhook de la cola (EventDeliveryController), mapea a 4xx para que no se
    // reintente un mensaje que nunca va a poder completarse.
    private void notifyPaymentRejected(UUID paymentId) {
        Payment payment = paymentRepository.findById(paymentId).orElseThrow(() -> new PaymentNotFoundException(paymentId));
        PayableOrder order = payableOrderPort.load(payment.getOrderId());
        String buyerEmail = buyerEmailPort.findEmailsByUserId(Set.of(order.buyerId())).get(order.buyerId());
        if (buyerEmail == null) {
            throw new BuyerEmailNotFoundException(order.buyerId());
        }
        MailContent content = mailContent.build(order, payment);
        mailSender.send(buyerEmail, content.subject(), content.body());
    }
}
