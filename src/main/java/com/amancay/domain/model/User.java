package com.amancay.domain.model;

import java.time.Instant;
import java.util.UUID;

public class User {

    private final UUID id;
    private String email;
    private String name;
    private Role role;
    private final boolean active;
    private final Instant createdAt;
    private final Instant updatedAt;

    public User(UUID id, String email, String name, Role role, boolean active, Instant createdAt,
            Instant updatedAt) {
        this.id = id;
        this.email = email;
        this.name = name;
        this.role = role;
        this.active = active;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public boolean isAdmin() {
        return role == Role.ADMIN;
    }

    // Los datos del token mandan sobre el email; el nombre solo se completa si el usuario
    // nunca cargo uno (despues lo maneja el con su perfil). Devuelve si algo cambio.
    public boolean syncWithToken(String tokenEmail, String tokenName) {
        boolean changed = false;
        if (tokenEmail != null && !tokenEmail.equals(email)) {
            this.email = tokenEmail;
            changed = true;
        }
        if (tokenName != null && name == null) {
            this.name = tokenName;
            changed = true;
        }
        return changed;
    }

    public void rename(String name) {
        this.name = name;
    }

    public void changeRole(Role role) {
        this.role = role;
    }

    public UUID getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getName() {
        return name;
    }

    public Role getRole() {
        return role;
    }

    public boolean isActive() {
        return active;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
