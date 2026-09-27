package com.amancay.dto;

import com.amancay.entity.PaymentStatus;

import jakarta.validation.constraints.NotNull;

public record ConfirmPaymentRequest(@NotNull PaymentStatus status) {
}
