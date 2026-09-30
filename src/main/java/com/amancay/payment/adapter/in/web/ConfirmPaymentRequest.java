package com.amancay.payment.adapter.in.web;

import com.amancay.payment.domain.model.PaymentStatus;

import jakarta.validation.constraints.NotNull;

public record ConfirmPaymentRequest(@NotNull PaymentStatus status) {
}
