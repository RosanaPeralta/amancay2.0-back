package com.amancay.application.service;

import java.util.UUID;

import org.springframework.stereotype.Component;

import com.amancay.application.port.out.LoadRequesterPort;
import com.amancay.domain.exception.AdminRequiredException;

// Segunda barrera para los casos de uso de administracion: el controller ya exige
// @PreAuthorize("hasRole('ADMIN')"), pero el caso de uso lo vuelve a verificar para que
// un adaptador de entrada nuevo que se olvide la anotacion no lo deje abierto.
@Component
public class AdminGuard {

    private final LoadRequesterPort loadRequesterPort;

    public AdminGuard(LoadRequesterPort loadRequesterPort) {
        this.loadRequesterPort = loadRequesterPort;
    }

    public void requireAdmin(UUID requesterId) {
        if (requesterId == null || !loadRequesterPort.load(requesterId).admin()) {
            throw new AdminRequiredException();
        }
    }
}
