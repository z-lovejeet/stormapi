package com.stormapi.engine.data;

import com.stormapi.common.model.ApiResponse;
import com.stormapi.engine.assertion.Assertion;
import com.stormapi.engine.assertion.AssertionEvaluator;
import com.stormapi.scenario.model.TestScenario;
import com.stormapi.scenario.service.ScenarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * REST controller for data-driven (parameterized) test execution.
 *
 * <p>Endpoint: {@code POST /api/data-driven/execute}
 */
@RestController
@RequestMapping("/api/data-driven")
@Tag(name = "Data-Driven Testing", description = "Parameterized test execution with CSV/JSON data")
public class DataDrivenController {

    private static final Logger log = LoggerFactory.getLogger(DataDrivenController.class);

    private final DataDrivenExecutor dataDrivenExecutor;
    private final ScenarioService scenarioService;

    public DataDrivenController(DataDrivenExecutor dataDrivenExecutor,
                                ScenarioService scenarioService) {
        this.dataDrivenExecutor = dataDrivenExecutor;
        this.scenarioService = scenarioService;
    }

    @PostMapping("/execute")
    @Operation(summary = "Execute scenario with parameterized data",
            description = "Runs a scenario once per data row (CSV or JSON), with optional assertions")
    public ResponseEntity<ApiResponse<DataDrivenExecutionResponse>> execute(
            @Valid @RequestBody DataDrivenRequest request,
            HttpServletRequest httpRequest) {

        log.info("Data-driven execution request for scenario {} with format {}",
                request.scenarioId(), request.format());

        // 1. Load scenario
        TestScenario scenario = scenarioService.getById(request.scenarioId());

        // 2. Parse data
        DataReader reader = createReader(request.format());
        List<Map<String, String>> rows = reader.read(request.dataContent());

        if (rows.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(400, "Bad Request",
                            "Data contains no rows", "EMPTY_DATA",
                            httpRequest.getRequestURI()));
        }

        // 3. Create assertions from definitions
        List<Assertion> assertions = request.assertions() != null
                ? AssertionEvaluator.createAssertions(request.assertions())
                : List.of();

        // 4. Execute
        DataDrivenExecutionResponse response =
                dataDrivenExecutor.execute(scenario, rows, assertions);

        return ResponseEntity.ok(ApiResponse.success(response, httpRequest.getRequestURI()));
    }

    /**
     * Factory for data readers based on format string.
     */
    private DataReader createReader(String format) {
        return switch (format.toUpperCase()) {
            case "CSV" -> new CsvDataReader();
            case "JSON" -> new JsonDataReader();
            default -> throw new IllegalArgumentException(
                    "Unsupported data format: " + format + ". Supported: CSV, JSON");
        };
    }
}
