package com.stormapi.scenario.model;

import com.stormapi.collection.model.KeyValuePair;
import com.stormapi.common.model.BaseEntity;
import com.stormapi.test.model.HttpMethod;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * Single step within a {@link TestScenario}.
 *
 * <p>Each step stores its own URL, method, headers, and body — values are
 * <b>copied</b> from a collection endpoint (not referenced by FK) so that
 * editing a collection endpoint never breaks existing scenarios.</p>
 *
 * <p>Steps may contain {@code {{variable}}} placeholders in URL, header values,
 * and body that the {@code TemplateResolver} replaces at execution time.</p>
 *
 * <p>Extraction rules are stored as a JSON array in {@code extractionRulesJson}:
 * <pre>
 * [{"variableName":"userId","jsonPath":"$.data.id"},
 *  {"variableName":"token","jsonPath":"$.token"}]
 * </pre></p>
 */
@Entity
@Table(name = "scenario_steps", indexes = {
        @Index(name = "idx_scenario_step_scenario_id", columnList = "scenario_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScenarioStep extends BaseEntity {

    /** Parent scenario */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "scenario_id", nullable = false)
    private TestScenario scenario;

    /** Execution order (0-indexed) */
    @Column(nullable = false)
    private int stepOrder;

    /** Step display name */
    @Column(nullable = false, length = 255)
    private String name;

    /** Target URL — may contain {{variable}} placeholders */
    @Column(nullable = false, length = 2048)
    private String url;

    /** HTTP method */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private HttpMethod method;

    /** Request headers as key-value pairs */
    @ElementCollection
    @CollectionTable(
            name = "scenario_step_headers",
            joinColumns = @JoinColumn(name = "step_id")
    )
    @Builder.Default
    private List<KeyValuePair> headers = new ArrayList<>();

    /** Request body — may contain {{variable}} placeholders */
    @Column(columnDefinition = "TEXT")
    private String body;

    /**
     * JSON-encoded extraction rules applied to the response body.
     * Parsed by {@code ScenarioMapper.parseExtractionRules()}.
     */
    @Column(name = "extraction_rules_json", columnDefinition = "TEXT")
    private String extractionRulesJson;

}
