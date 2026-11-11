
package com.amancay.infrastructure.adapter.in.web;

import java.util.List;
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
import org.springframework.web.bind.annotation.RestController;

import com.amancay.application.port.in.ApproveReturnRequestUseCase;
import com.amancay.application.port.in.ListReturnRequestsUseCase;
import com.amancay.application.port.in.RejectReturnRequestUseCase;
import com.amancay.domain.model.ReturnRequest;
import com.amancay.infrastructure.adapter.in.web.dto.RejectReturnRequestRequest;
import com.amancay.infrastructure.adapter.in.web.dto.ReturnRequestResponse;
import com.amancay.infrastructure.security.LoggedUser;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/admin/return-requests")
@Validated
@PreAuthorize("hasRole('ADMIN')")
public class AdminReturnRequestController {

    private final ListReturnRequestsUseCase listReturnRequestsUseCase;
    private final ApproveReturnRequestUseCase approveReturnRequestUseCase;
    private final RejectReturnRequestUseCase rejectReturnRequestUseCase;

    public AdminReturnRequestController(
            ListReturnRequestsUseCase listReturnRequestsUseCase,
            ApproveReturnRequestUseCase approveReturnRequestUseCase,
            RejectReturnRequestUseCase rejectReturnRequestUseCase) {
        this.listReturnRequestsUseCase = listReturnRequestsUseCase;
        this.approveReturnRequestUseCase = approveReturnRequestUseCase;
        this.rejectReturnRequestUseCase = rejectReturnRequestUseCase;
    }

    @GetMapping
    public ResponseEntity<List<ReturnRequestResponse>> listPending(
            @AuthenticationPrincipal LoggedUser loggedUser) {

        List<ReturnRequest> requests =
                listReturnRequestsUseCase.listPending(loggedUser.id());

        return ResponseEntity.ok(
                requests.stream()
                        .map(ReturnRequestResponse::from)
                        .toList());
    }

    @PatchMapping("/{id}/approve")
    public ResponseEntity<ReturnRequestResponse> approve(
            @AuthenticationPrincipal LoggedUser loggedUser,
            @PathVariable UUID id) {

        ReturnRequest request =
                approveReturnRequestUseCase.approve(loggedUser.id(), id);

        return ResponseEntity.ok(ReturnRequestResponse.from(request));
    }

    @PatchMapping("/{id}/reject")
    public ResponseEntity<ReturnRequestResponse> reject(
            @AuthenticationPrincipal LoggedUser loggedUser,
            @PathVariable UUID id,
            @Valid @RequestBody RejectReturnRequestRequest request) {

        ReturnRequest rejected =
                rejectReturnRequestUseCase.reject(
                        loggedUser.id(),
                        id,
                        request.resolutionNote());

        return ResponseEntity.ok(ReturnRequestResponse.from(rejected));
    }
}