package com.amancay.domain.model;

import java.util.UUID;

import lombok.Getter;

@Getter
public class Category {

    private final UUID id;
    private String name;

    public Category(UUID id, String name) {
        this.id = id;
        this.name = name;
    }

    public static Category create(String name) {
        return new Category(null, name);
    }

    public void rename(String name) {
        this.name = name;
    }
}
