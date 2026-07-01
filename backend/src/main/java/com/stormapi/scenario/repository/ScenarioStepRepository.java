package com.stormapi.scenario.repository;

import com.stormapi.scenario.model.ScenarioStep;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for {@link ScenarioStep} entities.
 */
public interface ScenarioStepRepository extends JpaRepository<ScenarioStep, Long> {

    /** Find all steps for a scenario, ordered by step execution order. */
    List<ScenarioStep> findByScenarioIdOrderByStepOrderAsc(Long scenarioId);

    /** Find a specific step within a scenario. */
    Optional<ScenarioStep> findByIdAndScenarioId(Long id, Long scenarioId);

    /** Count steps in a scenario (for determining next stepOrder). */
    int countByScenarioId(Long scenarioId);

}
