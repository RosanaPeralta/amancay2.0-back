package com.amancay.controllers;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.amancay.dto.AddFavoriteRequest;
import com.amancay.dto.FavoriteDto;
import com.amancay.dto.PageResponse;
import com.amancay.security.LoggedUser;
import com.amancay.service.FavoriteService;

import jakarta.validation.Valid;

/** USR-08: favoritos del usuario autenticado. El recurso se identifica por {@code productId}. */
@RestController
@RequestMapping("/api/me/favorites")
@Validated
public class FavoriteController {
    private final FavoriteService favoriteService;

    public FavoriteController(FavoriteService favoriteService) {
        this.favoriteService = favoriteService;
    }

    @GetMapping
    public ResponseEntity<PageResponse<FavoriteDto>> list(
            @AuthenticationPrincipal LoggedUser loggedUser,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100),
                Sort.by("createdAt").descending());
        return ResponseEntity.ok(favoriteService.list(loggedUser.id(), pageable));
    }

    @GetMapping("/ids")
    public ResponseEntity<List<UUID>> listIds(@AuthenticationPrincipal LoggedUser loggedUser) {
        return ResponseEntity.ok(favoriteService.listProductIds(loggedUser.id()));
    }

    @PostMapping
    public ResponseEntity<FavoriteDto> add(
            @AuthenticationPrincipal LoggedUser loggedUser,
            @Valid @RequestBody AddFavoriteRequest request) {
        FavoriteDto favorite = favoriteService.add(loggedUser.id(), request.productId());
        return ResponseEntity.created(URI.create("/api/me/favorites/" + favorite.productId())).body(favorite);
    }

    @DeleteMapping("/{productId}")
    public ResponseEntity<Void> remove(
            @AuthenticationPrincipal LoggedUser loggedUser,
            @PathVariable UUID productId) {
        favoriteService.remove(loggedUser.id(), productId);
        return ResponseEntity.noContent().build();
    }
}
