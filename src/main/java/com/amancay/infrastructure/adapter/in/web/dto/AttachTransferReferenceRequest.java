package com.amancay.infrastructure.adapter.in.web.dto;

import jakarta.validation.constraints.NotBlank;

public record AttachTransferReferenceRequest(@NotBlank String transferReference) {
}
