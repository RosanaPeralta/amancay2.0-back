package com.amancay.domain.ports.out;

import java.util.UUID;

public interface AddressUserLockPort {
    void lockExistingUser(UUID userId);
}