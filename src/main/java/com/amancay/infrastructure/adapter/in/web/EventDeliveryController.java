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
