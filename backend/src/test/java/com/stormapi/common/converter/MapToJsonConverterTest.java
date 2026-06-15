package com.stormapi.common.converter;

import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for MapToJsonConverter.
 * Verifies round-trip serialization, null handling, and error behavior.
 */
class MapToJsonConverterTest {

    private final MapToJsonConverter converter = new MapToJsonConverter();

    @Test
    void convertToDatabaseColumn_validMap_returnsJsonString() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("Content-Type", "application/json");
        map.put("Authorization", "Bearer token123");

        String json = converter.convertToDatabaseColumn(map);

        assertNotNull(json);
        assertTrue(json.contains("\"Content-Type\""));
        assertTrue(json.contains("\"application/json\""));
        assertTrue(json.contains("\"Authorization\""));
    }

    @Test
    void convertToEntityAttribute_validJson_returnsMap() {
        String json = "{\"Content-Type\":\"application/json\",\"Accept\":\"*/*\"}";

        Map<String, String> map = converter.convertToEntityAttribute(json);

        assertEquals(2, map.size());
        assertEquals("application/json", map.get("Content-Type"));
        assertEquals("*/*", map.get("Accept"));
    }

    @Test
    void convertToDatabaseColumn_nullMap_returnsNull() {
        assertNull(converter.convertToDatabaseColumn(null));
    }

    @Test
    void convertToDatabaseColumn_emptyMap_returnsNull() {
        assertNull(converter.convertToDatabaseColumn(Collections.emptyMap()));
    }

    @Test
    void convertToEntityAttribute_nullJson_returnsEmptyMap() {
        Map<String, String> result = converter.convertToEntityAttribute(null);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void convertToEntityAttribute_blankJson_returnsEmptyMap() {
        Map<String, String> result = converter.convertToEntityAttribute("  ");
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void convertToEntityAttribute_malformedJson_throwsException() {
        assertThrows(IllegalArgumentException.class, () ->
                converter.convertToEntityAttribute("{invalid json}"));
    }

    @Test
    void roundTrip_preservesData() {
        Map<String, String> original = new LinkedHashMap<>();
        original.put("X-Custom-Header", "value1");
        original.put("X-Another", "value2");

        String json = converter.convertToDatabaseColumn(original);
        Map<String, String> restored = converter.convertToEntityAttribute(json);

        assertEquals(original, restored);
    }

}
