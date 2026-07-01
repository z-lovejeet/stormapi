package com.stormapi.scenario.mapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stormapi.collection.dto.KeyValuePairDto;
import com.stormapi.collection.model.KeyValuePair;
import com.stormapi.scenario.dto.AddStepRequest;
import com.stormapi.scenario.dto.CreateScenarioRequest;
import com.stormapi.scenario.dto.ExtractionRuleDto;
import com.stormapi.scenario.dto.ScenarioResponse;
import com.stormapi.scenario.dto.StepResponse;
import com.stormapi.scenario.model.ScenarioStep;
import com.stormapi.scenario.model.TestScenario;

import java.util.Collections;
import java.util.List;

/**
 * Maps between Scenario/Step entities and their DTO representations.
 *
 * <p>Uses a shared {@link ObjectMapper} instance for JSON serialization
 * of extraction rules (stored as TEXT in the database).</p>
 */
public final class ScenarioMapper {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final TypeReference<List<ExtractionRuleDto>> RULES_TYPE =
            new TypeReference<>() {};

    private ScenarioMapper() { /* utility class */ }

    // ── Entity construction ──────────────────────────────────────

    /**
     * Creates a {@link TestScenario} entity from a create request.
     */
    public static TestScenario toEntity(CreateScenarioRequest request) {
        return TestScenario.builder()
                .name(request.name())
                .description(request.description())
                .failFast(request.isFailFast())
                .build();
    }

    /**
     * Creates a {@link ScenarioStep} entity from an add-step request.
     */
    public static ScenarioStep toStepEntity(AddStepRequest request,
                                             TestScenario scenario,
                                             int stepOrder) {
        return ScenarioStep.builder()
                .scenario(scenario)
                .stepOrder(stepOrder)
                .name(request.name())
                .url(request.url())
                .method(request.method())
                .headers(toKeyValuePairs(request.headers()))
                .body(request.body())
                .extractionRulesJson(serializeExtractionRules(request.extractionRules()))
                .build();
    }

    // ── Response mapping ─────────────────────────────────────────

    /**
     * Maps a {@link TestScenario} entity to its response DTO.
     */
    public static ScenarioResponse toResponse(TestScenario scenario) {
        List<StepResponse> steps = scenario.getSteps() == null
                ? List.of()
                : scenario.getSteps().stream()
                        .map(ScenarioMapper::toStepResponse)
                        .toList();

        return new ScenarioResponse(
                scenario.getId(),
                scenario.getName(),
                scenario.getDescription(),
                scenario.isFailFast(),
                steps,
                scenario.getCreatedAt(),
                scenario.getUpdatedAt()
        );
    }

    /**
     * Maps a {@link ScenarioStep} entity to its response DTO.
     */
    public static StepResponse toStepResponse(ScenarioStep step) {
        return new StepResponse(
                step.getId(),
                step.getStepOrder(),
                step.getName(),
                step.getUrl(),
                step.getMethod(),
                toKeyValuePairDtos(step.getHeaders()),
                step.getBody(),
                parseExtractionRules(step.getExtractionRulesJson())
        );
    }

    // ── Extraction rules JSON ────────────────────────────────────

    /**
     * Parses a JSON string into a list of extraction rule DTOs.
     * Returns empty list if the JSON is null, blank, or malformed.
     */
    public static List<ExtractionRuleDto> parseExtractionRules(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return JSON.readValue(json, RULES_TYPE);
        } catch (JsonProcessingException e) {
            return List.of();
        }
    }

    /**
     * Serializes a list of extraction rule DTOs to a JSON string.
     * Returns null if the list is null or empty.
     */
    public static String serializeExtractionRules(List<ExtractionRuleDto> rules) {
        if (rules == null || rules.isEmpty()) {
            return null;
        }
        try {
            return JSON.writeValueAsString(rules);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    // ── KeyValuePair helpers ─────────────────────────────────────

    private static List<KeyValuePair> toKeyValuePairs(List<KeyValuePairDto> dtos) {
        if (dtos == null || dtos.isEmpty()) {
            return Collections.emptyList();
        }
        return dtos.stream()
                .map(dto -> new KeyValuePair(dto.key(), dto.value()))
                .toList();
    }

    private static List<KeyValuePairDto> toKeyValuePairDtos(List<KeyValuePair> pairs) {
        if (pairs == null || pairs.isEmpty()) {
            return List.of();
        }
        return pairs.stream()
                .map(p -> new KeyValuePairDto(p.getKey(), p.getValue()))
                .toList();
    }

}
