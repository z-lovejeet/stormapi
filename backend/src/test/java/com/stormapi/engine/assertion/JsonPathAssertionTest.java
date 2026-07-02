package com.stormapi.engine.assertion;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class JsonPathAssertionTest {

    private static final String JSON_BODY = """
            {
              "status": "ok",
              "data": {
                "id": 42,
                "name": "Alice",
                "active": true
              },
              "items": [
                {"label": "first"},
                {"label": "second"}
              ]
            }
            """;

    @Test
    void shouldPassWhenPathValueMatches() {
        JsonPathAssertion assertion = new JsonPathAssertion("$.status", "ok");
        AssertionContext ctx = new AssertionContext(200, 100.0, JSON_BODY, Map.of());
        AssertionResult result = assertion.evaluate(ctx);

        assertTrue(result.passed());
        assertEquals("ok", result.actual());
    }

    @Test
    void shouldFailWhenPathValueDoesNotMatch() {
        JsonPathAssertion assertion = new JsonPathAssertion("$.status", "error");
        AssertionContext ctx = new AssertionContext(200, 100.0, JSON_BODY, Map.of());
        AssertionResult result = assertion.evaluate(ctx);

        assertFalse(result.passed());
        assertEquals("ok", result.actual());
        assertEquals("error", result.expected());
    }

    @Test
    void shouldNavigateNestedPath() {
        JsonPathAssertion assertion = new JsonPathAssertion("$.data.name", "Alice");
        AssertionContext ctx = new AssertionContext(200, 100.0, JSON_BODY, Map.of());
        AssertionResult result = assertion.evaluate(ctx);

        assertTrue(result.passed());
    }

    @Test
    void shouldNavigateArrayAccess() {
        JsonPathAssertion assertion = new JsonPathAssertion("$.items[0].label", "first");
        AssertionContext ctx = new AssertionContext(200, 100.0, JSON_BODY, Map.of());
        AssertionResult result = assertion.evaluate(ctx);

        assertTrue(result.passed());
    }

    @Test
    void shouldReturnEmptyForMissingPath() {
        JsonPathAssertion assertion = new JsonPathAssertion("$.nonexistent", "value");
        AssertionContext ctx = new AssertionContext(200, 100.0, JSON_BODY, Map.of());
        AssertionResult result = assertion.evaluate(ctx);

        assertFalse(result.passed());
        assertEquals("", result.actual());
    }

    @Test
    void shouldHandleNullBody() {
        JsonPathAssertion assertion = new JsonPathAssertion("$.status", "ok");
        AssertionContext ctx = new AssertionContext(200, 100.0, null, Map.of());
        AssertionResult result = assertion.evaluate(ctx);

        assertFalse(result.passed());
        assertTrue(result.message().contains("empty"));
    }

    @Test
    void shouldHandleInvalidJson() {
        JsonPathAssertion assertion = new JsonPathAssertion("$.status", "ok");
        AssertionContext ctx = new AssertionContext(200, 100.0, "not json", Map.of());
        AssertionResult result = assertion.evaluate(ctx);

        assertFalse(result.passed());
        assertTrue(result.message().contains("not valid JSON"));
    }

    @Test
    void shouldRejectBlankPath() {
        assertThrows(IllegalArgumentException.class, () -> new JsonPathAssertion("", "val"));
    }

    @Test
    void shouldRejectPathWithoutDollar() {
        assertThrows(IllegalArgumentException.class, () -> new JsonPathAssertion("status", "val"));
    }

    @Test
    void shouldHandleNumericValues() {
        JsonPathAssertion assertion = new JsonPathAssertion("$.data.id", "42");
        AssertionContext ctx = new AssertionContext(200, 100.0, JSON_BODY, Map.of());
        AssertionResult result = assertion.evaluate(ctx);

        assertTrue(result.passed());
    }

    @Test
    void shouldHandleBooleanValues() {
        JsonPathAssertion assertion = new JsonPathAssertion("$.data.active", "true");
        AssertionContext ctx = new AssertionContext(200, 100.0, JSON_BODY, Map.of());
        AssertionResult result = assertion.evaluate(ctx);

        assertTrue(result.passed());
    }
}
