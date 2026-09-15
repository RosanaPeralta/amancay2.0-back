package com.amancay.dto;

import java.time.Instant;
import java.util.UUID;

import com.amancay.entity.Role;

public record UserDto(
        UUID id,
        String email,
        String name,
        Role role,
        Instant createdAt) {
}
