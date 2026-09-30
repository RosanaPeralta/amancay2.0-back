package com.amancay.domain.event;

import java.util.UUID;

import com.amancay.domain.model.OrderState;

public record OrderStateChanged(UUID orderId, OrderState previousState, OrderState newState) {
}