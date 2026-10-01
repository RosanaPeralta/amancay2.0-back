package com.amancay.infrastructure.adapter.in.web;

import jakarta.validation.constraints.NotBlank;

public record AttachTransferReferenceRequest(@NotBlank String transferReference) {
}
