package com.amancay.application.service;

import java.util.UUID;

import org.springframework.stereotype.Component;

import com.amancay.application.port.out.LoadRequesterPort;
import com.amancay.domain.exception.AdminRequiredException;

// Segunda barrera, ademas del @PreAuthorize del controller.
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
