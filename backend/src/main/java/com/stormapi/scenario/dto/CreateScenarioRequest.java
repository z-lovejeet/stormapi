package com.stormapi.scenario.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for creating a new test scenario.
 */
public record CreateScenarioRequest(
        @NotBlank @Size(max = 255) String name,
        @Size(max = 1000) String description,
        Boolean failFast
) {
    /**
     * Defaults failFast to true if not provided.
     */
    public boolean isFailFast() {
        return failFast == null || failFast;
    }
}
