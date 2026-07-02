package com.stormapi.engine.data;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class JsonDataReaderTest {

    private final JsonDataReader reader = new JsonDataReader();

    @Test
    void shouldParseSimpleJsonArray() {
        String json = """
                [
                  {"username": "alice", "password": "pass123"},
                  {"username": "bob", "password": "secret"}
                ]
                """;

        List<Map<String, String>> rows = reader.read(json);

        assertEquals(2, rows.size());
        assertEquals("alice", rows.get(0).get("username"));
        assertEquals("pass123", rows.get(0).get("password"));
        assertEquals("bob", rows.get(1).get("username"));
    }

    @Test
    void shouldCoerceNumericAndBooleanValues() {
        String json = """
                [{"id": 42, "active": true, "score": 3.14}]
                """;

        List<Map<String, String>> rows = reader.read(json);

        assertEquals(1, rows.size());
        assertEquals("42", rows.get(0).get("id"));
        assertEquals("true", rows.get(0).get("active"));
        assertEquals("3.14", rows.get(0).get("score"));
    }

    @Test
    void shouldSerializeNestedObjectsAsJsonString() {
        String json = """
                [{"name": "test", "config": {"key": "val"}}]
                """;

        List<Map<String, String>> rows = reader.read(json);

        assertEquals(1, rows.size());
        assertTrue(rows.get(0).get("config").contains("key"));
    }

    @Test
    void shouldHandleNullValues() {
        String json = """
                [{"name": "test", "value": null}]
                """;

        List<Map<String, String>> rows = reader.read(json);

        assertEquals("", rows.get(0).get("value"));
    }

    @Test
    void shouldReturnEmptyForNullContent() {
        assertTrue(reader.read(null).isEmpty());
    }

    @Test
    void shouldReturnEmptyForBlankContent() {
        assertTrue(reader.read("  ").isEmpty());
    }

    @Test
    void shouldThrowForNonArrayJson() {
        assertThrows(IllegalArgumentException.class, () -> reader.read("{\"key\": \"val\"}"));
    }

    @Test
    void shouldThrowForNonObjectElements() {
        assertThrows(IllegalArgumentException.class, () -> reader.read("[1, 2, 3]"));
    }

    @Test
    void shouldThrowForInvalidJson() {
        assertThrows(IllegalArgumentException.class, () -> reader.read("not json"));
    }

    @Test
    void shouldReturnJsonFormat() {
        assertEquals("JSON", reader.getFormat());
    }

    @Test
    void shouldHandleEmptyArray() {
        String json = "[]";
        List<Map<String, String>> rows = reader.read(json);
        assertTrue(rows.isEmpty());
    }
}
