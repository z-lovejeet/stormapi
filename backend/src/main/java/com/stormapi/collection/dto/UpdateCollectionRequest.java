package com.stormapi.collection.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for updating an API collection.
 */
public record UpdateCollectionRequest(
        @NotBlank @Size(max = 255) String name,
        @Size(max = 1000) String description
) {}
