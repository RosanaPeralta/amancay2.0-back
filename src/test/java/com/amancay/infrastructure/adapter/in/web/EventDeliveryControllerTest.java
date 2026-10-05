package com.amancay.infrastructure.adapter.in.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.amancay.application.port.in.GetOrProvisionUserUseCase;
import com.amancay.domain.event.OrderStatusChangedEvent;
import com.amancay.domain.event.PaymentStatusChangedEvent;
import com.amancay.domain.exception.OrderNotFoundException;
import com.amancay.domain.model.OrderStatus;
import com.amancay.domain.model.PaymentStatus;
import com.amancay.infrastructure.adapter.in.event.OrderStatusNotificationListener;
import com.amancay.infrastructure.adapter.in.event.PaymentStatusNotificationListener;
import com.amancay.infrastructure.config.SecurityConfig;
import com.amancay.infrastructure.security.SupabaseJwtAuthenticationConverter;

@WebMvcTest(controllers = EventDeliveryController.class,
        properties = {
                "supabase.jwt.issuer=https://test.supabase.co/auth/v1",
                "supabase.jwt.jwks-uri=https://test.supabase.co/auth/v1/.well-known/jwks.json",
                "amancay.events.webhook.api-key=test-secret"})
@Import({SecurityConfig.class, SupabaseJwtAuthenticationConverter.class})
class EventDeliveryControllerTest {

    private static final UUID ORDER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID PAYMENT_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @MockitoBean
    private GetOrProvisionUserUseCase getOrProvisionUser;

    @MockitoBean
    private OrderStatusNotificationListener orderStatusNotificationListener;

    @MockitoBean
    private PaymentStatusNotificationListener paymentStatusNotificationListener;

    @Test
    void rejectsDeliveryWithoutTheApiKey() throws Exception {
        mockMvc.perform(post("/internal/events/order-status-changed")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(orderStatusChangedPayload()))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(orderStatusNotificationListener);
    }

    @Test
    void rejectsDeliveryWithTheWrongApiKey() throws Exception {
        mockMvc.perform(post("/internal/events/order-status-changed")
                        .header("X-Internal-Api-Key", "wrong-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(orderStatusChangedPayload()))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(orderStatusNotificationListener);
    }

    @Test
    void acksAnOrderStatusChangedDeliveryWithTheCorrectApiKey() throws Exception {
        mockMvc.perform(post("/internal/events/order-status-changed")
                        .header("X-Internal-Api-Key", "test-secret")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(orderStatusChangedPayload()))
                .andExpect(status().isOk());

        verify(orderStatusNotificationListener).onOrderStatusChanged(
                new OrderStatusChangedEvent(ORDER_ID, OrderStatus.CREADO, OrderStatus.EN_PREPARACION));
    }

    @Test
    void acksAPaymentStatusChangedDeliveryWithTheCorrectApiKey() throws Exception {
        mockMvc.perform(post("/internal/events/payment-status-changed")
                        .header("X-Internal-Api-Key", "test-secret")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(paymentStatusChangedPayload()))
                .andExpect(status().isOk());

        verify(paymentStatusNotificationListener).onPaymentStatusChanged(
                new PaymentStatusChangedEvent(PAYMENT_ID, PaymentStatus.PENDIENTE, PaymentStatus.RECHAZADO));
    }

    @Test
    void mapsAnUnprocessableDeliveryToNotFoundSoTheQueueDoesNotRetryIt() throws Exception {
        doThrow(new OrderNotFoundException(ORDER_ID)).when(orderStatusNotificationListener)
                .onOrderStatusChanged(any());

        mockMvc.perform(post("/internal/events/order-status-changed")
                        .header("X-Internal-Api-Key", "test-secret")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(orderStatusChangedPayload()))
                .andExpect(status().isNotFound());
    }

    @Test
    void rejectsAMalformedDeliveryAsABadRequest() throws Exception {
        mockMvc.perform(post("/internal/events/order-status-changed")
                        .header("X-Internal-Api-Key", "test-secret")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(orderStatusNotificationListener);
    }

    private String orderStatusChangedPayload() {
        return "{\"orderId\":\"" + ORDER_ID + "\",\"previousStatus\":\"CREADO\",\"newStatus\":\"EN_PREPARACION\"}";
    }

    private String paymentStatusChangedPayload() {
        return "{\"paymentId\":\"" + PAYMENT_ID + "\",\"previousStatus\":\"PENDIENTE\",\"newStatus\":\"RECHAZADO\"}";
    }
}
