package com.stormapi.scenario.dto;

import java.time.Instant;
import java.util.List;

/**
 * Response DTO for a full test scenario with all steps.
 */
public record ScenarioResponse(
        Long id,
        String name,
        String description,
        boolean failFast,
        List<StepResponse> steps,
        Instant createdAt,
        Instant updatedAt
) {}
