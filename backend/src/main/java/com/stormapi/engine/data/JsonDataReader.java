package com.stormapi.engine.data;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Reads parameterized test data from a JSON array of objects.
 *
 * <p>Expected format:
 * <pre>
 * [
 *   { "username": "alice", "password": "pass1" },
 *   { "username": "bob",   "password": "pass2" }
 * ]
 * </pre>
 *
 * <p>All values are coerced to strings. Nested objects/arrays
 * are serialized as their JSON string representation.
 * Enforces {@link DataReader#MAX_ROWS} limit.
 */
public class JsonDataReader implements DataReader {

    private static final ObjectMapper JSON = new ObjectMapper();

    @Override
    public List<Map<String, String>> read(String content) {
        if (content == null || content.isBlank()) {
            return List.of();
        }

        JsonNode root;
        try {
            root = JSON.readTree(content);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Invalid JSON: " + e.getMessage(), e);
        }

        if (!root.isArray()) {
            throw new IllegalArgumentException(
                    "JSON data must be an array of objects, got: " + root.getNodeType());
        }

        ArrayNode array = (ArrayNode) root;
        if (array.size() > MAX_ROWS) {
            throw new IllegalArgumentException(
                    "JSON data exceeds maximum of " + MAX_ROWS + " rows, got: " + array.size());
        }

        List<Map<String, String>> rows = new ArrayList<>(array.size());

        for (JsonNode element : array) {
            if (!element.isObject()) {
                throw new IllegalArgumentException(
                        "Each JSON array element must be an object, got: " + element.getNodeType());
            }

            Map<String, String> row = new LinkedHashMap<>();
            Iterator<Map.Entry<String, JsonNode>> fields = element.fields();

            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                row.put(field.getKey(), nodeToString(field.getValue()));
            }

            rows.add(row);
        }

        return rows;
    }

    @Override
    public String getFormat() {
        return "JSON";
    }

    private String nodeToString(JsonNode node) {
        if (node == null || node.isNull()) {
            return "";
        }
        if (node.isTextual()) {
            return node.textValue();
        }
        if (node.isNumber() || node.isBoolean()) {
            return node.asText();
        }
        // Nested objects/arrays → serialize as JSON string
        return node.toString();
    }
}
