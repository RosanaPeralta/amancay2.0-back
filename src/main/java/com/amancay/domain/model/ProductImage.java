package com.amancay.domain.model;

import java.util.UUID;

public class ProductImage {

    private final UUID id;
    private String imageUrl;

    public ProductImage(UUID id, String imageUrl) {
        this.id = id;
        this.imageUrl = imageUrl;
    }

    void changeUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public UUID getId() {
        return id;
    }

    public String getImageUrl() {
        return imageUrl;
    }
}
