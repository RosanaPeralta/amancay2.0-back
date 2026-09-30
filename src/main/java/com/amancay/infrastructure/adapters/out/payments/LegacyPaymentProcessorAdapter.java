package com.amancay.infrastructure.adapters.out.payments;

import org.springframework.stereotype.Component;

import com.amancay.domain.model.PaymentMethodType;
import com.amancay.domain.model.PaymentOutcome;
import com.amancay.domain.model.PaymentState;
import com.amancay.domain.model.PurchaseOrder;
import com.amancay.domain.ports.in.PaymentUseCases.PaymentRequest;
import com.amancay.domain.ports.out.PaymentProcessorPort;
import com.amancay.dto.CreatePaymentRequest;
import com.amancay.infrastructure.adapters.out.persistence.repository.OrderRepository;
import com.amancay.service.PaymentProcessorResolver;

@Component
public class LegacyPaymentProcessorAdapter implements PaymentProcessorPort {
    private final PaymentProcessorResolver resolver;
    private final OrderRepository orderRepository;

    public LegacyPaymentProcessorAdapter(PaymentProcessorResolver resolver, OrderRepository orderRepository) {
        this.resolver = resolver;
        this.orderRepository = orderRepository;
    }

    @Override
    public PaymentOutcome process(PaymentMethodType method, PurchaseOrder order, PaymentRequest request) {
        CreatePaymentRequest.CardData card = request.card() == null ? null
                : new CreatePaymentRequest.CardData(request.card().number(), request.card().holderName(),
                        request.card().expiry(), request.card().cvv());
        CreatePaymentRequest legacyRequest = new CreatePaymentRequest(
                com.amancay.entity.PaymentMethod.valueOf(method.name()), card);
        com.amancay.service.PaymentResult result = resolver.resolve(
            com.amancay.entity.PaymentMethod.valueOf(method.name())).process(
                orderRepository.findById(order.id()).orElseGet(com.amancay.entity.Order::new), legacyRequest);
        return new PaymentOutcome(PaymentState.valueOf(result.status().name()), result.reason());
    }
}