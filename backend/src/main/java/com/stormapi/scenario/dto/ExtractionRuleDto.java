package com.stormapi.scenario.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * DTO for a single variable extraction rule within a scenario step.
 * Maps a JSONPath expression to a named variable for use in subsequent steps.
 *
 * @param variableName alphanumeric identifier (e.g., "userId", "authToken")
 * @param jsonPath     simplified JSONPath expression starting with $ (e.g., "$.data.id")
 */
public record ExtractionRuleDto(
        @NotBlank String variableName,
        @NotBlank String jsonPath
) {}
