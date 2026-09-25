package com.amancay.dto;

import com.amancay.entity.PaymentStatus;

import jakarta.validation.constraints.NotNull;

// Solo tiene sentido confirmar como APROBADO o RECHAZADO; PENDIENTE se valida
// aparte en el service porque no es una decision valida para este endpoint.
public record ConfirmPaymentRequest(@NotNull PaymentStatus status) {
}
