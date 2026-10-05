package com.amancay.infrastructure.adapter.in.web;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.amancay.domain.exception.AddressLimitReachedException;
import com.amancay.domain.exception.AddressNotFoundException;
import com.amancay.domain.exception.AdminRequiredException;
import com.amancay.domain.exception.CategoryNotFoundException;
import com.amancay.domain.exception.DiscountNotFoundException;
import com.amancay.domain.exception.DuplicateCategoryNameException;
import com.amancay.domain.exception.DuplicateFavoriteException;
import com.amancay.domain.exception.DuplicateReviewException;
import com.amancay.domain.exception.FavoriteNotFoundException;
import com.amancay.domain.exception.InactiveUserException;
import com.amancay.domain.exception.InsufficientStockException;
import com.amancay.domain.exception.InvalidOrderStatusTransitionException;
import com.amancay.domain.exception.InvalidPaymentStatusTransitionException;
import com.amancay.domain.exception.OrderAccessDeniedException;
import com.amancay.domain.exception.OrderNotFoundException;
import com.amancay.domain.exception.PaymentNotFoundException;
import com.amancay.domain.exception.ProductNotFoundException;
import com.amancay.domain.exception.PurchaseRequiredException;
import com.amancay.domain.exception.ReviewNotFoundException;
import com.amancay.domain.exception.SelfRoleChangeException;
import com.amancay.domain.exception.UserNotFoundException;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(ProductNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(ProductNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", exception.getMessage()));
    }

    @ExceptionHandler(DiscountNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleDiscountNotFound(DiscountNotFoundException exception) {
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

    @ExceptionHandler(InactiveUserException.class)
    public ResponseEntity<Map<String, String>> handleInactiveUser(InactiveUserException exception) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Access denied"));
    }

    // Misma respuesta que AccessDeniedException: el front no distingue de donde vino el rechazo
    // (el @PreAuthorize del controller o el chequeo del caso de uso).
    @ExceptionHandler({OrderAccessDeniedException.class, AdminRequiredException.class})
    public ResponseEntity<Map<String, String>> handleDomainAccessDenied(RuntimeException exception) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Access denied"));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgument(IllegalArgumentException exception) {
        return ResponseEntity.badRequest().body(Map.of("error", exception.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException exception) {
        Map<String, String> fields = new LinkedHashMap<>();
        for (FieldError fieldError : exception.getBindingResult().getFieldErrors()) {
            fields.putIfAbsent(fieldError.getField(), fieldError.getDefaultMessage());
        }
        return ResponseEntity.badRequest().body(Map.of("error", "Validation failed", "fields", fields));
    }

    @ExceptionHandler(CategoryNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleCategoryNotFound(CategoryNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", exception.getMessage()));
    }

    @ExceptionHandler(DuplicateCategoryNameException.class)
    public ResponseEntity<Map<String, String>> handleDuplicateCategoryName(DuplicateCategoryNameException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", exception.getMessage()));
    }

    @ExceptionHandler(OrderNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleOrderNotFound(OrderNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", exception.getMessage()));
    }

    @ExceptionHandler(InvalidOrderStatusTransitionException.class)
    public ResponseEntity<Map<String, String>> handleInvalidOrderStatusTransition(InvalidOrderStatusTransitionException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", exception.getMessage()));
    }

    @ExceptionHandler(InvalidPaymentStatusTransitionException.class)
    public ResponseEntity<Map<String, String>> handleInvalidPaymentStatusTransition(InvalidPaymentStatusTransitionException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", exception.getMessage()));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, String>> handleIllegalState(IllegalStateException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", exception.getMessage()));
    }

    @ExceptionHandler(InsufficientStockException.class)
    public ResponseEntity<Map<String, String>> handleInsufficientStock(InsufficientStockException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", exception.getMessage()));
    }

    @ExceptionHandler(PaymentNotFoundException.class)
    public ResponseEntity<Map<String, String>> handlePaymentNotFound(PaymentNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", exception.getMessage()));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, String>> handleDataIntegrityViolation(DataIntegrityViolationException exception) {
        return ResponseEntity.badRequest().body(
                Map.of("error", "Invalid or inconsistent data: " + exception.getMostSpecificCause().getMessage()));
    }
}