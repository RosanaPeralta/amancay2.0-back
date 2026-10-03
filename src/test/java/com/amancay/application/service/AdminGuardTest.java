package com.amancay.application.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.amancay.application.port.out.LoadRequesterPort.Requester;
import com.amancay.domain.exception.AdminRequiredException;
import com.amancay.domain.exception.UserNotFoundException;

class AdminGuardTest {

    private final UUID adminId = UUID.randomUUID();
    private final UUID buyerId = UUID.randomUUID();
    private final AdminGuard guard = new AdminGuard(id -> {
        if (id.equals(adminId) || id.equals(buyerId)) {
            return new Requester(id, id.equals(adminId));
        }
        throw new UserNotFoundException(id);
    });

    @Test
    void letsAnAdminThrough() {
        assertThatCode(() -> guard.requireAdmin(adminId)).doesNotThrowAnyException();
    }

    @Test
    void rejectsABuyer() {
        assertThatThrownBy(() -> guard.requireAdmin(buyerId)).isInstanceOf(AdminRequiredException.class);
    }

    @Test
    void rejectsAMissingRequesterWithoutLookingItUp() {
        assertThatThrownBy(() -> guard.requireAdmin(null)).isInstanceOf(AdminRequiredException.class);
    }
}
