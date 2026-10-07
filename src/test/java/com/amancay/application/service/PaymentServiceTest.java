package com.amancay.application.service;

import static com.amancay.application.service.fake.Admins.ADMIN_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.amancay.application.port.in.ListPendingPaymentsQuery.PendingPayment;
import com.amancay.application.port.in.PaymentOutcome;
import com.amancay.application.port.out.PayableOrderPort.PayableOrder;
import com.amancay.application.port.out.PayableOrderPort;
import com.amancay.application.port.out.PaymentRepositoryPort;
import com.amancay.application.port.out.StockPort;
import com.amancay.application.service.fake.Admins;
import com.amancay.domain.exception.AdminRequiredException;
import com.amancay.domain.exception.InsufficientStockException;
import com.amancay.domain.exception.OrderAccessDeniedException;
import com.amancay.domain.exception.OrderNotFoundException;
import com.amancay.domain.exception.PaymentNotFoundException;
import com.amancay.domain.model.Payment;
import com.amancay.domain.model.PaymentDetails;
import com.amancay.domain.model.PaymentMethod;
import com.amancay.domain.model.PaymentResult;
import com.amancay.domain.model.PaymentStatus;

// Sin Mockito ni Spring: cada puerto de salida se reemplaza por un fake en memoria.
class PaymentServiceTest {

    private static final UUID REQUESTER_ID = UUID.fromString("77777777-7777-7777-7777-777777777777");

    private final InMemoryPayments payments = new InMemoryPayments();
    private final FakeOrders orders = new FakeOrders();
    private final FakeStock stock = new FakeStock();
    private final Map<UUID, String> emails = new HashMap<>();
    private PaymentResult nextProcessorResult = PaymentResult.approved();
    private final List<PaymentDetails> processedDetails = new ArrayList<>();

    private PaymentService paymentService;

    @BeforeEach
    void setUp() {
        paymentService = new PaymentService(payments, orders,
                (order, details) -> {
                    processedDetails.add(details);
                    return nextProcessorResult;
                },
                stock,
                userIds -> {
                    Map<UUID, String> found = new HashMap<>();
                    userIds.stream().filter(emails::containsKey).forEach(id -> found.put(id, emails.get(id)));
                    return found;
                },
                Admins.guard());
    }

    @Test
    void approvedPaymentMovesTheOrderToEnPreparacion() {
        PayableOrder order = orders.create(REQUESTER_ID, "200");

        PaymentOutcome result = paymentService.create(REQUESTER_ID, order.id(), card());

        assertThat(result.payment().getStatus()).isEqualTo(PaymentStatus.APROBADO);
        assertThat(result.payment().getAmount()).isEqualByComparingTo("200");
        assertThat(result.payment().getId()).isNotNull();
        assertThat(orders.paid).containsExactly(order.id());
    }

    @Test
    void rejectedPaymentIsSavedWithItsReasonAndDoesNotTouchTheOrder() {
        PayableOrder order = orders.create(REQUESTER_ID, "200");
        nextProcessorResult = PaymentResult.rejected("Card declined (simulated)");

        PaymentOutcome result = paymentService.create(REQUESTER_ID, order.id(), card());

        assertThat(result.payment().getStatus()).isEqualTo(PaymentStatus.RECHAZADO);
        assertThat(result.reason()).isEqualTo("Card declined (simulated)");
        assertThat(payments.findByOrderId(order.id())).hasSize(1);
        assertThat(orders.paid).isEmpty();
    }

    @Test
    void cannotPayAnOrderThatAlreadyHasAnApprovedPayment() {
        PayableOrder order = orders.create(REQUESTER_ID, "200");
        payments.put(order.id(), PaymentStatus.APROBADO, PaymentMethod.TARJETA_CREDITO);

        assertThatThrownBy(() -> paymentService.create(REQUESTER_ID, order.id(), card()))
                .isInstanceOf(IllegalStateException.class);
        assertThat(payments.findByOrderId(order.id())).hasSize(1);
        assertThat(processedDetails).isEmpty();
    }

    @Test
    void unknownOrderIsReportedAsNotFound() {
        assertThatThrownBy(() -> paymentService.create(REQUESTER_ID, UUID.randomUUID(), card()))
                .isInstanceOf(OrderNotFoundException.class);
    }

    @Test
    void cannotPayAnotherUsersOrder() {
        PayableOrder order = orders.create(UUID.randomUUID(), "200");

        assertThatThrownBy(() -> paymentService.create(REQUESTER_ID, order.id(), card()))
                .isInstanceOf(OrderAccessDeniedException.class);
        assertThat(payments.findByOrderId(order.id())).isEmpty();
    }

