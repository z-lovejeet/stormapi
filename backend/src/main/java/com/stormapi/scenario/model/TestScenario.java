package com.stormapi.scenario.model;

import com.stormapi.common.model.BaseEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * Multi-step test scenario — chains API calls with variable extraction
 * and template resolution between steps.
 *
 * <p>Analogous to a Postman "Runner" or k6 scenario: each step can extract
 * values from the response (JSONPath) and inject them into subsequent
 * requests via {@code {{variable}}} placeholders.</p>
 */
@Entity
@Table(name = "test_scenarios")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TestScenario extends BaseEntity {

    /** Scenario display name */
    @Column(nullable = false, length = 255)
    private String name;

    /** Optional description */
    @Column(length = 1000)
    private String description;

    /**
     * If true, execution aborts on the first step failure.
     * If false, all steps execute regardless of individual failures.
     */
    @Column(nullable = false)
    @Builder.Default
    private boolean failFast = true;

    /** Ordered list of steps in this scenario */
    @OneToMany(mappedBy = "scenario", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("stepOrder ASC")
    @Builder.Default
    private List<ScenarioStep> steps = new ArrayList<>();

}
