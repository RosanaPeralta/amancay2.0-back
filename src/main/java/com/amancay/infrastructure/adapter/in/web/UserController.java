package com.amancay.infrastructure.adapter.in.web;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.amancay.application.port.in.GetOrProvisionUserUseCase;
import com.amancay.application.port.in.UpdateProfileUseCase;
import com.amancay.infrastructure.adapter.in.web.dto.UpdateUserRequest;
import com.amancay.infrastructure.adapter.in.web.dto.UserResponse;
import com.amancay.infrastructure.security.LoggedUser;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/me")
@Validated
public class UserController {

    private final GetOrProvisionUserUseCase getOrProvisionUserUseCase;
    private final UpdateProfileUseCase updateProfileUseCase;

    public UserController(GetOrProvisionUserUseCase getOrProvisionUserUseCase,
            UpdateProfileUseCase updateProfileUseCase) {
        this.getOrProvisionUserUseCase = getOrProvisionUserUseCase;
        this.updateProfileUseCase = updateProfileUseCase;
    }

    @GetMapping
    public ResponseEntity<UserResponse> me(@AuthenticationPrincipal LoggedUser loggedUser) {
        return ResponseEntity.ok(UserResponse.from(
                getOrProvisionUserUseCase.getOrProvision(loggedUser.id(), loggedUser.email(), loggedUser.name())));
    }

    @PatchMapping
    public ResponseEntity<UserResponse> updateMe(@AuthenticationPrincipal LoggedUser loggedUser,
            @Valid @RequestBody UpdateUserRequest request) {
        return ResponseEntity.ok(UserResponse.from(updateProfileUseCase.updateName(loggedUser.id(), request.name())));
    }
}
