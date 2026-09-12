package com.amancay.controllers;

import java.util.Map;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.amancay.exceptions.AddressLimitReachedException;
import com.amancay.exceptions.AddressNotFoundException;
import com.amancay.exceptions.CategoryNotFoundException;
import com.amancay.exceptions.DuplicateCategoryNameException;
import com.amancay.exceptions.DuplicateFavoriteException;
import com.amancay.exceptions.DuplicateReviewException;
import com.amancay.exceptions.FavoriteNotFoundException;
import com.amancay.exceptions.ProductNotFoundException;
import com.amancay.exceptions.PurchaseRequiredException;
import com.amancay.exceptions.ReviewNotFoundException;
import com.amancay.exceptions.SelfRoleChangeException;
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

    @ExceptionHandler(AddressNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleAddressNotFound(AddressNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", exception.getMessage()));
    }

    @ExceptionHandler(AddressLimitReachedException.class)
    public ResponseEntity<Map<String, String>> handleAddressLimitReached(AddressLimitReachedException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", exception.getMessage()));
    }

    @ExceptionHandler(FavoriteNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleFavoriteNotFound(FavoriteNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", exception.getMessage()));
    }

    @ExceptionHandler(DuplicateFavoriteException.class)
    public ResponseEntity<Map<String, String>> handleDuplicateFavorite(DuplicateFavoriteException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", exception.getMessage()));
    }

    @ExceptionHandler(SelfRoleChangeException.class)
    public ResponseEntity<Map<String, String>> handleSelfRoleChange(SelfRoleChangeException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", exception.getMessage()));
    }

    /** {@code @PreAuthorize} rechazado (BUYER contra un endpoint ADMIN): mismo formato que el resto. */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, String>> handleAccessDenied(AccessDeniedException exception) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Access denied"));
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

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, String>> handleDataIntegrityViolation(DataIntegrityViolationException exception) {
        return ResponseEntity.badRequest().body(
                Map.of("error", "Invalid or inconsistent data: " + exception.getMostSpecificCause().getMessage()));
    }
}