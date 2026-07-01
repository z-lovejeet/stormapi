package com.stormapi.scenario.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for updating an existing test scenario's metadata.
 */
public record UpdateScenarioRequest(
        @NotBlank @Size(max = 255) String name,
        @Size(max = 1000) String description,
        Boolean failFast
) {
    public boolean isFailFast() {
        return failFast == null || failFast;
    }
}
