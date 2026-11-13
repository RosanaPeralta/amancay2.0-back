package com.amancay.infrastructure.adapter.in.web.dto;

import jakarta.validation.constraints.NotBlank;

public record RejectReturnRequestRequest(
        @NotBlank(message = "La nota de rechazo es obligatoria")
        String resolutionNote) {
}