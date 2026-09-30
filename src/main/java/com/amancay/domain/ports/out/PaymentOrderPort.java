package com.amancay.domain.ports.out;

import java.util.UUID;

import com.amancay.domain.model.PurchaseOrder;
import com.amancay.domain.model.OrderState;

public interface PaymentOrderPort {
    void authorize(UUID requesterId, UUID orderId);

    PurchaseOrder getOrder(UUID orderId);

    void changeStatus(UUID orderId, OrderState status);
}