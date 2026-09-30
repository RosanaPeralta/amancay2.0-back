package com.amancay.domain.model;

import java.util.List;

public record ProductPage(List<ProductSummary> content, int page, int size, long totalElements, int totalPages) {
    public ProductPage {
        content = List.copyOf(content);
    }
}