package com.stormapi.metrics.controller;

import com.stormapi.common.exception.ResourceNotFoundException;
import com.stormapi.metrics.model.MetricSnapshot;
import com.stormapi.metrics.model.RequestLog;
import com.stormapi.metrics.repository.MetricSnapshotRepository;
import com.stormapi.metrics.repository.RequestLogRepository;
import com.stormapi.test.repository.TestResultRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(MetricsController.class)
@DisplayName("MetricsController WebMvc Tests")
class MetricsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MetricSnapshotRepository snapshotRepository;

    @MockitoBean
    private RequestLogRepository requestLogRepository;

    @MockitoBean
    private TestResultRepository testResultRepository;

    @Test
    @DisplayName("GET /api/metrics/{resultId}/snapshots returns snapshot list")
    void getSnapshots_returnsList() throws Exception {
        when(testResultRepository.existsById(1L)).thenReturn(true);
        MetricSnapshot snapshot = MetricSnapshot.builder()
                .timestamp(Instant.now()).activeUsers(10).requestsPerSecond(50)
                .avgResponseTimeMs(20).errorRate(0).p95Ms(30)
                .cumulativeRequests(100).cumulativeErrors(0).build();
        when(snapshotRepository.findByTestResultIdOrderByTimestampAsc(1L))
                .thenReturn(List.of(snapshot));

        mockMvc.perform(get("/api/metrics/1/snapshots"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].activeUsers").value(10));
    }

    @Test
    @DisplayName("GET /api/metrics/{resultId}/snapshots for non-existent result returns 404")
    void getSnapshots_notFound_returns404() throws Exception {
        when(testResultRepository.existsById(99L)).thenReturn(false);

        mockMvc.perform(get("/api/metrics/99/snapshots"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.errorCode").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    @DisplayName("GET /api/metrics/{resultId}/request-logs returns paginated logs")
    void getRequestLogs_returnsPaginated() throws Exception {
        when(testResultRepository.existsById(1L)).thenReturn(true);
        RequestLog log = RequestLog.builder()
                .timestamp(Instant.now()).url("https://api.example.com")
                .method("GET").statusCode(200).responseTimeMs(45)
                .responseSize(1024).success(true).build();
        log.setId(5L);
        when(requestLogRepository.findByTestResultIdOrderByTimestampAsc(eq(1L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(log)));

        mockMvc.perform(get("/api/metrics/1/request-logs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content[0].statusCode").value(200));
    }

    @Test
    @DisplayName("GET /api/metrics/{resultId}/request-logs with success filter")
    void getRequestLogs_withSuccessFilter() throws Exception {
        when(testResultRepository.existsById(1L)).thenReturn(true);
        when(requestLogRepository.findByTestResultIdAndSuccessOrderByTimestampAsc(
                eq(1L), eq(false), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/api/metrics/1/request-logs?success=false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray());
    }

}
