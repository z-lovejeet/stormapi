package com.stormapi.test.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stormapi.test.dto.CreateTestRequest;
import com.stormapi.test.model.HttpMethod;
import com.stormapi.test.model.TestConfig;
import com.stormapi.test.model.TestResult;
import com.stormapi.test.model.TestStatus;
import com.stormapi.test.model.TestType;
import com.stormapi.test.repository.TestConfigRepository;
import com.stormapi.test.service.TestOrchestrator;
import com.stormapi.test.service.TestQueryService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TestController.class)
@DisplayName("TestController WebMvc Tests")
class TestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private TestOrchestrator orchestrator;

    @MockitoBean
    private TestQueryService queryService;

    @MockitoBean
    private TestConfigRepository testConfigRepository;

    @Test
    @DisplayName("POST /api/tests creates test and returns 201")
    void createTest_returns201() throws Exception {
        CreateTestRequest request = new CreateTestRequest(
                "Load Test", "desc", "https://api.example.com", HttpMethod.GET,
                null, null, TestType.LOAD, 50, 60, 10,
                null, null, null, 0, 5000, 0, false
        );

        TestConfig saved = buildConfig(1L);
        when(testConfigRepository.save(any())).thenReturn(saved);

        mockMvc.perform(post("/api/tests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.name").value("Load Test"));
    }

    @Test
    @DisplayName("POST /api/tests with missing name returns 400")
    void createTest_missingName_returns400() throws Exception {
        String body = """
                {
                    "targetUrl": "https://api.example.com",
                    "httpMethod": "GET",
                    "testType": "LOAD",
                    "virtualUsers": 50,
                    "durationSeconds": 60,
                    "rampUpSeconds": 10,
                    "timeoutMs": 5000
                }
                """;

        mockMvc.perform(post("/api/tests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.errorCode").value("VALIDATION_FAILED"));
    }

    @Test
    @DisplayName("GET /api/tests returns paginated list")
    void listTests_returnsPaginatedList() throws Exception {
        Page<TestConfig> configs = new PageImpl<>(List.of(buildConfig(1L)));
        when(queryService.listTests(any(), any(), any(Pageable.class))).thenReturn(configs);
        when(queryService.toSummaryPage(any())).thenReturn(configs.map(c ->
                new com.stormapi.test.dto.TestSummaryResponse(
                        c.getId(), c.getName(), c.getTargetUrl(), c.getTestType(),
                        c.getStatus(), c.getVirtualUsers(), c.getDurationSeconds(),
                        null, null, null, 0, c.getCreatedAt()
                )));

        mockMvc.perform(get("/api/tests"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray());
    }

    @Test
    @DisplayName("GET /api/tests/{id} returns test config")
    void getTest_returnsConfig() throws Exception {
        TestConfig config = buildConfig(1L);
        when(queryService.getTestConfig(1L)).thenReturn(config);

        mockMvc.perform(get("/api/tests/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.name").value("Load Test"));
    }

    @Test
    @DisplayName("GET /api/tests/{id}/result returns latest result")
    void getLatestResult_returnsResult() throws Exception {
        TestResult result = buildResult(10L);
        when(queryService.getLatestResult(1L)).thenReturn(result);

        mockMvc.perform(get("/api/tests/1/result"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(10))
                .andExpect(jsonPath("$.data.totalRequests").value(1000));
    }

    @Test
    @DisplayName("POST /api/tests/{id}/start returns 202 ACCEPTED")
    void startTest_returns202() throws Exception {
        when(orchestrator.startTest(1L)).thenReturn(10L);
        when(queryService.getLatestResult(1L)).thenReturn(buildResult(10L));

        mockMvc.perform(post("/api/tests/1/start"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("POST /api/tests/{id}/stop returns 200")
    void stopTest_returns200() throws Exception {
        mockMvc.perform(post("/api/tests/1/stop"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("DELETE /api/tests/{id} for non-running test returns 204")
    void deleteTest_nonRunning_returns204() throws Exception {
        when(orchestrator.isRunning(1L)).thenReturn(false);
        when(queryService.getTestConfig(1L)).thenReturn(buildConfig(1L));

        mockMvc.perform(delete("/api/tests/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("DELETE /api/tests/{id} for running test returns 409")
    void deleteTest_running_returns409() throws Exception {
        when(orchestrator.isRunning(1L)).thenReturn(true);

        mockMvc.perform(delete("/api/tests/1"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.errorCode").value("INVALID_STATE_TRANSITION"));
    }

    private TestConfig buildConfig(Long id) {
        TestConfig config = TestConfig.builder()
                .name("Load Test").targetUrl("https://api.example.com")
                .httpMethod(HttpMethod.GET).testType(TestType.LOAD)
                .virtualUsers(50).durationSeconds(60).rampUpSeconds(10)
                .status(TestStatus.CREATED).results(new ArrayList<>()).build();
        config.setId(id);
        config.setCreatedAt(Instant.now());
        config.setUpdatedAt(Instant.now());
        return config;
    }

    private TestResult buildResult(Long id) {
        TestResult result = TestResult.builder()
                .testConfig(buildConfig(1L)).status(TestStatus.RUNNING)
                .totalRequests(1000).successCount(950).failureCount(50)
                .avgResponseTimeMs(45.5).minResponseTimeMs(5).maxResponseTimeMs(500)
                .p50Ms(30).p75Ms(50).p90Ms(80).p95Ms(120).p99Ms(250)
                .throughputRps(16.7).errorRate(5).totalDataBytes(500000)
                .startedAt(Instant.now()).durationMs(0).build();
        result.setId(id);
        result.setCreatedAt(Instant.now());
        return result;
    }

}
