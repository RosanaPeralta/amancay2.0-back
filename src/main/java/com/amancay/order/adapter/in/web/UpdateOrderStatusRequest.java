package com.amancay.order.adapter.in.web;

import com.amancay.order.domain.model.OrderStatus;

import jakarta.validation.constraints.NotNull;

public record UpdateOrderStatusRequest(@NotNull OrderStatus status) {
}
