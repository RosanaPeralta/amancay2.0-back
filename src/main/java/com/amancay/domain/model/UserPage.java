package com.amancay.domain.model;

import java.util.List;

public record UserPage(List<UserProfile> content, int page, int size, long totalElements, int totalPages) {
    public UserPage {
        content = List.copyOf(content);
    }
}