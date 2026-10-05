package com.amancay.infrastructure.adapter.in.web;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.amancay.infrastructure.adapter.in.event.OrderStatusNotificationListener;
import com.amancay.infrastructure.adapter.in.event.PaymentStatusNotificationListener;
import com.amancay.infrastructure.adapter.in.web.dto.OrderStatusChangedEventRequest;
import com.amancay.infrastructure.adapter.in.web.dto.PaymentStatusChangedEventRequest;

import jakarta.validation.Valid;

/**
 * Donde el futuro servicio de cola (bigqueue) entrega los mensajes de "cambio de estado"
 * para que se procesen, con reintentos a su cargo. Protegido por {@code QueueWebhookAuthFilter}
 * (clave compartida, no Supabase JWT: quien llama es ese servicio, no un usuario).
 *
 * <p>Cada endpoint llama al mismo metodo que hoy dispara {@code @TransactionalEventListener}
 * en memoria, asi que el resultado es identico sin importar quien lo invoque: un 2xx significa
 * "mensaje procesado, no reintentar"; un 4xx (ver {@code ApiExceptionHandler}: dato no
 * encontrado, validacion) significa "este mensaje nunca va a poder completarse, no
 * reintentar"; cualquier otra falla cae en 5xx por default de Spring Boot, que es la senal de
 * "reintentar".
 */
@RestController
@RequestMapping("/internal/events")
@Validated
public class EventDeliveryController {

    private final OrderStatusNotificationListener orderStatusNotificationListener;
    private final PaymentStatusNotificationListener paymentStatusNotificationListener;

    public EventDeliveryController(OrderStatusNotificationListener orderStatusNotificationListener,
            PaymentStatusNotificationListener paymentStatusNotificationListener) {
        this.orderStatusNotificationListener = orderStatusNotificationListener;
        this.paymentStatusNotificationListener = paymentStatusNotificationListener;
    }

    @PostMapping("/order-status-changed")
    public ResponseEntity<Void> onOrderStatusChanged(@Valid @RequestBody OrderStatusChangedEventRequest request) {
        orderStatusNotificationListener.onOrderStatusChanged(request.toDomainEvent());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/payment-status-changed")
    public ResponseEntity<Void> onPaymentStatusChanged(@Valid @RequestBody PaymentStatusChangedEventRequest request) {
        paymentStatusNotificationListener.onPaymentStatusChanged(request.toDomainEvent());
        return ResponseEntity.ok().build();
    }
}
