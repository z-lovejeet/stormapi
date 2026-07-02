package com.stormapi.engine.assertion;

import jakarta.validation.constraints.NotBlank;

/**
 * DTO for assertion definitions received from the frontend.
 *
 * <p>Transported in API requests, then converted to {@link Assertion}
 * instances by {@link AssertionEvaluator#createAssertion}.
 *
 * @param type          assertion type (STATUS_CODE, RESPONSE_TIME, BODY_CONTAINS, JSON_PATH, HEADER)
 * @param target        what to check — JSONPath expression, header name, or null for status/time
 * @param operator      comparison operator (EQUALS, LESS_THAN, CONTAINS) — for future use
 * @param expectedValue expected value as string (e.g., "200", "500", "success", "$.data.id")
 */
public record AssertionDefinition(
        @NotBlank String type,
        String target,
        String operator,
        @NotBlank String expectedValue
) {
}
