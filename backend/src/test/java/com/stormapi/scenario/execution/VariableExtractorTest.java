package com.stormapi.scenario.execution;

import com.stormapi.scenario.dto.ExtractionRuleDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link VariableExtractor}.
 */
class VariableExtractorTest {

    private VariableExtractor extractor;

    @BeforeEach
    void setUp() {
        extractor = new VariableExtractor();
    }

    @Test
    @DisplayName("Extract root-level string field")
    void shouldExtractRootLevelString() {
        String json = """
                {"token": "abc123", "status": "ok"}
                """;
        List<ExtractionRuleDto> rules = List.of(
                new ExtractionRuleDto("token", "$.token")
        );

        Map<String, String> result = extractor.extract(json, rules);

        assertThat(result).containsEntry("token", "abc123");
    }

    @Test
    @DisplayName("Extract nested object field")
    void shouldExtractNestedField() {
        String json = """
                {"data": {"id": 42, "name": "John"}}
                """;
        List<ExtractionRuleDto> rules = List.of(
                new ExtractionRuleDto("userId", "$.data.id"),
                new ExtractionRuleDto("userName", "$.data.name")
        );

        Map<String, String> result = extractor.extract(json, rules);

        assertThat(result)
                .containsEntry("userId", "42")
                .containsEntry("userName", "John");
    }

    @Test
    @DisplayName("Extract array element field")
    void shouldExtractArrayElement() {
        String json = """
                {"users": [{"name": "Alice"}, {"name": "Bob"}]}
                """;
        List<ExtractionRuleDto> rules = List.of(
                new ExtractionRuleDto("firstUser", "$.users[0].name"),
                new ExtractionRuleDto("secondUser", "$.users[1].name")
        );

        Map<String, String> result = extractor.extract(json, rules);

        assertThat(result)
                .containsEntry("firstUser", "Alice")
                .containsEntry("secondUser", "Bob");
    }

    @Test
    @DisplayName("Missing path returns empty string")
    void shouldReturnEmptyForMissingPath() {
        String json = """
                {"data": {"id": 1}}
                """;
        List<ExtractionRuleDto> rules = List.of(
                new ExtractionRuleDto("missing", "$.data.nonexistent")
        );

        Map<String, String> result = extractor.extract(json, rules);

        assertThat(result).containsEntry("missing", "");
    }

    @Test
    @DisplayName("Invalid JSON returns empty map")
    void shouldReturnEmptyMapForInvalidJson() {
        String json = "this is not json";
        List<ExtractionRuleDto> rules = List.of(
                new ExtractionRuleDto("value", "$.field")
        );

        Map<String, String> result = extractor.extract(json, rules);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Null body returns empty map")
    void shouldReturnEmptyMapForNullBody() {
        List<ExtractionRuleDto> rules = List.of(
                new ExtractionRuleDto("value", "$.field")
        );

        Map<String, String> result = extractor.extract(null, rules);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Empty rules returns empty map")
    void shouldReturnEmptyMapForEmptyRules() {
        String json = """
                {"data": 42}
                """;

        Map<String, String> result = extractor.extract(json, List.of());

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Extract boolean and number values as strings")
    void shouldExtractBooleanAndNumber() {
        String json = """
                {"active": true, "count": 99, "ratio": 3.14}
                """;
        List<ExtractionRuleDto> rules = List.of(
                new ExtractionRuleDto("active", "$.active"),
                new ExtractionRuleDto("count", "$.count"),
                new ExtractionRuleDto("ratio", "$.ratio")
        );

        Map<String, String> result = extractor.extract(json, rules);

        assertThat(result)
                .containsEntry("active", "true")
                .containsEntry("count", "99")
                .containsEntry("ratio", "3.14");
    }

}
