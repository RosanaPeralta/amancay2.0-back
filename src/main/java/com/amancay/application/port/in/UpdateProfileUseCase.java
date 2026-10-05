package com.amancay.application.port.in;

import java.util.UUID;

import com.amancay.domain.model.User;

public interface UpdateProfileUseCase {
    User updateName(UUID id, String name);
}
