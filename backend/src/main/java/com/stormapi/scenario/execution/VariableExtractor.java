package com.stormapi.scenario.execution;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stormapi.scenario.dto.ExtractionRuleDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Extracts named variable values from JSON response bodies using
 * simplified JSONPath dot-notation.
 *
 * <h3>Supported syntax</h3>
 * <ul>
 *   <li>{@code $.field} — root-level field</li>
 *   <li>{@code $.parent.child} — nested object field</li>
 *   <li>{@code $.array[0]} — array element by index</li>
 *   <li>{@code $.array[0].field} — field on array element</li>
 * </ul>
 *
 * <h3>Unsupported</h3>
 * <ul>
 *   <li>Recursive descent ({@code $..*})</li>
 *   <li>Wildcards ({@code $.array[*]})</li>
 *   <li>Filter expressions ({@code $.array[?(@.name == 'foo')]})</li>
 * </ul>
 *
 * <p>Thread-safe: stateless, all state passed as method parameters.</p>
 */
public class VariableExtractor {

    private static final Logger log = LoggerFactory.getLogger(VariableExtractor.class);
    private static final ObjectMapper JSON = new ObjectMapper();

    /**
     * Pattern to match array index access: {@code fieldName[index]}
     */
    private static final Pattern ARRAY_PATTERN = Pattern.compile("^(\\w+)\\[(\\d+)]$");

    /**
     * Extracts variable values from a JSON response body using the given rules.
     *
     * @param responseBody JSON string to extract from (may be null)
     * @param rules        extraction rules mapping variable names to JSON paths
     * @return map of variableName → extracted string value (never null)
     */
    public Map<String, String> extract(String responseBody, List<ExtractionRuleDto> rules) {
        Map<String, String> result = new LinkedHashMap<>();

        if (responseBody == null || responseBody.isBlank() || rules == null || rules.isEmpty()) {
            return result;
        }

        JsonNode root;
        try {
            root = JSON.readTree(responseBody);
        } catch (JsonProcessingException e) {
            log.warn("Response body is not valid JSON, skipping extraction: {}", e.getMessage());
            return result;
        }

        for (ExtractionRuleDto rule : rules) {
            if (rule.variableName() == null || rule.variableName().isBlank()
                    || rule.jsonPath() == null || rule.jsonPath().isBlank()) {
                log.warn("Skipping extraction rule with blank variable name or path");
                continue;
            }

            String value = navigatePath(root, rule.jsonPath());
            result.put(rule.variableName(), value);

            if (value.isEmpty()) {
                log.debug("Path '{}' not found in response, variable '{}' set to empty",
                        rule.jsonPath(), rule.variableName());
            }
        }

        return result;
    }

    /**
     * Navigates a JSONPath expression against a JSON tree.
     * Returns the text value of the resolved node, or empty string if not found.
     */
    private String navigatePath(JsonNode root, String jsonPath) {
        // Strip leading "$" and optional "."
        String path = jsonPath.startsWith("$.") ? jsonPath.substring(2)
                : jsonPath.startsWith("$") ? jsonPath.substring(1)
                : jsonPath;

        if (path.isEmpty()) {
            return nodeToString(root);
        }

        // Split on "." but preserve array brackets
        String[] segments = path.split("\\.");
        JsonNode current = root;

        for (String segment : segments) {
            if (current == null || current.isMissingNode() || current.isNull()) {
                return "";
            }

            Matcher arrayMatcher = ARRAY_PATTERN.matcher(segment);
            if (arrayMatcher.matches()) {
                // Handle array access: field[index]
                String fieldName = arrayMatcher.group(1);
                int index = Integer.parseInt(arrayMatcher.group(2));

                current = current.get(fieldName);
                if (current == null || !current.isArray() || index >= current.size()) {
                    return "";
                }
                current = current.get(index);
            } else {
                // Simple field access
                current = current.get(segment);
            }
        }

        return nodeToString(current);
    }

    /**
     * Converts a JsonNode to its string representation.
     * For text nodes: returns the text value.
     * For number/boolean nodes: returns the string representation.
     * For null/missing: returns empty string.
     * For object/array nodes: returns the JSON string.
     */
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
        // For objects/arrays, return the JSON representation
        return node.toString();
    }

}
