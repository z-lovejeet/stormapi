package com.stormapi.scenario.dto;

import com.stormapi.collection.dto.KeyValuePairDto;
import com.stormapi.test.model.HttpMethod;

import java.util.List;

/**
 * Response DTO for a single scenario step.
 */
public record StepResponse(
        Long id,
        int stepOrder,
        String name,
        String url,
        HttpMethod method,
        List<KeyValuePairDto> headers,
        String body,
        List<ExtractionRuleDto> extractionRules
) {}
