package com.amancay.controllers;

public class FavoriteController extends com.amancay.infrastructure.adapters.in.web.FavoriteController {
    public FavoriteController(com.amancay.service.FavoriteService favoriteService) {
        super(favoriteService);
    }
}
