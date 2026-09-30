package com.amancay.domain.model;

public record PaymentOutcome(PaymentState state, String reason) {
}