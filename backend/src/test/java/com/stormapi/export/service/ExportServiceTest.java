package com.stormapi.export.service;

import com.stormapi.common.exception.ResourceNotFoundException;
import com.stormapi.metrics.model.MetricSnapshot;
import com.stormapi.metrics.repository.MetricSnapshotRepository;
import com.stormapi.test.model.HttpMethod;
import com.stormapi.test.model.TestConfig;
import com.stormapi.test.model.TestResult;
import com.stormapi.test.model.TestStatus;
import com.stormapi.test.model.TestType;
import com.stormapi.test.repository.TestResultRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ExportService Unit Tests")
class ExportServiceTest {

    @Mock
    private TestResultRepository testResultRepository;

    @Mock
    private MetricSnapshotRepository metricSnapshotRepository;

    private ExportService exportService;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        exportService = new ExportService(testResultRepository, metricSnapshotRepository, objectMapper);
    }

    @Test
    @DisplayName("exportJson writes valid JSON to output stream")
    void exportJson_writesValidJson() throws IOException {
        when(testResultRepository.findById(1L)).thenReturn(Optional.of(buildResult()));
        when(metricSnapshotRepository.findByTestResultIdOrderByTimestampAsc(1L))
                .thenReturn(List.of(buildSnapshot()));

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        exportService.exportJson(1L, os);

        String json = os.toString();
        assertTrue(json.contains("\"result\""));
        assertTrue(json.contains("\"metricSnapshots\""));
        assertTrue(json.contains("\"activeUsers\""));
    }

    @Test
    @DisplayName("exportCsv writes CSV with header and data rows")
    void exportCsv_writesValidCsv() {
        when(testResultRepository.findById(1L)).thenReturn(Optional.of(buildResult()));
        when(metricSnapshotRepository.findByTestResultIdOrderByTimestampAsc(1L))
                .thenReturn(List.of(buildSnapshot()));

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        exportService.exportCsv(1L, os);

        String csv = os.toString();
        assertTrue(csv.startsWith("timestamp,active_users,"));
        String[] lines = csv.split("\n");
        assertEquals(2, lines.length); // header + 1 data row
    }

    @Test
    @DisplayName("exportJson throws when result not found")
    void exportJson_notFound_throws() {
        when(testResultRepository.findById(99L)).thenReturn(Optional.empty());
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        assertThrows(ResourceNotFoundException.class,
                () -> exportService.exportJson(99L, os));
    }

    @Test
    @DisplayName("exportCsv throws when result not found")
    void exportCsv_notFound_throws() {
        when(testResultRepository.findById(99L)).thenReturn(Optional.empty());
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        assertThrows(ResourceNotFoundException.class,
                () -> exportService.exportCsv(99L, os));
    }

    private TestResult buildResult() {
        TestConfig config = TestConfig.builder()
                .name("Test").targetUrl("https://api.example.com")
                .httpMethod(HttpMethod.GET).testType(TestType.LOAD)
                .virtualUsers(50).durationSeconds(60).status(TestStatus.COMPLETED)
                .results(new ArrayList<>()).build();
        config.setId(1L);
        config.setCreatedAt(Instant.now());

        TestResult result = TestResult.builder()
                .testConfig(config).status(TestStatus.COMPLETED)
                .totalRequests(1000).successCount(950).failureCount(50)
                .avgResponseTimeMs(45.5).minResponseTimeMs(5).maxResponseTimeMs(500)
                .p50Ms(30).p75Ms(50).p90Ms(80).p95Ms(120).p99Ms(250)
                .throughputRps(16.7).errorRate(5).totalDataBytes(500000)
                .startedAt(Instant.now().minusSeconds(60)).completedAt(Instant.now())
                .durationMs(60000).build();
        result.setId(1L);
        result.setCreatedAt(Instant.now());
        return result;
    }

    private MetricSnapshot buildSnapshot() {
        return MetricSnapshot.builder()
                .timestamp(Instant.now()).activeUsers(50).requestsPerSecond(100)
                .avgResponseTimeMs(45).errorRate(2.5).p95Ms(120)
                .cumulativeRequests(5000).cumulativeErrors(125).build();
    }

}
