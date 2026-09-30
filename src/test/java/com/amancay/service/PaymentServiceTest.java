package com.amancay.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.amancay.dto.AdminPendingPaymentDto;
import com.amancay.dto.ConfirmPaymentRequest;
import com.amancay.dto.CreatePaymentRequest;
import com.amancay.dto.PaymentDto;
import com.amancay.entity.Payment;
import com.amancay.entity.PaymentMethod;
import com.amancay.entity.PaymentStatus;
import com.amancay.entity.User;
import com.amancay.exceptions.InsufficientStockException;
import com.amancay.exceptions.PaymentNotFoundException;
import com.amancay.order.application.port.in.ChangeOrderStatusUseCase;
import com.amancay.order.application.port.in.GetOrderQuery;
import com.amancay.order.domain.exception.OrderAccessDeniedException;
import com.amancay.order.domain.exception.OrderNotFoundException;
import com.amancay.order.domain.model.Order;
import com.amancay.order.domain.model.OrderFixtures;
import com.amancay.order.domain.model.OrderItem;
import com.amancay.order.domain.model.OrderStatus;
import com.amancay.repository.PaymentRepository;
import com.amancay.repository.ProductVariantRepository;
import com.amancay.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    private static final UUID ORDER_ID = UUID.fromString("55555555-5555-5555-5555-555555555555");
    private static final UUID PAYMENT_ID = UUID.fromString("66666666-6666-6666-6666-666666666666");
    private static final UUID REQUESTER_ID = UUID.fromString("77777777-7777-7777-7777-777777777777");

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private GetOrderQuery getOrderQuery;

    @Mock
    private ChangeOrderStatusUseCase changeOrderStatusUseCase;

    @Mock
    private PaymentProcessorResolver processorResolver;

    @Mock
    private PaymentProcessor processor;

    @Mock
    private ProductVariantRepository productVariantRepository;

    @Mock
    private UserRepository userRepository;

    private PaymentService paymentService;

    @BeforeEach
    void setUp() {
        paymentService = new PaymentService(paymentRepository, getOrderQuery, changeOrderStatusUseCase, processorResolver,
                productVariantRepository, userRepository);
    }

    @Test
    void approvedPaymentMovesTheOrderToEnPreparacion() {
        Order order = order(ORDER_ID, BigDecimal.valueOf(200));
        CreatePaymentRequest request = cardRequest();
        when(getOrderQuery.get(REQUESTER_ID, ORDER_ID, null)).thenReturn(order);
        when(processorResolver.resolve(PaymentMethod.TARJETA_CREDITO)).thenReturn(processor);
        when(processor.process(order, request)).thenReturn(PaymentResult.approved());
        when(paymentRepository.saveAndFlush(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PaymentDto result = paymentService.createPayment(REQUESTER_ID, ORDER_ID, request);

        assertThat(result.status()).isEqualTo(PaymentStatus.APROBADO);
        assertThat(result.amount()).isEqualByComparingTo("200");
        verify(getOrderQuery).get(REQUESTER_ID, ORDER_ID, null);
        verify(changeOrderStatusUseCase).changeStatus(ORDER_ID, OrderStatus.EN_PREPARACION);
    }

    @Test
    void rejectedPaymentDoesNotTouchTheOrderStatus() {
        Order order = order(ORDER_ID, BigDecimal.valueOf(200));
        CreatePaymentRequest request = cardRequest();
        when(getOrderQuery.get(REQUESTER_ID, ORDER_ID, null)).thenReturn(order);
        when(processorResolver.resolve(PaymentMethod.TARJETA_CREDITO)).thenReturn(processor);
        when(processor.process(order, request)).thenReturn(PaymentResult.rejected("Card declined (simulated)"));
        when(paymentRepository.saveAndFlush(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PaymentDto result = paymentService.createPayment(REQUESTER_ID, ORDER_ID, request);

        assertThat(result.status()).isEqualTo(PaymentStatus.RECHAZADO);
        assertThat(result.reason()).isEqualTo("Card declined (simulated)");
        verify(changeOrderStatusUseCase, never()).changeStatus(any(), any());
    }

    @Test
    void cannotPayAnOrderThatAlreadyHasAnApprovedPayment() {
        Order order = order(ORDER_ID, BigDecimal.valueOf(200));
        when(getOrderQuery.get(REQUESTER_ID, ORDER_ID, null)).thenReturn(order);
        when(paymentRepository.existsByOrderIdAndStatus(ORDER_ID, PaymentStatus.APROBADO)).thenReturn(true);

        assertThatThrownBy(() -> paymentService.createPayment(REQUESTER_ID, ORDER_ID, cardRequest()))
                .isInstanceOf(IllegalStateException.class);
        verify(paymentRepository, never()).saveAndFlush(any());
    }

    @Test
    void unknownOrderIsReportedAsNotFound() {
        when(getOrderQuery.get(REQUESTER_ID, ORDER_ID, null)).thenThrow(new OrderNotFoundException(ORDER_ID));

        assertThatThrownBy(() -> paymentService.createPayment(REQUESTER_ID, ORDER_ID, cardRequest()))
                .isInstanceOf(OrderNotFoundException.class);
    }

    @Test
    void cannotPayAnotherUsersOrder() {
        when(getOrderQuery.get(REQUESTER_ID, ORDER_ID, null))
                .thenThrow(new OrderAccessDeniedException("No tienes permisos para ver esta orden"));

        assertThatThrownBy(() -> paymentService.createPayment(REQUESTER_ID, ORDER_ID, cardRequest()))
                .isInstanceOf(OrderAccessDeniedException.class);
        verify(paymentRepository, never()).saveAndFlush(any());
    }

    @Test
    void retryingARejectedPaymentCreatesANewAttemptForTheSameOrder() {
        Order order = order(ORDER_ID, BigDecimal.valueOf(200));
        Payment rejected = payment(PAYMENT_ID, order, PaymentStatus.RECHAZADO);
        CreatePaymentRequest request = cardRequest();
        when(paymentRepository.findById(PAYMENT_ID)).thenReturn(Optional.of(rejected));
        when(getOrderQuery.get(REQUESTER_ID, ORDER_ID, null)).thenReturn(order);
        when(processorResolver.resolve(PaymentMethod.TARJETA_CREDITO)).thenReturn(processor);
        when(processor.process(order, request)).thenReturn(PaymentResult.approved());
        when(paymentRepository.saveAndFlush(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PaymentDto result = paymentService.retryPayment(REQUESTER_ID, PAYMENT_ID, request);

        assertThat(result.status()).isEqualTo(PaymentStatus.APROBADO);
        verify(getOrderQuery).get(REQUESTER_ID, ORDER_ID, null);
        verify(changeOrderStatusUseCase).changeStatus(ORDER_ID, OrderStatus.EN_PREPARACION);
    }

    @Test
    void cannotRetryAPaymentThatWasNotRejected() {
        Order order = order(ORDER_ID, BigDecimal.valueOf(200));
        Payment approved = payment(PAYMENT_ID, order, PaymentStatus.APROBADO);
        when(paymentRepository.findById(PAYMENT_ID)).thenReturn(Optional.of(approved));

        assertThatThrownBy(() -> paymentService.retryPayment(REQUESTER_ID, PAYMENT_ID, cardRequest()))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void unknownPaymentIsReportedAsNotFoundOnRetry() {
        when(paymentRepository.findById(PAYMENT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.retryPayment(REQUESTER_ID, PAYMENT_ID, cardRequest()))
                .isInstanceOf(PaymentNotFoundException.class);
    }

    @Test
    void cannotRetryAnotherUsersPayment() {
        Order order = order(ORDER_ID, BigDecimal.valueOf(200));
        Payment rejected = payment(PAYMENT_ID, order, PaymentStatus.RECHAZADO);
        when(paymentRepository.findById(PAYMENT_ID)).thenReturn(Optional.of(rejected));
        when(getOrderQuery.get(REQUESTER_ID, ORDER_ID, null))
                .thenThrow(new OrderAccessDeniedException("No tienes permisos para ver esta orden"));

        assertThatThrownBy(() -> paymentService.retryPayment(REQUESTER_ID, PAYMENT_ID, cardRequest()))
                .isInstanceOf(OrderAccessDeniedException.class);
        verify(paymentRepository, never()).saveAndFlush(any());
    }

    @Test
    void listsPaymentsOfOwnOrder() {
        Payment payment = payment(PAYMENT_ID, order(ORDER_ID, BigDecimal.valueOf(200)), PaymentStatus.APROBADO);
        when(paymentRepository.findByOrderIdOrderByCreatedAtDesc(ORDER_ID)).thenReturn(List.of(payment));

        List<PaymentDto> result = paymentService.listByOrder(REQUESTER_ID, ORDER_ID);

        assertThat(result).hasSize(1);
        verify(getOrderQuery).get(REQUESTER_ID, ORDER_ID, null);
    }

    @Test
    void cannotListPaymentsOfAnotherUsersOrder() {
        when(getOrderQuery.get(REQUESTER_ID, ORDER_ID, null))
                .thenThrow(new OrderAccessDeniedException("No tienes permisos para ver esta orden"));

        assertThatThrownBy(() -> paymentService.listByOrder(REQUESTER_ID, ORDER_ID))
                .isInstanceOf(OrderAccessDeniedException.class);
    }

    @Test
    void confirmingAPendingPaymentAsApprovedMovesTheOrderForward() {
        Order order = order(ORDER_ID, BigDecimal.valueOf(200));
        Payment pending = payment(PAYMENT_ID, order, PaymentStatus.PENDIENTE);
        when(paymentRepository.findById(PAYMENT_ID)).thenReturn(Optional.of(pending));
        when(paymentRepository.saveAndFlush(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(getOrderQuery.getById(ORDER_ID)).thenReturn(order);

        PaymentDto result = paymentService.confirmPayment(PAYMENT_ID,
                new ConfirmPaymentRequest(PaymentStatus.APROBADO));

        assertThat(result.status()).isEqualTo(PaymentStatus.APROBADO);
        verify(changeOrderStatusUseCase).changeStatus(ORDER_ID, OrderStatus.EN_PREPARACION);
    }

    @Test
    void confirmingAPendingPaymentAsRejectedDoesNotTouchTheOrder() {
        Order order = order(ORDER_ID, BigDecimal.valueOf(200));
        Payment pending = payment(PAYMENT_ID, order, PaymentStatus.PENDIENTE);
        when(paymentRepository.findById(PAYMENT_ID)).thenReturn(Optional.of(pending));
        when(paymentRepository.saveAndFlush(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PaymentDto result = paymentService.confirmPayment(PAYMENT_ID,
                new ConfirmPaymentRequest(PaymentStatus.RECHAZADO));

        assertThat(result.status()).isEqualTo(PaymentStatus.RECHAZADO);
        verify(changeOrderStatusUseCase, never()).changeStatus(any(), any());
    }

    @Test
    void cannotConfirmAPaymentThatIsNotPending() {
        Order order = order(ORDER_ID, BigDecimal.valueOf(200));
        Payment approved = payment(PAYMENT_ID, order, PaymentStatus.APROBADO);
        when(paymentRepository.findById(PAYMENT_ID)).thenReturn(Optional.of(approved));

        assertThatThrownBy(() -> paymentService.confirmPayment(PAYMENT_ID,
                new ConfirmPaymentRequest(PaymentStatus.APROBADO)))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void cannotConfirmWithPendienteAsTheDecision() {
        assertThatThrownBy(() -> paymentService.confirmPayment(PAYMENT_ID,
                new ConfirmPaymentRequest(PaymentStatus.PENDIENTE)))
                .isInstanceOf(IllegalArgumentException.class);
        verify(paymentRepository, never()).findById(any());
    }

    @Test
    void approvedPaymentDecrementsStockOfEachItem() {
        UUID variantId = UUID.randomUUID();
        Order order = orderWithItem(ORDER_ID, BigDecimal.valueOf(200), variantId, 3);
        CreatePaymentRequest request = cardRequest();
        when(getOrderQuery.get(REQUESTER_ID, ORDER_ID, null)).thenReturn(order);
        when(processorResolver.resolve(PaymentMethod.TARJETA_CREDITO)).thenReturn(processor);
        when(processor.process(order, request)).thenReturn(PaymentResult.approved());
        when(paymentRepository.saveAndFlush(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(productVariantRepository.decrementStock(variantId, 3)).thenReturn(1);

        paymentService.createPayment(REQUESTER_ID, ORDER_ID, request);

        verify(productVariantRepository).decrementStock(variantId, 3);
    }

    @Test
    void approvalFailsWhenStockRanOutInTheMeantime() {
        UUID variantId = UUID.randomUUID();
        Order order = orderWithItem(ORDER_ID, BigDecimal.valueOf(200), variantId, 3);
        CreatePaymentRequest request = cardRequest();
        when(getOrderQuery.get(REQUESTER_ID, ORDER_ID, null)).thenReturn(order);
        when(processorResolver.resolve(PaymentMethod.TARJETA_CREDITO)).thenReturn(processor);
        when(processor.process(order, request)).thenReturn(PaymentResult.approved());
        when(paymentRepository.saveAndFlush(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(productVariantRepository.decrementStock(variantId, 3)).thenReturn(0);

        assertThatThrownBy(() -> paymentService.createPayment(REQUESTER_ID, ORDER_ID, request))
                .isInstanceOf(InsufficientStockException.class);
        verify(changeOrderStatusUseCase, never()).changeStatus(any(), any());
    }

    @Test
    void attachingTransferReferenceStoresIt() {
        Order order = order(ORDER_ID, BigDecimal.valueOf(200));
        Payment pending = payment(PAYMENT_ID, order, PaymentStatus.PENDIENTE);
        pending.setMethod(PaymentMethod.TRANSFERENCIA);
        when(paymentRepository.findById(PAYMENT_ID)).thenReturn(Optional.of(pending));
        when(paymentRepository.saveAndFlush(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PaymentDto result = paymentService.attachTransferReference(REQUESTER_ID, PAYMENT_ID, "TRX-123");

        assertThat(result.transferReference()).isEqualTo("TRX-123");
        verify(getOrderQuery).get(REQUESTER_ID, ORDER_ID, null);
    }

    @Test
    void cannotAttachTransferReferenceToACardPayment() {
        Order order = order(ORDER_ID, BigDecimal.valueOf(200));
        Payment pending = payment(PAYMENT_ID, order, PaymentStatus.PENDIENTE);
        when(paymentRepository.findById(PAYMENT_ID)).thenReturn(Optional.of(pending));

        assertThatThrownBy(() -> paymentService.attachTransferReference(REQUESTER_ID, PAYMENT_ID, "TRX-123"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void cannotAttachTransferReferenceToAnAlreadyDecidedPayment() {
        Order order = order(ORDER_ID, BigDecimal.valueOf(200));
        Payment approved = payment(PAYMENT_ID, order, PaymentStatus.APROBADO);
        approved.setMethod(PaymentMethod.TRANSFERENCIA);
        when(paymentRepository.findById(PAYMENT_ID)).thenReturn(Optional.of(approved));

        assertThatThrownBy(() -> paymentService.attachTransferReference(REQUESTER_ID, PAYMENT_ID, "TRX-123"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void cannotAttachTransferReferenceToAnotherUsersPayment() {
        Order order = order(ORDER_ID, BigDecimal.valueOf(200));
        Payment pending = payment(PAYMENT_ID, order, PaymentStatus.PENDIENTE);
        pending.setMethod(PaymentMethod.TRANSFERENCIA);
        when(paymentRepository.findById(PAYMENT_ID)).thenReturn(Optional.of(pending));
        when(getOrderQuery.get(REQUESTER_ID, ORDER_ID, null))
                .thenThrow(new OrderAccessDeniedException("No tienes permisos para ver esta orden"));

        assertThatThrownBy(() -> paymentService.attachTransferReference(REQUESTER_ID, PAYMENT_ID, "TRX-123"))
                .isInstanceOf(OrderAccessDeniedException.class);
        verify(paymentRepository, never()).saveAndFlush(any());
    }

    @Test
    void listPendingForAdminReturnsAllPendingPaymentsWithBuyerEmail() {
        UUID buyerId = UUID.randomUUID();
        Order order = OrderFixtures.persisted(ORDER_ID, buyerId, OrderStatus.CREADO, BigDecimal.valueOf(200), List.of());
        Payment pending = payment(PAYMENT_ID, order, PaymentStatus.PENDIENTE);
        pending.setMethod(PaymentMethod.TRANSFERENCIA);
        pending.setTransferReference("TRX-999");
        when(paymentRepository.findByStatusOrderByCreatedAtAsc(PaymentStatus.PENDIENTE)).thenReturn(List.of(pending));
        when(getOrderQuery.getAllById(Set.of(ORDER_ID))).thenReturn(List.of(order));
        User buyer = new User();
        buyer.setId(buyerId);
        buyer.setEmail("buyer@amancay.com");
        when(userRepository.findAllById(Set.of(buyerId))).thenReturn(List.of(buyer));

        List<AdminPendingPaymentDto> result = paymentService.listPendingForAdmin();

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().amount()).isEqualByComparingTo("200");
        assertThat(result.getFirst().transferReference()).isEqualTo("TRX-999");
        assertThat(result.getFirst().buyerEmail()).isEqualTo("buyer@amancay.com");
    }

    @Test
    void listPendingForAdminReturnsEmptyListWhenNothingIsPending() {
        when(paymentRepository.findByStatusOrderByCreatedAtAsc(PaymentStatus.PENDIENTE)).thenReturn(List.of());

        List<AdminPendingPaymentDto> result = paymentService.listPendingForAdmin();

        assertThat(result).isEmpty();
    }

    private Order order(UUID id, BigDecimal total) {
        return OrderFixtures.persisted(id, REQUESTER_ID, OrderStatus.CREADO, total, List.of());
    }

    private Order orderWithItem(UUID id, BigDecimal total, UUID variantId, int quantity) {
        return OrderFixtures.persisted(id, REQUESTER_ID, OrderStatus.CREADO, total,
                List.of(OrderItem.create(variantId, quantity, total)));
    }

    private Payment payment(UUID id, Order order, PaymentStatus status) {
        Payment payment = new Payment();
        payment.setId(id);
        payment.setOrderId(order.getId());
        payment.setAmount(order.getTotal());
        payment.setMethod(PaymentMethod.TARJETA_CREDITO);
        payment.setStatus(status);
        return payment;
    }

    private CreatePaymentRequest cardRequest() {
        return new CreatePaymentRequest(PaymentMethod.TARJETA_CREDITO,
                new CreatePaymentRequest.CardData("4111111111111234", "Test User", "12/30", "123"));
    }
}
