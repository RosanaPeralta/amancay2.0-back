package com.amancay.infrastructure.adapter.in.web.dto;

import com.amancay.domain.model.PaymentStatus;

import jakarta.validation.constraints.NotNull;

public record ConfirmPaymentRequest(@NotNull PaymentStatus status) {
}
