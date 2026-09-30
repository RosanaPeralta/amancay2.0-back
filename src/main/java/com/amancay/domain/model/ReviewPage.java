package com.amancay.domain.model;

import java.util.List;

public record ReviewPage(List<ProductReview> content, int page, int size, long totalElements, int totalPages) {
    public ReviewPage {
        content = List.copyOf(content);
    }
}