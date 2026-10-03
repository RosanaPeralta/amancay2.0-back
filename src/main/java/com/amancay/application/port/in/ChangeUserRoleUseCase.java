package com.amancay.application.port.in;

import java.util.UUID;

import com.amancay.domain.model.Role;
import com.amancay.domain.model.User;

public interface ChangeUserRoleUseCase {
    // Un ADMIN no puede quitarse su propio rol: evita dejar el sistema sin administradores.
    User changeRole(UUID adminId, UUID targetId, Role role);
}
