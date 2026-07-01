package com.stormapi.scenario.execution;

import com.stormapi.collection.dto.KeyValuePairDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link TemplateResolver}.
 */
class TemplateResolverTest {

    private TemplateResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new TemplateResolver();
    }

    @Test
    @DisplayName("Single variable replacement")
    void shouldReplaceSingleVariable() {
        String template = "https://api.example.com/users/{{userId}}";
        Map<String, String> vars = Map.of("userId", "42");

        String result = resolver.resolve(template, vars);

        assertThat(result).isEqualTo("https://api.example.com/users/42");
    }

    @Test
    @DisplayName("Multiple variable replacements")
    void shouldReplaceMultipleVariables() {
        String template = "{{baseUrl}}/users/{{userId}}/posts/{{postId}}";
        Map<String, String> vars = Map.of(
                "baseUrl", "https://api.example.com",
                "userId", "5",
                "postId", "99"
        );

        String result = resolver.resolve(template, vars);

        assertThat(result).isEqualTo("https://api.example.com/users/5/posts/99");
    }

    @Test
    @DisplayName("Missing variable left as-is")
    void shouldLeaveMissingVariableAsIs() {
        String template = "Hello {{name}}, your token is {{token}}";
        Map<String, String> vars = Map.of("name", "Alice");

        String result = resolver.resolve(template, vars);

        assertThat(result).isEqualTo("Hello Alice, your token is {{token}}");
    }

    @Test
    @DisplayName("No placeholders returns original string")
    void shouldReturnOriginalWithNoPlaceholders() {
        String template = "https://api.example.com/health";
        Map<String, String> vars = Map.of("key", "value");

        String result = resolver.resolve(template, vars);

        assertThat(result).isEqualTo(template);
    }

    @Test
    @DisplayName("Null template returns null")
    void shouldReturnNullForNullTemplate() {
        assertThat(resolver.resolve(null, Map.of("k", "v"))).isNull();
    }

    @Test
    @DisplayName("Header values are resolved, keys are not")
    void shouldResolveHeaderValues() {
        List<KeyValuePairDto> headers = List.of(
                new KeyValuePairDto("Authorization", "Bearer {{token}}"),
                new KeyValuePairDto("X-Request-Id", "{{requestId}}")
        );
        Map<String, String> vars = Map.of("token", "abc123", "requestId", "req-001");

        Map<String, String> result = resolver.resolveHeaders(headers, vars);

        assertThat(result)
                .containsEntry("Authorization", "Bearer abc123")
                .containsEntry("X-Request-Id", "req-001");
    }

}
