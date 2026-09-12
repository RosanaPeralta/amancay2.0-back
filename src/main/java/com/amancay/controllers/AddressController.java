package com.amancay.controllers;

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

import com.amancay.dto.AddressDto;
import com.amancay.dto.CreateAddressRequest;
import com.amancay.dto.UpdateAddressRequest;
import com.amancay.security.LoggedUser;
import com.amancay.service.AddressService;

import jakarta.validation.Valid;

/** USR-05/06: direcciones del usuario autenticado. Sin paginación: son a lo sumo 10. */
@RestController
@RequestMapping("/api/me/addresses")
@Validated
public class AddressController {
    private final AddressService addressService;

    public AddressController(AddressService addressService) {
        this.addressService = addressService;
    }

    @GetMapping
    public ResponseEntity<List<AddressDto>> list(@AuthenticationPrincipal LoggedUser loggedUser) {
        return ResponseEntity.ok(addressService.list(loggedUser.id()));
    }

    @PostMapping
    public ResponseEntity<AddressDto> create(
            @AuthenticationPrincipal LoggedUser loggedUser,
            @Valid @RequestBody CreateAddressRequest request) {
        AddressDto address = addressService.create(loggedUser.id(), request);
        return ResponseEntity.created(URI.create("/api/me/addresses/" + address.id())).body(address);
    }

    @PutMapping("/{id}")
    public ResponseEntity<AddressDto> update(
            @AuthenticationPrincipal LoggedUser loggedUser,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateAddressRequest request) {
        return ResponseEntity.ok(addressService.update(loggedUser.id(), id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal LoggedUser loggedUser,
            @PathVariable UUID id) {
        addressService.delete(loggedUser.id(), id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/default")
    public ResponseEntity<AddressDto> setDefault(
            @AuthenticationPrincipal LoggedUser loggedUser,
            @PathVariable UUID id) {
        return ResponseEntity.ok(addressService.setDefault(loggedUser.id(), id));
    }
}
