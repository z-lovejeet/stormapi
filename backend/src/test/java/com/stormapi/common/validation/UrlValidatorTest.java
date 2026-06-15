package com.stormapi.common.validation;

import jakarta.validation.ConstraintValidatorContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Unit tests for UrlValidator — validates HTTP/HTTPS URL format.
 */
class UrlValidatorTest {

    private UrlValidator validator;
    private ConstraintValidatorContext context;

    @BeforeEach
    void setUp() {
        validator = new UrlValidator();
        context = mock(ConstraintValidatorContext.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "https://api.example.com",
            "http://localhost:8080",
            "https://api.example.com/v1/users?page=1",
            "http://192.168.1.1:3000/health",
            "https://sub.domain.example.com/path/to/resource"
    })
    void validUrls_returnTrue(String url) {
        assertThat(validator.isValid(url, context)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "not-a-url",
            "ftp://files.example.com",
            "://missing-scheme",
            "just-text",
            "ws://websocket.example.com"
    })
    void invalidUrls_returnFalse(String url) {
        assertThat(validator.isValid(url, context)).isFalse();
    }

    @Test
    void nullValue_returnsTrue() {
        // Null handling is delegated to @NotBlank
        assertThat(validator.isValid(null, context)).isTrue();
    }

    @Test
    void emptyString_returnsTrue() {
        // Empty handling is delegated to @NotBlank
        assertThat(validator.isValid("", context)).isTrue();
    }

    @Test
    void blankString_returnsTrue() {
        assertThat(validator.isValid("   ", context)).isTrue();
    }

}
