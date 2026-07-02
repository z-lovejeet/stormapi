package com.stormapi.engine.assertion;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Asserts that a JSONPath expression in the response body evaluates
 * to the expected value.
 *
 * <p>Reuses the same simplified JSONPath dot-notation supported by
 * {@code VariableExtractor}:
 * <ul>
 *   <li>{@code $.field} — root-level field</li>
 *   <li>{@code $.parent.child} — nested object field</li>
 *   <li>{@code $.array[0]} — array element by index</li>
 *   <li>{@code $.array[0].field} — field on array element</li>
 * </ul>
 */
public class JsonPathAssertion implements Assertion {

    private static final String TYPE = "JSON_PATH";
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final Pattern ARRAY_PATTERN = Pattern.compile("^(\\w+)\\[(\\d+)]$");

    private final String jsonPath;
    private final String expectedValue;

    public JsonPathAssertion(String jsonPath, String expectedValue) {
        if (jsonPath == null || jsonPath.isBlank()) {
            throw new IllegalArgumentException("JSON path must not be blank");
        }
        if (!jsonPath.startsWith("$")) {
            throw new IllegalArgumentException("JSON path must start with '$', got: " + jsonPath);
        }
        this.jsonPath = jsonPath;
        this.expectedValue = expectedValue != null ? expectedValue : "";
    }

    @Override
    public AssertionResult evaluate(AssertionContext context) {
        String body = context.responseBody();

        if (body == null || body.isEmpty()) {
            return AssertionResult.fail(TYPE, jsonPath,
                    expectedValue, "(empty body)",
                    "Response body is empty, cannot evaluate JSONPath '" + jsonPath + "'");
        }

        JsonNode root;
        try {
            root = JSON.readTree(body);
        } catch (JsonProcessingException e) {
            return AssertionResult.fail(TYPE, jsonPath,
                    expectedValue, "(invalid JSON)",
                    "Response body is not valid JSON: " + e.getMessage());
        }

        String actual = navigatePath(root, jsonPath);

        boolean passed = expectedValue.equals(actual);
        String message = passed
                ? "JSONPath '" + jsonPath + "' value '" + actual + "' matches expected '" + expectedValue + "'"
                : "JSONPath '" + jsonPath + "' value '" + actual + "' does not match expected '" + expectedValue + "'";

        return passed
                ? AssertionResult.pass(TYPE, jsonPath, expectedValue, actual, message)
                : AssertionResult.fail(TYPE, jsonPath, expectedValue, actual, message);
    }

    @Override
    public String getType() {
        return TYPE;
    }

    /**
     * Navigates a JSONPath expression against a JSON tree.
     * Returns the text value of the resolved node, or empty string if not found.
     */
    private String navigatePath(JsonNode root, String path) {
        // Strip leading "$" and optional "."
        String normalized = path.startsWith("$.") ? path.substring(2)
                : path.startsWith("$") ? path.substring(1)
                : path;

        if (normalized.isEmpty()) {
            return nodeToString(root);
        }

        String[] segments = normalized.split("\\.");
        JsonNode current = root;

        for (String segment : segments) {
            if (current == null || current.isMissingNode() || current.isNull()) {
                return "";
            }

            Matcher arrayMatcher = ARRAY_PATTERN.matcher(segment);
            if (arrayMatcher.matches()) {
                String fieldName = arrayMatcher.group(1);
                int index = Integer.parseInt(arrayMatcher.group(2));

                current = current.get(fieldName);
                if (current == null || !current.isArray() || index >= current.size()) {
                    return "";
                }
                current = current.get(index);
            } else {
                current = current.get(segment);
            }
        }

        return nodeToString(current);
    }

    private String nodeToString(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return "";
        }
        if (node.isTextual()) {
            return node.textValue();
        }
        if (node.isNumber() || node.isBoolean()) {
            return node.asText();
        }
        return node.toString();
    }
}
