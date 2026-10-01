package com.amancay.infrastructure.adapter.in.web;

import com.amancay.domain.model.OrderStatus;

import jakarta.validation.constraints.NotNull;

public record UpdateOrderStatusRequest(@NotNull OrderStatus status) {
}
