package com.amancay.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.amancay.dto.UpdateUserRequest;
import com.amancay.dto.UserDto;
import com.amancay.security.LoggedUser;
import com.amancay.service.UserService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/me")
@Validated
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public ResponseEntity<UserDto> me(@AuthenticationPrincipal LoggedUser loggedUser) {
        return ResponseEntity.ok(userService.getOrProvision(loggedUser.id(), loggedUser.email(), null));
    }

    @PatchMapping
    public ResponseEntity<UserDto> updateMe(@AuthenticationPrincipal LoggedUser loggedUser,
            @Valid @RequestBody UpdateUserRequest request) {
        return ResponseEntity.ok(userService.updateProfile(loggedUser.id(), request));
    }
}
