package com.amancay.infrastructure.adapter.in.web.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;

public record AddFavoriteRequest(
        @NotNull UUID productId) {
}