    @Test
    void retryingARejectedPaymentCreatesANewAttemptForTheSameOrder() {
        PayableOrder order = orders.create(REQUESTER_ID, "200");
        Payment rejected = payments.put(order.id(), PaymentStatus.RECHAZADO, PaymentMethod.TARJETA_CREDITO);

        PaymentOutcome result = paymentService.retry(REQUESTER_ID, rejected.getId(), card());

        assertThat(result.payment().getStatus()).isEqualTo(PaymentStatus.APROBADO);
        assertThat(result.payment().getId()).isNotEqualTo(rejected.getId());
        assertThat(payments.findByOrderId(order.id())).hasSize(2);
        assertThat(orders.paid).containsExactly(order.id());
    }

    @Test
    void cannotRetryAPaymentThatWasNotRejected() {
        PayableOrder order = orders.create(REQUESTER_ID, "200");
        Payment approved = payments.put(order.id(), PaymentStatus.APROBADO, PaymentMethod.TARJETA_CREDITO);

        assertThatThrownBy(() -> paymentService.retry(REQUESTER_ID, approved.getId(), card()))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void unknownPaymentIsReportedAsNotFoundOnRetry() {
        assertThatThrownBy(() -> paymentService.retry(REQUESTER_ID, UUID.randomUUID(), card()))
                .isInstanceOf(PaymentNotFoundException.class);
    }

    @Test
    void cannotRetryAnotherUsersPayment() {
        PayableOrder order = orders.create(UUID.randomUUID(), "200");
        Payment rejected = payments.put(order.id(), PaymentStatus.RECHAZADO, PaymentMethod.TARJETA_CREDITO);

        assertThatThrownBy(() -> paymentService.retry(REQUESTER_ID, rejected.getId(), card()))
                .isInstanceOf(OrderAccessDeniedException.class);
        assertThat(payments.findByOrderId(order.id())).hasSize(1);
    }

    @Test
    void listsPaymentsOfOwnOrderNewestFirst() {
        PayableOrder order = orders.create(REQUESTER_ID, "200");
        Payment older = payments.put(order.id(), PaymentStatus.RECHAZADO, PaymentMethod.TARJETA_CREDITO);
        Payment newer = payments.put(order.id(), PaymentStatus.APROBADO, PaymentMethod.TARJETA_CREDITO);

        List<Payment> result = paymentService.listByOrder(REQUESTER_ID, order.id());

        assertThat(result).extracting(Payment::getId).containsExactly(newer.getId(), older.getId());
    }

    @Test
    void cannotListPaymentsOfAnotherUsersOrder() {
        PayableOrder order = orders.create(UUID.randomUUID(), "200");

        assertThatThrownBy(() -> paymentService.listByOrder(REQUESTER_ID, order.id()))
                .isInstanceOf(OrderAccessDeniedException.class);
    }

    @Test
    void confirmingAPendingPaymentAsApprovedMovesTheOrderForward() {
        PayableOrder order = orders.create(UUID.randomUUID(), "200");
        Payment pending = payments.put(order.id(), PaymentStatus.PENDIENTE, PaymentMethod.TRANSFERENCIA);

        Payment result = paymentService.confirm(ADMIN_ID, pending.getId(), PaymentStatus.APROBADO);

        assertThat(result.getStatus()).isEqualTo(PaymentStatus.APROBADO);
        assertThat(payments.findById(pending.getId()).orElseThrow().getStatus()).isEqualTo(PaymentStatus.APROBADO);
        assertThat(orders.paid).containsExactly(order.id());
    }

    @Test
    void confirmingAPendingPaymentAsRejectedDoesNotTouchTheOrder() {
        PayableOrder order = orders.create(UUID.randomUUID(), "200");
        Payment pending = payments.put(order.id(), PaymentStatus.PENDIENTE, PaymentMethod.TRANSFERENCIA);

        Payment result = paymentService.confirm(ADMIN_ID, pending.getId(), PaymentStatus.RECHAZADO);

        assertThat(result.getStatus()).isEqualTo(PaymentStatus.RECHAZADO);
        assertThat(orders.paid).isEmpty();
    }

    @Test
    void buyerCannotConfirmPaymentsNorListPendingOnes() {
        PayableOrder order = orders.create(REQUESTER_ID, "200");
        Payment pending = payments.put(order.id(), PaymentStatus.PENDIENTE, PaymentMethod.TRANSFERENCIA);

        assertThatThrownBy(() -> paymentService.confirm(REQUESTER_ID, pending.getId(), PaymentStatus.APROBADO))
                .isInstanceOf(AdminRequiredException.class);
        assertThatThrownBy(() -> paymentService.listPending(REQUESTER_ID)).isInstanceOf(AdminRequiredException.class);
        assertThat(payments.findById(pending.getId()).orElseThrow().getStatus()).isEqualTo(PaymentStatus.PENDIENTE);
        assertThat(orders.paid).isEmpty();
    }

    @Test
    void cannotConfirmAPaymentThatIsNotPending() {
        PayableOrder order = orders.create(UUID.randomUUID(), "200");
        Payment approved = payments.put(order.id(), PaymentStatus.APROBADO, PaymentMethod.TRANSFERENCIA);

        assertThatThrownBy(() -> paymentService.confirm(ADMIN_ID, approved.getId(), PaymentStatus.APROBADO))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void cannotConfirmWithPendienteAsTheDecisionEvenForAnUnknownPayment() {
        // Se valida la decision antes de buscar el pago: no es un 404 sino un 400.
        assertThatThrownBy(() -> paymentService.confirm(ADMIN_ID, UUID.randomUUID(), PaymentStatus.PENDIENTE))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void approvedPaymentDecrementsStockOfEachItem() {
        UUID variantId = UUID.randomUUID();
        stock.set(variantId, 5);
        PayableOrder order = orders.create(REQUESTER_ID, "200", new PayableOrder.Line(variantId, 3));

        paymentService.create(REQUESTER_ID, order.id(), card());

        assertThat(stock.of(variantId)).isEqualTo(2);
    }

    @Test
    void approvalFailsWhenStockRanOutInTheMeantime() {
        UUID variantId = UUID.randomUUID();
        stock.set(variantId, 2);
        PayableOrder order = orders.create(REQUESTER_ID, "200", new PayableOrder.Line(variantId, 3));

        assertThatThrownBy(() -> paymentService.create(REQUESTER_ID, order.id(), card()))
                .isInstanceOf(InsufficientStockException.class);
        assertThat(orders.paid).isEmpty();
    }

    @Test
    void confirmingATransferDecrementsStockToo() {
        UUID variantId = UUID.randomUUID();
        stock.set(variantId, 4);
        PayableOrder order = orders.create(UUID.randomUUID(), "200", new PayableOrder.Line(variantId, 1));
        Payment pending = payments.put(order.id(), PaymentStatus.PENDIENTE, PaymentMethod.TRANSFERENCIA);

        paymentService.confirm(ADMIN_ID, pending.getId(), PaymentStatus.APROBADO);

        assertThat(stock.of(variantId)).isEqualTo(3);
    }

    @Test
    void attachingTransferReferenceStoresIt() {
        PayableOrder order = orders.create(REQUESTER_ID, "200");
        Payment pending = payments.put(order.id(), PaymentStatus.PENDIENTE, PaymentMethod.TRANSFERENCIA);

        Payment result = paymentService.attachTransferReference(REQUESTER_ID, pending.getId(), "TRX-123");

        assertThat(result.getTransferReference()).isEqualTo("TRX-123");
        assertThat(payments.findById(pending.getId()).orElseThrow().getTransferReference()).isEqualTo("TRX-123");
    }

    @Test
    void cannotAttachTransferReferenceToACardPayment() {
        PayableOrder order = orders.create(REQUESTER_ID, "200");
        Payment pending = payments.put(order.id(), PaymentStatus.PENDIENTE, PaymentMethod.TARJETA_CREDITO);

        assertThatThrownBy(() -> paymentService.attachTransferReference(REQUESTER_ID, pending.getId(), "TRX-123"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void cannotAttachTransferReferenceToAnAlreadyDecidedPayment() {
        PayableOrder order = orders.create(REQUESTER_ID, "200");
        Payment approved = payments.put(order.id(), PaymentStatus.APROBADO, PaymentMethod.TRANSFERENCIA);

        assertThatThrownBy(() -> paymentService.attachTransferReference(REQUESTER_ID, approved.getId(), "TRX-123"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void cannotAttachTransferReferenceToAnotherUsersPayment() {
        PayableOrder order = orders.create(UUID.randomUUID(), "200");
        Payment pending = payments.put(order.id(), PaymentStatus.PENDIENTE, PaymentMethod.TRANSFERENCIA);

        assertThatThrownBy(() -> paymentService.attachTransferReference(REQUESTER_ID, pending.getId(), "TRX-123"))
                .isInstanceOf(OrderAccessDeniedException.class);
        assertThat(payments.findById(pending.getId()).orElseThrow().getTransferReference()).isNull();
    }

    @Test
    void listPendingReturnsAllPendingPaymentsWithBuyerEmail() {
        UUID buyerId = UUID.randomUUID();
        emails.put(buyerId, "buyer@amancay.com");
        PayableOrder order = orders.create(buyerId, "200");
        Payment pending = payments.put(order.id(), PaymentStatus.PENDIENTE, PaymentMethod.TRANSFERENCIA);
        payments.put(order.id(), PaymentStatus.RECHAZADO, PaymentMethod.TARJETA_CREDITO);

        List<PendingPayment> result = paymentService.listPending(ADMIN_ID);

        assertThat(result).singleElement().satisfies(item -> {
            assertThat(item.payment().getId()).isEqualTo(pending.getId());
            assertThat(item.payment().getAmount()).isEqualByComparingTo("200");
            assertThat(item.buyerEmail()).isEqualTo("buyer@amancay.com");
        });
    }

    @Test
    void listPendingReturnsEmptyListWhenNothingIsPending() {
        assertThat(paymentService.listPending(ADMIN_ID)).isEmpty();
    }

    private PaymentDetails card() {
        return new PaymentDetails(PaymentMethod.TARJETA_CREDITO,
                new PaymentDetails.CardData("4111111111111234", "Test User", "12/30", "123"));
    }

    // Imita a la base: asigna ids y un createdAt creciente, para poder ordenar.
    private static final class InMemoryPayments implements PaymentRepositoryPort {

        private final Map<UUID, Payment> store = new LinkedHashMap<>();
        private Instant clock = Instant.parse("2026-01-01T00:00:00Z");

        Payment put(UUID orderId, PaymentStatus status, PaymentMethod method) {
            return save(new Payment(null, orderId, new BigDecimal("200"), method, status, null, null, null));
        }

        @Override
        public Optional<Payment> findById(UUID id) {
            return Optional.ofNullable(store.get(id)).map(InMemoryPayments::copy);
        }

        @Override
        public List<Payment> findByOrderId(UUID orderId) {
            return store.values().stream().filter(payment -> payment.getOrderId().equals(orderId))
                    .sorted(Comparator.comparing(Payment::getCreatedAt).reversed()).map(InMemoryPayments::copy)
                    .toList();
        }

        @Override
        public List<Payment> findByStatus(PaymentStatus status) {
            return store.values().stream().filter(payment -> payment.getStatus() == status)
                    .sorted(Comparator.comparing(Payment::getCreatedAt)).map(InMemoryPayments::copy).toList();
        }

        @Override
        public boolean existsByOrderIdAndStatus(UUID orderId, PaymentStatus status) {
            return store.values().stream()
                    .anyMatch(payment -> payment.getOrderId().equals(orderId) && payment.getStatus() == status);
        }

        @Override
        public Payment save(Payment payment) {
            clock = clock.plusSeconds(1);
            UUID id = payment.getId() == null ? UUID.randomUUID() : payment.getId();
            Payment saved = new Payment(id, payment.getOrderId(), payment.getAmount(), payment.getMethod(),
                    payment.getStatus(), payment.getTransferReference(),
                    payment.getCreatedAt() == null ? clock : payment.getCreatedAt(), clock);
            store.put(id, saved);
            return copy(saved);
        }

        private static Payment copy(Payment payment) {
            return new Payment(payment.getId(), payment.getOrderId(), payment.getAmount(), payment.getMethod(),
                    payment.getStatus(), payment.getTransferReference(), payment.getCreatedAt(),
                    payment.getUpdatedAt());
        }
    }

    // Imita las reglas de acceso de las ordenes: 404 si no existe, 403 si no es del requester.
    private static final class FakeOrders implements PayableOrderPort {

        private final Map<UUID, PayableOrder> store = new HashMap<>();
        private final List<UUID> paid = new ArrayList<>();

        PayableOrder create(UUID buyerId, String total, PayableOrder.Line... lines) {
            PayableOrder order = new PayableOrder(UUID.randomUUID(), buyerId, new BigDecimal(total), List.of(lines));
            store.put(order.id(), order);
            return order;
        }

        @Override
        public PayableOrder loadForRequester(UUID requesterId, UUID orderId) {
            PayableOrder order = load(orderId);
            if (!order.buyerId().equals(requesterId)) {
                throw new OrderAccessDeniedException("No tienes permisos para ver esta orden");
            }
            return order;
        }

        @Override
        public PayableOrder load(UUID orderId) {
            return Optional.ofNullable(store.get(orderId)).orElseThrow(() -> new OrderNotFoundException(orderId));
        }

        @Override
        public List<PayableOrder> loadAll(Collection<UUID> orderIds) {
            return orderIds.stream().map(store::get).filter(order -> order != null).toList();
        }

        @Override
        public void markAsPaid(UUID orderId) {
            paid.add(orderId);
        }
    }

    private static final class FakeStock implements StockPort {

        private final Map<UUID, Integer> quantities = new HashMap<>();

        void set(UUID variantId, int quantity) {
            quantities.put(variantId, quantity);
        }

        int of(UUID variantId) {
            return quantities.get(variantId);
        }

        @Override
        public boolean decrement(UUID productVariantId, int quantity) {
            int available = quantities.getOrDefault(productVariantId, 0);
            if (available < quantity) {
                return false;
            }
            quantities.put(productVariantId, available - quantity);
            return true;
        }
    }
}
