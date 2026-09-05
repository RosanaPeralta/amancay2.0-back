package com.amancay.exceptions;

public class DuplicateSlugException extends RuntimeException {
    public DuplicateSlugException(String slug) {
        super("A product with slug '" + slug + "' already exists");
    }
}