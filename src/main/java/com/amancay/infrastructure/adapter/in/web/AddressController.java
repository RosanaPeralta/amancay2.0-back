package com.amancay.infrastructure.adapter.in.web;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.amancay.application.port.in.CreateAddressUseCase;
import com.amancay.application.port.in.DeleteAddressUseCase;
import com.amancay.application.port.in.ListAddressesQuery;
import com.amancay.application.port.in.SetDefaultAddressUseCase;
import com.amancay.application.port.in.UpdateAddressUseCase;
import com.amancay.infrastructure.adapter.in.web.dto.AddressResponse;
import com.amancay.infrastructure.adapter.in.web.dto.CreateAddressRequest;
import com.amancay.infrastructure.adapter.in.web.dto.UpdateAddressRequest;
import com.amancay.infrastructure.security.LoggedUser;

import jakarta.validation.Valid;

/** Direcciones del usuario autenticado. Sin paginación: son a lo sumo 10. */
@RestController
@RequestMapping("/api/me/addresses")
@Validated
public class AddressController {

    private final ListAddressesQuery listAddressesQuery;
    private final CreateAddressUseCase createAddressUseCase;
    private final UpdateAddressUseCase updateAddressUseCase;
    private final DeleteAddressUseCase deleteAddressUseCase;
    private final SetDefaultAddressUseCase setDefaultAddressUseCase;

    public AddressController(ListAddressesQuery listAddressesQuery, CreateAddressUseCase createAddressUseCase,
            UpdateAddressUseCase updateAddressUseCase, DeleteAddressUseCase deleteAddressUseCase,
            SetDefaultAddressUseCase setDefaultAddressUseCase) {
        this.listAddressesQuery = listAddressesQuery;
        this.createAddressUseCase = createAddressUseCase;
        this.updateAddressUseCase = updateAddressUseCase;
        this.deleteAddressUseCase = deleteAddressUseCase;
        this.setDefaultAddressUseCase = setDefaultAddressUseCase;
    }

    @GetMapping
    public ResponseEntity<List<AddressResponse>> list(@AuthenticationPrincipal LoggedUser loggedUser) {
        return ResponseEntity.ok(listAddressesQuery.list(loggedUser.id()).stream().map(AddressResponse::from).toList());
    }

    @PostMapping
    public ResponseEntity<AddressResponse> create(
            @AuthenticationPrincipal LoggedUser loggedUser,
            @Valid @RequestBody CreateAddressRequest request) {
        AddressResponse address = AddressResponse.from(createAddressUseCase.create(loggedUser.id(), request.toData()));
        return ResponseEntity.created(URI.create("/api/me/addresses/" + address.id())).body(address);
    }

    @PutMapping("/{id}")
    public ResponseEntity<AddressResponse> update(
            @AuthenticationPrincipal LoggedUser loggedUser,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateAddressRequest request) {
        return ResponseEntity.ok(AddressResponse.from(updateAddressUseCase.update(loggedUser.id(), id, request.toData())));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal LoggedUser loggedUser,
            @PathVariable UUID id) {
        deleteAddressUseCase.delete(loggedUser.id(), id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/default")
    public ResponseEntity<AddressResponse> setDefault(
            @AuthenticationPrincipal LoggedUser loggedUser,
            @PathVariable UUID id) {
        return ResponseEntity.ok(AddressResponse.from(setDefaultAddressUseCase.setDefault(loggedUser.id(), id)));
    }
}
