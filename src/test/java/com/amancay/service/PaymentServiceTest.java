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
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import com.amancay.dto.ConfirmPaymentRequest;
import com.amancay.dto.CreatePaymentRequest;
import com.amancay.dto.PaymentDto;
import com.amancay.entity.Order;
import com.amancay.entity.OrderStatus;
import com.amancay.entity.Payment;
import com.amancay.entity.PaymentMethod;
import com.amancay.entity.PaymentStatus;
import com.amancay.exceptions.OrderNotFoundException;
import com.amancay.exceptions.PaymentNotFoundException;
import com.amancay.repository.OrderRepository;
import com.amancay.repository.PaymentRepository;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    private static final UUID ORDER_ID = UUID.fromString("55555555-5555-5555-5555-555555555555");
    private static final UUID PAYMENT_ID = UUID.fromString("66666666-6666-6666-6666-666666666666");
    private static final UUID REQUESTER_ID = UUID.fromString("77777777-7777-7777-7777-777777777777");

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderService orderService;

    @Mock
    private PaymentProcessorResolver processorResolver;

    @Mock
    private PaymentProcessor processor;

    private PaymentService paymentService;

    @BeforeEach
    void setUp() {
        paymentService = new PaymentService(paymentRepository, orderRepository, orderService, processorResolver);
    }

    @Test
    void approvedPaymentMovesTheOrderToEnPreparacion() {
        Order order = order(ORDER_ID, BigDecimal.valueOf(200));
        CreatePaymentRequest request = cardRequest();
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));
        when(processorResolver.resolve(PaymentMethod.TARJETA_CREDITO)).thenReturn(processor);
        when(processor.process(order, request)).thenReturn(PaymentResult.approved());
        when(paymentRepository.saveAndFlush(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PaymentDto result = paymentService.createPayment(REQUESTER_ID, ORDER_ID, request);

        assertThat(result.status()).isEqualTo(PaymentStatus.APROBADO);
        assertThat(result.amount()).isEqualByComparingTo("200");
        verify(orderService).getOrder(REQUESTER_ID, ORDER_ID, null);
        verify(orderService).changeStatus(ORDER_ID, OrderStatus.EN_PREPARACION);
    }

    @Test
    void rejectedPaymentDoesNotTouchTheOrderStatus() {
        Order order = order(ORDER_ID, BigDecimal.valueOf(200));
        CreatePaymentRequest request = cardRequest();
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));
        when(processorResolver.resolve(PaymentMethod.TARJETA_CREDITO)).thenReturn(processor);
        when(processor.process(order, request)).thenReturn(PaymentResult.rejected("Card declined (simulated)"));
        when(paymentRepository.saveAndFlush(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PaymentDto result = paymentService.createPayment(REQUESTER_ID, ORDER_ID, request);

        assertThat(result.status()).isEqualTo(PaymentStatus.RECHAZADO);
        assertThat(result.reason()).isEqualTo("Card declined (simulated)");
        verify(orderService, never()).changeStatus(any(), any());
    }

    @Test
    void cannotPayAnOrderThatAlreadyHasAnApprovedPayment() {
        Order order = order(ORDER_ID, BigDecimal.valueOf(200));
        Payment approved = new Payment();
        approved.setStatus(PaymentStatus.APROBADO);
        order.addPayment(approved);
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> paymentService.createPayment(REQUESTER_ID, ORDER_ID, cardRequest()))
                .isInstanceOf(IllegalStateException.class);
        verify(paymentRepository, never()).saveAndFlush(any());
    }

    @Test
    void unknownOrderIsReportedAsNotFound() {
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.createPayment(REQUESTER_ID, ORDER_ID, cardRequest()))
                .isInstanceOf(OrderNotFoundException.class);
    }

    @Test
    void cannotPayAnotherUsersOrder() {
        when(orderService.getOrder(REQUESTER_ID, ORDER_ID, null))
                .thenThrow(new AccessDeniedException("No tienes permisos para ver esta orden"));

        assertThatThrownBy(() -> paymentService.createPayment(REQUESTER_ID, ORDER_ID, cardRequest()))
                .isInstanceOf(AccessDeniedException.class);
        verify(paymentRepository, never()).saveAndFlush(any());
    }

    @Test
    void retryingARejectedPaymentCreatesANewAttemptForTheSameOrder() {
        Order order = order(ORDER_ID, BigDecimal.valueOf(200));
        Payment rejected = payment(PAYMENT_ID, order, PaymentStatus.RECHAZADO);
        CreatePaymentRequest request = cardRequest();
        when(paymentRepository.findById(PAYMENT_ID)).thenReturn(Optional.of(rejected));
        when(processorResolver.resolve(PaymentMethod.TARJETA_CREDITO)).thenReturn(processor);
        when(processor.process(order, request)).thenReturn(PaymentResult.approved());
        when(paymentRepository.saveAndFlush(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PaymentDto result = paymentService.retryPayment(REQUESTER_ID, PAYMENT_ID, request);

        assertThat(result.status()).isEqualTo(PaymentStatus.APROBADO);
        verify(orderService).getOrder(REQUESTER_ID, ORDER_ID, null);
        verify(orderService).changeStatus(ORDER_ID, OrderStatus.EN_PREPARACION);
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
        when(orderService.getOrder(REQUESTER_ID, ORDER_ID, null))
                .thenThrow(new AccessDeniedException("No tienes permisos para ver esta orden"));

        assertThatThrownBy(() -> paymentService.retryPayment(REQUESTER_ID, PAYMENT_ID, cardRequest()))
                .isInstanceOf(AccessDeniedException.class);
        verify(paymentRepository, never()).saveAndFlush(any());
    }

    @Test
    void listsPaymentsOfOwnOrder() {
        Payment payment = payment(PAYMENT_ID, order(ORDER_ID, BigDecimal.valueOf(200)), PaymentStatus.APROBADO);
        when(paymentRepository.findByOrderIdOrderByCreatedAtDesc(ORDER_ID)).thenReturn(List.of(payment));

        List<PaymentDto> result = paymentService.listByOrder(REQUESTER_ID, ORDER_ID);

        assertThat(result).hasSize(1);
        verify(orderService).getOrder(REQUESTER_ID, ORDER_ID, null);
    }

    @Test
    void cannotListPaymentsOfAnotherUsersOrder() {
        when(orderService.getOrder(REQUESTER_ID, ORDER_ID, null))
                .thenThrow(new AccessDeniedException("No tienes permisos para ver esta orden"));

        assertThatThrownBy(() -> paymentService.listByOrder(REQUESTER_ID, ORDER_ID))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void confirmingAPendingPaymentAsApprovedMovesTheOrderForward() {
        Order order = order(ORDER_ID, BigDecimal.valueOf(200));
        Payment pending = payment(PAYMENT_ID, order, PaymentStatus.PENDIENTE);
        when(paymentRepository.findById(PAYMENT_ID)).thenReturn(Optional.of(pending));
        when(paymentRepository.saveAndFlush(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PaymentDto result = paymentService.confirmPayment(PAYMENT_ID,
                new ConfirmPaymentRequest(PaymentStatus.APROBADO));

        assertThat(result.status()).isEqualTo(PaymentStatus.APROBADO);
        verify(orderService).changeStatus(ORDER_ID, OrderStatus.EN_PREPARACION);
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
        verify(orderService, never()).changeStatus(any(), any());
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

    private Order order(UUID id, BigDecimal total) {
        Order order = new Order();
        order.setId(id);
        order.setTotal(total);
        return order;
    }

    private Payment payment(UUID id, Order order, PaymentStatus status) {
        Payment payment = new Payment();
        payment.setId(id);
        payment.setOrder(order);
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
