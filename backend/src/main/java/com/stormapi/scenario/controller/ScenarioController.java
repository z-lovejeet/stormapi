package com.stormapi.scenario.controller;

import com.stormapi.common.model.ApiResponse;
import com.stormapi.scenario.dto.AddStepRequest;
import com.stormapi.scenario.dto.CreateScenarioRequest;
import com.stormapi.scenario.dto.ReorderStepsRequest;
import com.stormapi.scenario.dto.ScenarioExecutionResponse;
import com.stormapi.scenario.dto.ScenarioResponse;
import com.stormapi.scenario.dto.StepResponse;
import com.stormapi.scenario.dto.UpdateScenarioRequest;
import com.stormapi.scenario.dto.UpdateStepRequest;
import com.stormapi.scenario.execution.ScenarioExecutor;
import com.stormapi.scenario.mapper.ScenarioMapper;
import com.stormapi.scenario.model.ScenarioStep;
import com.stormapi.scenario.model.TestScenario;
import com.stormapi.scenario.service.ScenarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST controller for test scenario management and execution.
 */
@RestController
@RequestMapping("/api/scenarios")
@Tag(name = "Scenarios", description = "Multi-step test scenario management and execution")
public class ScenarioController {

    private final ScenarioService scenarioService;
    private final ScenarioExecutor scenarioExecutor;

    public ScenarioController(ScenarioService scenarioService,
                              ScenarioExecutor scenarioExecutor) {
        this.scenarioService = scenarioService;
        this.scenarioExecutor = scenarioExecutor;
    }

    // ── Scenario CRUD ────────────────────────────────────────────

    @PostMapping
    @Operation(summary = "Create a new test scenario")
    public ResponseEntity<ApiResponse<ScenarioResponse>> createScenario(
            @Valid @RequestBody CreateScenarioRequest request,
            HttpServletRequest httpRequest) {

        TestScenario scenario = scenarioService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        ScenarioMapper.toResponse(scenario), httpRequest.getRequestURI()));
    }

    @GetMapping
    @Operation(summary = "List all test scenarios")
    public ResponseEntity<ApiResponse<List<ScenarioResponse>>> listScenarios(
            HttpServletRequest httpRequest) {

        List<ScenarioResponse> responses = scenarioService.listAll().stream()
                .map(ScenarioMapper::toResponse)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(responses, httpRequest.getRequestURI()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a test scenario with all steps")
    public ResponseEntity<ApiResponse<ScenarioResponse>> getScenario(
            @PathVariable Long id,
            HttpServletRequest httpRequest) {

        TestScenario scenario = scenarioService.getById(id);
        return ResponseEntity.ok(ApiResponse.success(
                ScenarioMapper.toResponse(scenario), httpRequest.getRequestURI()));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update scenario metadata")
    public ResponseEntity<ApiResponse<ScenarioResponse>> updateScenario(
            @PathVariable Long id,
            @Valid @RequestBody UpdateScenarioRequest request,
            HttpServletRequest httpRequest) {

        TestScenario scenario = scenarioService.update(id, request);
        return ResponseEntity.ok(ApiResponse.success(
                ScenarioMapper.toResponse(scenario), httpRequest.getRequestURI()));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a scenario and all its steps")
    public ResponseEntity<ApiResponse<Void>> deleteScenario(
            @PathVariable Long id,
            HttpServletRequest httpRequest) {

        scenarioService.delete(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT)
                .body(ApiResponse.success(httpRequest.getRequestURI()));
    }

    // ── Step Operations ──────────────────────────────────────────

    @PostMapping("/{scenarioId}/steps")
    @Operation(summary = "Add a step to a scenario")
    public ResponseEntity<ApiResponse<StepResponse>> addStep(
            @PathVariable Long scenarioId,
            @Valid @RequestBody AddStepRequest request,
            HttpServletRequest httpRequest) {

        ScenarioStep step = scenarioService.addStep(scenarioId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        ScenarioMapper.toStepResponse(step), httpRequest.getRequestURI()));
    }

    @PutMapping("/{scenarioId}/steps/{stepId}")
    @Operation(summary = "Update a scenario step")
    public ResponseEntity<ApiResponse<StepResponse>> updateStep(
            @PathVariable Long scenarioId,
            @PathVariable Long stepId,
            @Valid @RequestBody UpdateStepRequest request,
            HttpServletRequest httpRequest) {

        ScenarioStep step = scenarioService.updateStep(scenarioId, stepId, request);
        return ResponseEntity.ok(ApiResponse.success(
                ScenarioMapper.toStepResponse(step), httpRequest.getRequestURI()));
    }

    @DeleteMapping("/{scenarioId}/steps/{stepId}")
    @Operation(summary = "Remove a step from a scenario")
    public ResponseEntity<ApiResponse<Void>> deleteStep(
            @PathVariable Long scenarioId,
            @PathVariable Long stepId,
            HttpServletRequest httpRequest) {

        scenarioService.deleteStep(scenarioId, stepId);
        return ResponseEntity.status(HttpStatus.NO_CONTENT)
                .body(ApiResponse.success(httpRequest.getRequestURI()));
    }

    @PutMapping("/{scenarioId}/steps/reorder")
    @Operation(summary = "Reorder scenario steps")
    public ResponseEntity<ApiResponse<List<StepResponse>>> reorderSteps(
            @PathVariable Long scenarioId,
            @Valid @RequestBody ReorderStepsRequest request,
            HttpServletRequest httpRequest) {

        List<StepResponse> responses = scenarioService
                .reorderSteps(scenarioId, request).stream()
                .map(ScenarioMapper::toStepResponse)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(responses, httpRequest.getRequestURI()));
    }

    // ── Execution ────────────────────────────────────────────────

    @PostMapping("/{id}/execute")
    @Operation(summary = "Execute a scenario — runs all steps sequentially with variable chaining")
    public ResponseEntity<ApiResponse<ScenarioExecutionResponse>> executeScenario(
            @PathVariable Long id,
            HttpServletRequest httpRequest) {

        TestScenario scenario = scenarioService.getById(id);
        ScenarioExecutionResponse result = scenarioExecutor.execute(scenario);
        return ResponseEntity.ok(ApiResponse.success(result, httpRequest.getRequestURI()));
    }

}
