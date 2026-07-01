package com.stormapi.scenario.repository;

import com.stormapi.scenario.model.TestScenario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Repository for {@link TestScenario} entities.
 */
public interface TestScenarioRepository extends JpaRepository<TestScenario, Long> {

    /** List all scenarios ordered by most recently created first. */
    List<TestScenario> findAllByOrderByCreatedAtDesc();

    /** Search scenarios by name (case-insensitive partial match). */
    List<TestScenario> findByNameContainingIgnoreCase(String name);

}
