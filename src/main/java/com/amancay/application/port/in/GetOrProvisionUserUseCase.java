package com.amancay.application.port.in;

import java.util.UUID;

import com.amancay.domain.model.User;

public interface GetOrProvisionUserUseCase {
    // Da de alta al usuario autenticado la primera vez que aparece (como BUYER) y sincroniza
    // email/nombre con el token. Lanza InactiveUserException si esta desactivado.
    User getOrProvision(UUID id, String email, String name);
}
