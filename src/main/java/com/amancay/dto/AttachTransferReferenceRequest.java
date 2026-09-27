package com.amancay.dto;

import jakarta.validation.constraints.NotBlank;

public record AttachTransferReferenceRequest(@NotBlank String transferReference) {
}
