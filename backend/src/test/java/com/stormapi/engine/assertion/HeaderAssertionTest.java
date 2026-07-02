package com.stormapi.engine.assertion;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class HeaderAssertionTest {

    @Test
    void shouldPassWhenHeaderMatchesExactly() {
        HeaderAssertion assertion = new HeaderAssertion("Content-Type", "application/json");
        AssertionContext ctx = new AssertionContext(200, 100.0, "{}",
                Map.of("Content-Type", "application/json"));
        AssertionResult result = assertion.evaluate(ctx);

        assertTrue(result.passed());
        assertEquals("application/json", result.actual());
    }

    @Test
    void shouldFailWhenHeaderValueDoesNotMatch() {
        HeaderAssertion assertion = new HeaderAssertion("Content-Type", "application/json");
        AssertionContext ctx = new AssertionContext(200, 100.0, "{}",
                Map.of("Content-Type", "text/html"));
        AssertionResult result = assertion.evaluate(ctx);

        assertFalse(result.passed());
        assertEquals("text/html", result.actual());
    }

    @Test
    void shouldFailWhenHeaderNotPresent() {
        HeaderAssertion assertion = new HeaderAssertion("X-Custom", "value");
        AssertionContext ctx = new AssertionContext(200, 100.0, "{}",
                Map.of("Content-Type", "application/json"));
        AssertionResult result = assertion.evaluate(ctx);

        assertFalse(result.passed());
        assertTrue(result.message().contains("not found"));
    }

    @Test
    void shouldMatchHeaderNameCaseInsensitively() {
        HeaderAssertion assertion = new HeaderAssertion("content-type", "application/json");
        AssertionContext ctx = new AssertionContext(200, 100.0, "{}",
                Map.of("Content-Type", "application/json"));
        AssertionResult result = assertion.evaluate(ctx);

        assertTrue(result.passed());
    }

    @Test
    void shouldHandleNullHeaders() {
        HeaderAssertion assertion = new HeaderAssertion("Content-Type", "application/json");
        AssertionContext ctx = new AssertionContext(200, 100.0, "{}", null);
        AssertionResult result = assertion.evaluate(ctx);

        assertFalse(result.passed());
        assertTrue(result.message().contains("no headers"));
    }

    @Test
    void shouldRejectBlankHeaderName() {
        assertThrows(IllegalArgumentException.class, () -> new HeaderAssertion("", "value"));
    }
}
