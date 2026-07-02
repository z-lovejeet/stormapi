package com.stormapi.engine.assertion;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class BodyContainsAssertionTest {

    @Test
    void shouldPassWhenBodyContainsText() {
        BodyContainsAssertion assertion = new BodyContainsAssertion("success", true);
        AssertionContext ctx = new AssertionContext(200, 100.0,
                "{\"status\":\"success\",\"data\":[]}", Map.of());
        AssertionResult result = assertion.evaluate(ctx);

        assertTrue(result.passed());
        assertEquals("found", result.actual());
    }

    @Test
    void shouldFailWhenBodyDoesNotContainText() {
        BodyContainsAssertion assertion = new BodyContainsAssertion("error", true);
        AssertionContext ctx = new AssertionContext(200, 100.0,
                "{\"status\":\"success\"}", Map.of());
        AssertionResult result = assertion.evaluate(ctx);

        assertFalse(result.passed());
        assertEquals("not found", result.actual());
    }

    @Test
    void shouldHandleCaseInsensitiveSearch() {
        BodyContainsAssertion assertion = new BodyContainsAssertion("SUCCESS", false);
        AssertionContext ctx = new AssertionContext(200, 100.0,
                "{\"status\":\"success\"}", Map.of());
        AssertionResult result = assertion.evaluate(ctx);

        assertTrue(result.passed());
    }

    @Test
    void shouldFailOnNullBody() {
        BodyContainsAssertion assertion = new BodyContainsAssertion("test", true);
        AssertionContext ctx = new AssertionContext(200, 100.0, null, Map.of());
        AssertionResult result = assertion.evaluate(ctx);

        assertFalse(result.passed());
        assertTrue(result.message().contains("empty"));
    }

    @Test
    void shouldRejectBlankExpectedText() {
        assertThrows(IllegalArgumentException.class, () -> new BodyContainsAssertion("", true));
        assertThrows(IllegalArgumentException.class, () -> new BodyContainsAssertion("  ", true));
    }
}
