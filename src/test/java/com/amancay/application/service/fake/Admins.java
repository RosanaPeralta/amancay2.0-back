package com.amancay.application.service.fake;

import java.util.UUID;

import com.amancay.application.port.out.LoadRequesterPort.Requester;
import com.amancay.application.service.AdminGuard;

// Quien pide las acciones de administracion en los tests: ADMIN_ID es admin, cualquier
// otro id es un comprador.
public final class Admins {

    public static final UUID ADMIN_ID = UUID.fromString("00000000-0000-0000-0000-0000000000ad");

    private Admins() {
    }

    public static AdminGuard guard() {
        return new AdminGuard(id -> new Requester(id, ADMIN_ID.equals(id)));
    }
}
