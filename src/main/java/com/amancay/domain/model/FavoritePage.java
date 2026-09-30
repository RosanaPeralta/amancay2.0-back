package com.amancay.domain.model;

import java.util.List;

public record FavoritePage(List<FavoriteProduct> content, int page, int size, long totalElements, int totalPages) {
    public FavoritePage {
        content = List.copyOf(content);
    }
}