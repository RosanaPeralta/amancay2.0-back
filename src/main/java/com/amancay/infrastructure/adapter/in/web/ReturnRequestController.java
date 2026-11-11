
package com.amancay.infrastructure.adapter.in.web;

import java.net.URI;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.amancay.application.port.in.CreateReturnRequestUseCase;
import com.amancay.domain.model.ReturnRequest;
import com.amancay.infrastructure.adapter.in.web.dto.CreateReturnRequestRequest;
import com.amancay.infrastructure.adapter.in.web.dto.ReturnRequestResponse;
import com.amancay.infrastructure.security.LoggedUser;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/orders/{orderId}/return-requests")
@Validated
public class ReturnRequestController {

    private final CreateReturnRequestUseCase createReturnRequestUseCase;

    public ReturnRequestController(
            CreateReturnRequestUseCase createReturnRequestUseCase) {
        this.createReturnRequestUseCase = createReturnRequestUseCase;
    }

    @PostMapping
    public ResponseEntity<ReturnRequestResponse> create(
            @AuthenticationPrincipal LoggedUser loggedUser,
            @PathVariable UUID orderId,
            @Valid @RequestBody CreateReturnRequestRequest request) {

        ReturnRequest returnRequest = createReturnRequestUseCase.create(
                loggedUser.id(),
                orderId,
                request.reason(),
                request.comment());

        ReturnRequestResponse response = ReturnRequestResponse.from(returnRequest);

        return ResponseEntity
                .created(URI.create("/api/orders/" + orderId + "/return-requests/" + response.id()))
                .body(response);
    }
}