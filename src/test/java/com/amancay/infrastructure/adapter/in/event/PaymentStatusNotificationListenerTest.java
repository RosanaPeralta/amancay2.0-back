package com.amancay.infrastructure.adapter.in.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.amancay.application.port.out.PayableOrderPort;
import com.amancay.application.port.out.PayableOrderPort.PayableOrder;
import com.amancay.application.port.out.PaymentRepositoryPort;
import com.amancay.domain.event.PaymentStatusChangedEvent;
import com.amancay.domain.exception.BuyerEmailNotFoundException;
import com.amancay.domain.exception.PaymentNotFoundException;
import com.amancay.domain.model.Payment;
import com.amancay.domain.model.PaymentMethod;
import com.amancay.domain.model.PaymentStatus;

class PaymentStatusNotificationListenerTest {

    private final Map<UUID, Payment> payments = new HashMap<>();
    private final Map<UUID, PayableOrder> orders = new HashMap<>();
    private final Map<UUID, String> emails = new HashMap<>();
    private final List<SentMail> sentMails = new ArrayList<>();

    private final PaymentStatusNotificationListener listener = new PaymentStatusNotificationListener(
            new FakePaymentRepository(),
            new FakePayableOrders(),
            userIds -> {
                Map<UUID, String> found = new HashMap<>();
                userIds.stream().filter(emails::containsKey).forEach(id -> found.put(id, emails.get(id)));
                return found;
            },
            (to, subject, body) -> sentMails.add(new SentMail(to, subject, body)),
            new PaymentRejectedMailContent());

    @Test
    void sendsThePaymentRejectedMailWhenThePaymentIsRechazado() {
        UUID buyerId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        UUID paymentId = UUID.randomUUID();
        emails.put(buyerId, "buyer@amancay.com");
        orders.put(orderId, new PayableOrder(orderId, buyerId, new BigDecimal("150.00"), List.of()));
        payments.put(paymentId, new Payment(paymentId, orderId, new BigDecimal("150.00"),
                PaymentMethod.TARJETA_CREDITO, PaymentStatus.RECHAZADO, null, null, null));

        listener.onPaymentStatusChanged(
                new PaymentStatusChangedEvent(paymentId, PaymentStatus.PENDIENTE, PaymentStatus.RECHAZADO));

        assertThat(sentMails).singleElement().satisfies(mail -> {
            assertThat(mail.to()).isEqualTo("buyer@amancay.com");
            assertThat(mail.subject()).contains(orderId.toString());
        });
    }

    @Test
    void doesNotSendAnythingWhenThePaymentIsApproved() {
        UUID paymentId = UUID.randomUUID();
        payments.put(paymentId, new Payment(paymentId, UUID.randomUUID(), new BigDecimal("150.00"),
                PaymentMethod.TARJETA_CREDITO, PaymentStatus.APROBADO, null, null, null));

        listener.onPaymentStatusChanged(
                new PaymentStatusChangedEvent(paymentId, PaymentStatus.PENDIENTE, PaymentStatus.APROBADO));

        assertThat(sentMails).isEmpty();
    }

    @Test
    void throwsWhenThePaymentNoLongerExists() {
        UUID paymentId = UUID.randomUUID();

        assertThatThrownBy(() -> listener.onPaymentStatusChanged(
                new PaymentStatusChangedEvent(paymentId, PaymentStatus.PENDIENTE, PaymentStatus.RECHAZADO)))
                .isInstanceOf(PaymentNotFoundException.class);
        assertThat(sentMails).isEmpty();
    }

    @Test
    void throwsWhenTheBuyerHasNoEmailOnFile() {
        UUID buyerId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        UUID paymentId = UUID.randomUUID();
        orders.put(orderId, new PayableOrder(orderId, buyerId, new BigDecimal("150.00"), List.of()));
        payments.put(paymentId, new Payment(paymentId, orderId, new BigDecimal("150.00"),
                PaymentMethod.TARJETA_CREDITO, PaymentStatus.RECHAZADO, null, null, null));

        assertThatThrownBy(() -> listener.onPaymentStatusChanged(
                new PaymentStatusChangedEvent(paymentId, PaymentStatus.PENDIENTE, PaymentStatus.RECHAZADO)))
                .isInstanceOf(BuyerEmailNotFoundException.class);
        assertThat(sentMails).isEmpty();
    }

    private record SentMail(String to, String subject, String body) {
    }

    private final class FakePaymentRepository implements PaymentRepositoryPort {
        @Override
        public Optional<Payment> findById(UUID id) {
            return Optional.ofNullable(payments.get(id));
        }

        @Override
        public List<Payment> findByOrderId(UUID orderId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<Payment> findByStatus(PaymentStatus status) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean existsByOrderIdAndStatus(UUID orderId, PaymentStatus status) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Payment save(Payment payment) {
            throw new UnsupportedOperationException();
        }
    }

    private final class FakePayableOrders implements PayableOrderPort {
        @Override
        public PayableOrder loadForRequester(UUID requesterId, UUID orderId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public PayableOrder load(UUID orderId) {
            return orders.get(orderId);
        }

        @Override
        public List<PayableOrder> loadAll(Collection<UUID> orderIds) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void markAsPaid(UUID orderId) {
            throw new UnsupportedOperationException();
        }
    }
}
