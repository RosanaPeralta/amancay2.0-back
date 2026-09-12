package com.amancay.dto;

import com.amancay.entity.Role;

import jakarta.validation.constraints.NotNull;

public record UpdateUserRoleRequest(
        @NotNull Role role) {
}
