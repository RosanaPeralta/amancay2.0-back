package com.amancay.controllers;

import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.amancay.dto.PageResponse;
import com.amancay.dto.UpdateUserRoleRequest;
import com.amancay.dto.UserDto;
import com.amancay.security.LoggedUser;
import com.amancay.service.UserService;

import jakarta.validation.Valid;

/** USR-09: administración de usuarios. Solo ADMIN. */
@RestController
@RequestMapping("/api/admin/users")
@PreAuthorize("hasRole('ADMIN')")
@Validated
public class AdminUserController {
    private final UserService userService;

    public AdminUserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public ResponseEntity<PageResponse<UserDto>> list(
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100),
                Sort.by("createdAt").descending());
        return ResponseEntity.ok(userService.listUsers(q, pageable));
    }

    @PatchMapping("/{id}/role")
    public ResponseEntity<UserDto> changeRole(
            @AuthenticationPrincipal LoggedUser loggedUser,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateUserRoleRequest request) {
        return ResponseEntity.ok(userService.changeRole(loggedUser.id(), id, request.role()));
    }
}
