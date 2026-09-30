package com.amancay.dto;

import java.util.UUID;

public record CategoryDto(
        UUID id,
        String name) {
}