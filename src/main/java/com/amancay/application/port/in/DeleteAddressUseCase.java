package com.amancay.application.port.in;

import java.util.UUID;

public interface DeleteAddressUseCase {
    // Borrar la predeterminada no promueve otra: el usuario elige la siguiente explicitamente.
    void delete(UUID userId, UUID addressId);
}
