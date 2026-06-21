package com.stormapi.collection.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for creating a new API collection.
 */
public record CreateCollectionRequest(
        @NotBlank @Size(max = 255) String name,
        @Size(max = 1000) String description
) {}
