package com.amancay.infrastructure.security;

import java.util.UUID;

public record LoggedUser(UUID id, String email, String name) {
}
