package com.stormapi.scenario.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/**
 * Request DTO for reordering scenario steps.
 * Contains the step IDs in their desired new order.
 */
public record ReorderStepsRequest(
        @NotEmpty List<Long> stepIds
) {}
