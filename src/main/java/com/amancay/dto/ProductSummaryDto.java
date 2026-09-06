package com.amancay.dto;

import java.util.UUID;

public record ProductSummaryDto(UUID id, String name, String slug, boolean active) {
}
