package com.amancay.security;

import java.util.UUID;

public record LoggedUser(UUID id, String email) {
}
