package com.stormapi.test.controller;

import com.stormapi.common.exception.InvalidStateTransitionException;
import com.stormapi.common.model.ApiResponse;
import com.stormapi.test.dto.CreateTestRequest;
import com.stormapi.test.dto.TestConfigResponse;
import com.stormapi.test.dto.TestResultResponse;
import com.stormapi.test.dto.TestSummaryResponse;
import com.stormapi.test.mapper.TestMapper;
import com.stormapi.test.model.TestConfig;
import com.stormapi.test.model.TestResult;
import com.stormapi.test.model.TestStatus;
import com.stormapi.test.model.TestType;
import com.stormapi.test.repository.TestConfigRepository;
import com.stormapi.test.service.TestOrchestrator;
import com.stormapi.test.service.TestQueryService;
import com.stormapi.test.validation.TestConfigValidator;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for test configuration and execution management.
 * Handles CRUD, start/stop/rerun operations.
 */
@RestController
@RequestMapping("/api/tests")
@Tag(name = "Tests", description = "Test configuration and execution management")
public class TestController {

    private final TestOrchestrator orchestrator;
    private final TestQueryService queryService;
    private final TestConfigRepository testConfigRepository;

    public TestController(TestOrchestrator orchestrator,
                          TestQueryService queryService,
                          TestConfigRepository testConfigRepository) {
        this.orchestrator = orchestrator;
        this.queryService = queryService;
        this.testConfigRepository = testConfigRepository;
    }

    @PostMapping
    @Operation(summary = "Create a new test configuration and optionally start it")
    public ResponseEntity<ApiResponse<TestConfigResponse>> createTest(
            @Valid @RequestBody CreateTestRequest request,
            HttpServletRequest httpRequest) {

        TestConfigValidator.validate(request);
        TestConfig config = TestMapper.toEntity(request);
        config = testConfigRepository.save(config);

        if (request.autoStart()) {
            orchestrator.startTest(config.getId());
            config = queryService.getTestConfig(config.getId()); // refresh status
        }

        TestConfigResponse response = TestMapper.toResponse(config);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, httpRequest.getRequestURI()));
    }

    @GetMapping
    @Operation(summary = "List all tests with pagination and optional filters")
    public ResponseEntity<ApiResponse<Page<TestSummaryResponse>>> listTests(
            @RequestParam(required = false) TestStatus status,
            @RequestParam(required = false) TestType type,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable,
            HttpServletRequest httpRequest) {

        Page<TestConfig> configs = queryService.listTests(status, type, pageable);
        Page<TestSummaryResponse> summaries = queryService.toSummaryPage(configs);
        return ResponseEntity.ok(ApiResponse.success(summaries, httpRequest.getRequestURI()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get test configuration by ID")
    public ResponseEntity<ApiResponse<TestConfigResponse>> getTest(
            @PathVariable Long id,
            HttpServletRequest httpRequest) {

        TestConfig config = queryService.getTestConfig(id);
        return ResponseEntity.ok(ApiResponse.success(
                TestMapper.toResponse(config), httpRequest.getRequestURI()));
    }

    @GetMapping("/{id}/result")
    @Operation(summary = "Get the latest execution result for a test")
    public ResponseEntity<ApiResponse<TestResultResponse>> getLatestResult(
            @PathVariable Long id,
            HttpServletRequest httpRequest) {

        TestResult result = queryService.getLatestResult(id);
        return ResponseEntity.ok(ApiResponse.success(
                TestMapper.toResultResponse(result), httpRequest.getRequestURI()));
    }

    @GetMapping("/{id}/results")
    @Operation(summary = "Get all execution results for a test (re-runs)")
    public ResponseEntity<ApiResponse<Page<TestResultResponse>>> getResults(
            @PathVariable Long id,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable,
            HttpServletRequest httpRequest) {

        Page<TestResult> results = queryService.getResults(id, pageable);
        Page<TestResultResponse> page = results.map(TestMapper::toResultResponse);
        return ResponseEntity.ok(ApiResponse.success(page, httpRequest.getRequestURI()));
    }

    @PostMapping("/{id}/start")
    @Operation(summary = "Start executing a test")
    public ResponseEntity<ApiResponse<TestResultResponse>> startTest(
            @PathVariable Long id,
            HttpServletRequest httpRequest) {

        Long resultId = orchestrator.startTest(id);
        // Small delay to allow status propagation
        TestResult result = queryService.getLatestResult(id);
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(ApiResponse.success(TestMapper.toResultResponse(result), httpRequest.getRequestURI()));
    }

    @PostMapping("/{id}/stop")
    @Operation(summary = "Stop a running test gracefully")
    public ResponseEntity<ApiResponse<Void>> stopTest(
            @PathVariable Long id,
            HttpServletRequest httpRequest) {

        orchestrator.stopTest(id);
        return ResponseEntity.ok(ApiResponse.success(httpRequest.getRequestURI()));
    }

    @PostMapping("/{id}/rerun")
    @Operation(summary = "Re-run a test with the same configuration")
    public ResponseEntity<ApiResponse<TestResultResponse>> rerunTest(
            @PathVariable Long id,
            HttpServletRequest httpRequest) {

        Long resultId = orchestrator.startTest(id);
        TestResult result = queryService.getLatestResult(id);
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(ApiResponse.success(TestMapper.toResultResponse(result), httpRequest.getRequestURI()));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete test and all associated results")
    public ResponseEntity<ApiResponse<Void>> deleteTest(
            @PathVariable Long id,
            HttpServletRequest httpRequest) {

        if (orchestrator.isRunning(id)) {
            throw new InvalidStateTransitionException("Cannot delete test " + id + " while it is running");
        }
        TestConfig config = queryService.getTestConfig(id);
        testConfigRepository.delete(config);
        return ResponseEntity.status(HttpStatus.NO_CONTENT)
                .body(ApiResponse.success(httpRequest.getRequestURI()));
    }

}
