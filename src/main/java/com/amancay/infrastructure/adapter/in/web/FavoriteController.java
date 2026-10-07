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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.amancay.application.port.in.CreateFavoriteUseCase;
import com.amancay.application.port.in.ListFavoritesQuery;
import com.amancay.application.port.in.DeleteFavoriteUseCase;
import com.amancay.domain.model.PageQuery;
import com.amancay.infrastructure.adapter.in.web.dto.CreateFavoriteRequest;
import com.amancay.infrastructure.adapter.in.web.dto.FavoriteResponse;
import com.amancay.infrastructure.adapter.in.web.dto.PageResponse;
import com.amancay.infrastructure.security.LoggedUser;

import jakarta.validation.Valid;

/** Favoritos del usuario autenticado. El recurso se identifica por {@code productId}. */
@RestController
@RequestMapping("/api/me/favorites")
@Validated
public class FavoriteController {

    private final ListFavoritesQuery listFavoritesQuery;
    private final CreateFavoriteUseCase createFavoriteUseCase;
    private final DeleteFavoriteUseCase deleteFavoriteUseCase;

    public FavoriteController(ListFavoritesQuery listFavoritesQuery, CreateFavoriteUseCase createFavoriteUseCase,
            DeleteFavoriteUseCase deleteFavoriteUseCase) {
        this.listFavoritesQuery = listFavoritesQuery;
        this.createFavoriteUseCase = createFavoriteUseCase;
        this.deleteFavoriteUseCase = deleteFavoriteUseCase;
    }

    @GetMapping
    public ResponseEntity<PageResponse<FavoriteResponse>> list(
            @AuthenticationPrincipal LoggedUser loggedUser,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(PageResponse.from(listFavoritesQuery.list(loggedUser.id(), new PageQuery(page, size)),
                FavoriteResponse::from));
    }

    @GetMapping("/ids")
    public ResponseEntity<List<UUID>> listIds(@AuthenticationPrincipal LoggedUser loggedUser) {
        return ResponseEntity.ok(listFavoritesQuery.listProductIds(loggedUser.id()));
    }

    @PostMapping
    public ResponseEntity<FavoriteResponse> create(
            @AuthenticationPrincipal LoggedUser loggedUser,
            @Valid @RequestBody CreateFavoriteRequest request) {
        FavoriteResponse favorite = FavoriteResponse.from(createFavoriteUseCase.create(loggedUser.id(), request.productId()));
        return ResponseEntity.created(URI.create("/api/me/favorites/" + favorite.productId())).body(favorite);
    }

    @DeleteMapping("/{productId}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal LoggedUser loggedUser,
            @PathVariable UUID productId) {
        deleteFavoriteUseCase.delete(loggedUser.id(), productId);
        return ResponseEntity.noContent().build();
    }
}
