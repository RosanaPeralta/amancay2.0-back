package com.amancay.controllers;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.amancay.dto.DiscountRequest;
import com.amancay.dto.DiscountResponse;
import com.amancay.entity.Discount;
import com.amancay.service.DiscountService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/discounts")
@Validated
public class DiscountController {
    private final DiscountService discountService;

    public DiscountController(DiscountService discountService) {
        this.discountService = discountService;
    }

    @GetMapping
    public ResponseEntity<List<DiscountResponse>> list() {
        return ResponseEntity.ok(discountService.list().stream()
                .map(this::toResponse)
                .toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<DiscountResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(toResponse(discountService.getById(id)));
    }

    @GetMapping("/search")
    public ResponseEntity<List<DiscountResponse>> search(@RequestParam String description) {
        return ResponseEntity.ok(discountService.searchByDescription(description).stream()
                .map(this::toResponse)
                .toList());
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<DiscountResponse> create(@Valid @RequestBody DiscountRequest request) {
        Discount created = discountService.create(request);
        return ResponseEntity.created(URI.create("/api/discounts/" + created.getId())).body(toResponse(created));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<DiscountResponse> update(@PathVariable Long id, @Valid @RequestBody DiscountRequest request) {
        return ResponseEntity.ok(toResponse(discountService.update(id, request)));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        discountService.delete(id);
        return ResponseEntity.noContent().build();
    }

    private DiscountResponse toResponse(Discount discount) {
        return new DiscountResponse(discount.getId(), discount.getPercentage(), discount.getDescription());
    }
}
