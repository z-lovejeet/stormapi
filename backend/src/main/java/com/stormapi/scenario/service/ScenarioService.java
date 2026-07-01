package com.stormapi.scenario.service;

import com.stormapi.collection.dto.KeyValuePairDto;
import com.stormapi.collection.model.KeyValuePair;
import com.stormapi.common.exception.ResourceNotFoundException;
import com.stormapi.scenario.dto.AddStepRequest;
import com.stormapi.scenario.dto.CreateScenarioRequest;
import com.stormapi.scenario.dto.ReorderStepsRequest;
import com.stormapi.scenario.dto.UpdateScenarioRequest;
import com.stormapi.scenario.dto.UpdateStepRequest;
import com.stormapi.scenario.mapper.ScenarioMapper;
import com.stormapi.scenario.model.ScenarioStep;
import com.stormapi.scenario.model.TestScenario;
import com.stormapi.scenario.repository.ScenarioStepRepository;
import com.stormapi.scenario.repository.TestScenarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Service for scenario CRUD and step management operations.
 *
 * <p>Step ordering uses 0-indexed {@code stepOrder} values. When steps are
 * added, they receive the next sequential order. When steps are deleted,
 * remaining steps are recompacted. Explicit reordering is supported via
 * {@link #reorderSteps}.</p>
 */
@Service
@Transactional
public class ScenarioService {

    private static final Logger log = LoggerFactory.getLogger(ScenarioService.class);

    private final TestScenarioRepository scenarioRepository;
    private final ScenarioStepRepository stepRepository;

    public ScenarioService(TestScenarioRepository scenarioRepository,
                           ScenarioStepRepository stepRepository) {
        this.scenarioRepository = scenarioRepository;
        this.stepRepository = stepRepository;
    }

    // ── Scenario CRUD ────────────────────────────────────────────

    public TestScenario create(CreateScenarioRequest request) {
        TestScenario scenario = ScenarioMapper.toEntity(request);
        return scenarioRepository.save(scenario);
    }

    @Transactional(readOnly = true)
    public TestScenario getById(Long id) {
        return scenarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("TestScenario", id));
    }

    @Transactional(readOnly = true)
    public List<TestScenario> listAll() {
        return scenarioRepository.findAllByOrderByCreatedAtDesc();
    }

    public TestScenario update(Long id, UpdateScenarioRequest request) {
        TestScenario scenario = getById(id);
        scenario.setName(request.name());
        scenario.setDescription(request.description());
        scenario.setFailFast(request.isFailFast());
        return scenarioRepository.save(scenario);
    }

    public void delete(Long id) {
        if (!scenarioRepository.existsById(id)) {
            throw new ResourceNotFoundException("TestScenario", id);
        }
        scenarioRepository.deleteById(id);
    }

    // ── Step Operations ──────────────────────────────────────────

    /**
     * Adds a new step at the end of the scenario's step list.
     */
    public ScenarioStep addStep(Long scenarioId, AddStepRequest request) {
        TestScenario scenario = getById(scenarioId);
        int nextOrder = stepRepository.countByScenarioId(scenarioId);
        ScenarioStep step = ScenarioMapper.toStepEntity(request, scenario, nextOrder);
        return stepRepository.save(step);
    }

    /**
     * Updates an existing step within a scenario.
     */
    public ScenarioStep updateStep(Long scenarioId, Long stepId, UpdateStepRequest request) {
        ScenarioStep step = getStep(scenarioId, stepId);
        step.setName(request.name());
        step.setUrl(request.url());
        step.setMethod(request.method());
        step.setBody(request.body());
        step.setExtractionRulesJson(
                ScenarioMapper.serializeExtractionRules(request.extractionRules()));
        if (request.headers() != null) {
            step.setHeaders(request.headers().stream()
                    .map(dto -> new KeyValuePair(dto.key(), dto.value()))
                    .toList());
        }
        return stepRepository.save(step);
    }

    /**
     * Deletes a step and recompacts the ordering of remaining steps.
     */
    public void deleteStep(Long scenarioId, Long stepId) {
        ScenarioStep step = getStep(scenarioId, stepId);
        stepRepository.delete(step);
        recompactStepOrders(scenarioId);
    }

    /**
     * Reorders steps to match the provided ID sequence.
     * All step IDs must belong to the specified scenario.
     *
     * @throws IllegalArgumentException if IDs don't match the scenario's steps
     */
    public List<ScenarioStep> reorderSteps(Long scenarioId, ReorderStepsRequest request) {
        List<ScenarioStep> existingSteps =
                stepRepository.findByScenarioIdOrderByStepOrderAsc(scenarioId);

        Set<Long> existingIds = existingSteps.stream()
                .map(ScenarioStep::getId)
                .collect(Collectors.toSet());
        Set<Long> requestedIds = new HashSet<>(request.stepIds());

        if (!existingIds.equals(requestedIds)) {
            throw new IllegalArgumentException(
                    "Step IDs do not match scenario steps. Expected: " + existingIds
                            + ", received: " + requestedIds);
        }

        // Build lookup and assign new order
        Map<Long, ScenarioStep> lookup = existingSteps.stream()
                .collect(Collectors.toMap(ScenarioStep::getId, Function.identity()));

        for (int i = 0; i < request.stepIds().size(); i++) {
            ScenarioStep step = lookup.get(request.stepIds().get(i));
            step.setStepOrder(i);
        }

        return stepRepository.saveAll(existingSteps);
    }

    // ── Helpers ──────────────────────────────────────────────────

    private ScenarioStep getStep(Long scenarioId, Long stepId) {
        if (!scenarioRepository.existsById(scenarioId)) {
            throw new ResourceNotFoundException("TestScenario", scenarioId);
        }
        return stepRepository.findByIdAndScenarioId(stepId, scenarioId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "ScenarioStep " + stepId
                                + " does not belong to scenario " + scenarioId));
    }

    /**
     * Recompacts step orders after a deletion to avoid gaps.
     */
    private void recompactStepOrders(Long scenarioId) {
        List<ScenarioStep> steps =
                stepRepository.findByScenarioIdOrderByStepOrderAsc(scenarioId);
        for (int i = 0; i < steps.size(); i++) {
            if (steps.get(i).getStepOrder() != i) {
                steps.get(i).setStepOrder(i);
            }
        }
        stepRepository.saveAll(steps);
    }

}
