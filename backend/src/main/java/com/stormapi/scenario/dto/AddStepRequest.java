package com.stormapi.scenario.dto;

import com.stormapi.collection.dto.KeyValuePairDto;
import com.stormapi.test.model.HttpMethod;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Request DTO for adding a step to a test scenario.
 */
public record AddStepRequest(
        @NotBlank @Size(max = 255) String name,
        @NotBlank @Size(max = 2048) String url,
        @NotNull HttpMethod method,
        List<KeyValuePairDto> headers,
        String body,
        List<ExtractionRuleDto> extractionRules
) {}
