package com.amancay.infrastructure.adapter.in.web;

import java.util.UUID;

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

import com.amancay.application.port.in.ChangeUserRoleUseCase;
import com.amancay.application.port.in.ListUsersQuery;
import com.amancay.domain.model.PageQuery;
import com.amancay.infrastructure.adapter.in.web.dto.PageResponse;
import com.amancay.infrastructure.adapter.in.web.dto.UpdateUserRoleRequest;
import com.amancay.infrastructure.adapter.in.web.dto.UserResponse;
import com.amancay.infrastructure.security.LoggedUser;

import jakarta.validation.Valid;

/** Administración de usuarios. Solo ADMIN. */
@RestController
@RequestMapping("/api/admin/users")
@PreAuthorize("hasRole('ADMIN')")
@Validated
public class AdminUserController {

    private final ListUsersQuery listUsersQuery;
    private final ChangeUserRoleUseCase changeUserRoleUseCase;

    public AdminUserController(ListUsersQuery listUsersQuery, ChangeUserRoleUseCase changeUserRoleUseCase) {
        this.listUsersQuery = listUsersQuery;
        this.changeUserRoleUseCase = changeUserRoleUseCase;
    }

    @GetMapping
    public ResponseEntity<PageResponse<UserResponse>> list(
            @AuthenticationPrincipal LoggedUser loggedUser,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(PageResponse.from(listUsersQuery.list(loggedUser.id(), q, new PageQuery(page, size)), UserResponse::from));
    }

    @PatchMapping("/{id}/role")
    public ResponseEntity<UserResponse> changeRole(
            @AuthenticationPrincipal LoggedUser loggedUser,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateUserRoleRequest request) {
        return ResponseEntity.ok(UserResponse.from(changeUserRoleUseCase.changeRole(loggedUser.id(), id, request.role())));
    }
}
