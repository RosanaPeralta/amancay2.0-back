package com.amancay.payment.adapter.in.web;

import jakarta.validation.constraints.NotBlank;

public record AttachTransferReferenceRequest(@NotBlank String transferReference) {
}
