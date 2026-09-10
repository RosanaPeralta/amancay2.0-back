package com.amancay.controllers;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.amancay.exceptions.CategoryNotFoundException;
import com.amancay.exceptions.DuplicateCategoryNameException;
import com.amancay.exceptions.DuplicateSlugException;
import com.amancay.exceptions.DuplicateReviewException;
import com.amancay.exceptions.ProductNotFoundException;
import com.amancay.exceptions.PurchaseRequiredException;
import com.amancay.exceptions.ReviewNotFoundException;
import com.amancay.exceptions.UserNotFoundException;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(ProductNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(ProductNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", exception.getMessage()));
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleUserNotFound(UserNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", exception.getMessage()));
    }

    @ExceptionHandler(ReviewNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleReviewNotFound(ReviewNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", exception.getMessage()));
    }

    @ExceptionHandler(DuplicateReviewException.class)
    public ResponseEntity<Map<String, String>> handleDuplicateReview(DuplicateReviewException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", exception.getMessage()));
    }

    @ExceptionHandler(PurchaseRequiredException.class)
    public ResponseEntity<Map<String, String>> handlePurchaseRequired(PurchaseRequiredException exception) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", exception.getMessage()));
    }

    @ExceptionHandler(DuplicateSlugException.class)
    public ResponseEntity<Map<String, String>> handleDuplicateSlug(DuplicateSlugException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", exception.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgument(IllegalArgumentException exception) {
        return ResponseEntity.badRequest().body(Map.of("error", exception.getMessage()));
    }

    @ExceptionHandler(CategoryNotFoundException.class)
    public ResponseEntity<String> handleCategoryNotFound(CategoryNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
    }

    @ExceptionHandler(DuplicateCategoryNameException.class)
    public ResponseEntity<String> handleDuplicateCategoryName(DuplicateCategoryNameException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ex.getMessage());
    }
}