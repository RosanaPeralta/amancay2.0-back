package com.amancay.infrastructure.adapter.in.web;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
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

import com.amancay.application.port.in.CreateDiscountUseCase;
import com.amancay.application.port.in.DeleteDiscountUseCase;
import com.amancay.application.port.in.ListDiscountsQuery;
import com.amancay.application.port.in.UpdateDiscountUseCase;
import com.amancay.infrastructure.adapter.in.web.dto.DiscountRequest;
import com.amancay.infrastructure.adapter.in.web.dto.DiscountResponse;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/discounts")
@Validated
public class DiscountController {

    private final ListDiscountsQuery listDiscountsQuery;
    private final CreateDiscountUseCase createDiscountUseCase;
    private final UpdateDiscountUseCase updateDiscountUseCase;
    private final DeleteDiscountUseCase deleteDiscountUseCase;

    public DiscountController(ListDiscountsQuery listDiscountsQuery, CreateDiscountUseCase createDiscountUseCase,
            UpdateDiscountUseCase updateDiscountUseCase, DeleteDiscountUseCase deleteDiscountUseCase) {
        this.listDiscountsQuery = listDiscountsQuery;
        this.createDiscountUseCase = createDiscountUseCase;
        this.updateDiscountUseCase = updateDiscountUseCase;
        this.deleteDiscountUseCase = deleteDiscountUseCase;
    }

    @GetMapping
    public ResponseEntity<List<DiscountResponse>> list() {
        return ResponseEntity.ok(listDiscountsQuery.listAll().stream().map(DiscountResponse::from).toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<DiscountResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(DiscountResponse.from(listDiscountsQuery.getById(id)));
    }

    @GetMapping("/search")
    public ResponseEntity<List<DiscountResponse>> search(@RequestParam String description) {
        return ResponseEntity.ok(listDiscountsQuery.searchByDescription(description).stream()
                .map(DiscountResponse::from)
                .toList());
    }

    @PostMapping
    public ResponseEntity<DiscountResponse> create(@Valid @RequestBody DiscountRequest request) {
        DiscountResponse created = DiscountResponse.from(
                createDiscountUseCase.create(request.percentage(), request.description()));
        return ResponseEntity.created(URI.create("/api/discounts/" + created.id())).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<DiscountResponse> update(@PathVariable Long id, @Valid @RequestBody DiscountRequest request) {
        return ResponseEntity.ok(DiscountResponse.from(
                updateDiscountUseCase.update(id, request.percentage(), request.description())));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        deleteDiscountUseCase.delete(id);
        return ResponseEntity.noContent().build();
    }
}
