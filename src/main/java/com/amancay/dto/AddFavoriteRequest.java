package com.amancay.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;

public record AddFavoriteRequest(
        @NotNull UUID productId) {
}
