package com.amancay.application.port.in;

import java.util.List;
import java.util.UUID;

import com.amancay.domain.model.Product;

// discountId en null deja el descuento como esta (para quitarlo esta removeDiscount).
public record UpdateProductCommand(String name, String shortDescription, String description, boolean active,
        List<Product.VariantChange> variants, List<Product.ImageChange> images, List<UUID> categoryIds,
        Long discountId) {
}
