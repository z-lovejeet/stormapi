package com.stormapi.engine.assertion;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Orchestrates assertion evaluation against HTTP responses.
 *
 * <p>Evaluates a list of {@link Assertion} instances against a
 * {@link AssertionContext} and returns a list of results. Each assertion
 * is evaluated independently — a failure or exception in one does not
 * prevent others from running.
 *
 * <p>Also provides a factory method to create {@link Assertion} instances
 * from {@link AssertionDefinition} DTOs received from the frontend.
 */
@Service
public class AssertionEvaluator {

    private static final Logger log = LoggerFactory.getLogger(AssertionEvaluator.class);

    /**
     * Evaluates all assertions against the given context.
     *
     * @param assertions list of assertions to evaluate (may be null or empty)
     * @param context    HTTP response data to evaluate against
     * @return list of results, one per assertion (never null)
     */
    public List<AssertionResult> evaluate(List<Assertion> assertions, AssertionContext context) {
        if (assertions == null || assertions.isEmpty()) {
            return List.of();
        }

        List<AssertionResult> results = new ArrayList<>(assertions.size());

        for (Assertion assertion : assertions) {
            try {
                AssertionResult result = assertion.evaluate(context);
                results.add(result);

                if (!result.passed()) {
                    log.debug("Assertion [{}] failed: {}", assertion.getType(), result.message());
                }
            } catch (Exception e) {
                log.warn("Assertion [{}] threw exception: {}", assertion.getType(), e.getMessage());
                results.add(AssertionResult.error(assertion.getType(), e.getMessage()));
            }
        }

        return results;
    }

    /**
     * Creates an {@link Assertion} instance from a definition DTO.
     *
     * @param def the assertion definition from the frontend
     * @return a concrete assertion instance
     * @throws IllegalArgumentException if the type is unknown or values are invalid
     */
    public static Assertion createAssertion(AssertionDefinition def) {
        return switch (def.type().toUpperCase()) {
            case "STATUS_CODE" -> new StatusCodeAssertion(
                    parseIntSafe(def.expectedValue(), "STATUS_CODE expected value"));
            case "RESPONSE_TIME" -> new ResponseTimeAssertion(
                    parseDoubleSafe(def.expectedValue(), "RESPONSE_TIME expected value"));
            case "BODY_CONTAINS" -> new BodyContainsAssertion(def.expectedValue(), true);
            case "JSON_PATH" -> new JsonPathAssertion(
                    def.target() != null ? def.target() : def.expectedValue(),
                    def.expectedValue());
            case "HEADER" -> new HeaderAssertion(
                    def.target() != null ? def.target() : "",
                    def.expectedValue());
            default -> throw new IllegalArgumentException(
                    "Unknown assertion type: " + def.type());
        };
    }

    /**
     * Creates assertion instances from a list of definitions.
     *
     * @param definitions list of assertion definitions (may be null)
     * @return list of assertion instances (never null)
     */
    public static List<Assertion> createAssertions(List<AssertionDefinition> definitions) {
        if (definitions == null || definitions.isEmpty()) {
            return List.of();
        }

        List<Assertion> assertions = new ArrayList<>(definitions.size());
        for (AssertionDefinition def : definitions) {
            assertions.add(createAssertion(def));
        }
        return assertions;
    }

    private static int parseIntSafe(String value, String fieldName) {
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(fieldName + " must be an integer, got: '" + value + "'");
        }
    }

    private static double parseDoubleSafe(String value, String fieldName) {
        try {
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(fieldName + " must be a number, got: '" + value + "'");
        }
    }
}
