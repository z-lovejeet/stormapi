package com.stormapi.test.service;

import com.stormapi.common.exception.ResourceNotFoundException;
import com.stormapi.test.dto.ComparisonResponse;
import com.stormapi.test.dto.MetricDelta;
import com.stormapi.test.model.TestResult;
import com.stormapi.test.repository.TestResultRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ComparisonServiceTest {

    @Mock
    private TestResultRepository resultRepository;

    @InjectMocks
    private ComparisonService comparisonService;

    private TestResult buildResult(Long id, double avgMs, double throughput, double errorRate) {
        TestResult r = new TestResult();
        r.setId(id);
        r.setTotalRequests(1000);
        r.setSuccessCount(950);
        r.setFailureCount(50);
        r.setAvgResponseTimeMs(avgMs);
        r.setMinResponseTimeMs(10);
        r.setMaxResponseTimeMs(3000);
        r.setP50Ms(80);
        r.setP75Ms(120);
        r.setP90Ms(200);
        r.setP95Ms(300);
        r.setP99Ms(1000);
        r.setThroughputRps(throughput);
        r.setErrorRate(errorRate);
        r.setTotalDataBytes(5242880);
        return r;
    }

    @Test
    @DisplayName("compare returns correct deltas between two results")
    void compare_returnsDeltasCorrectly() {
        TestResult a = buildResult(1L, 150.0, 50.0, 5.0);
        TestResult b = buildResult(2L, 120.0, 65.0, 3.0);

        when(resultRepository.findById(1L)).thenReturn(Optional.of(a));
        when(resultRepository.findById(2L)).thenReturn(Optional.of(b));

        ComparisonResponse response = comparisonService.compare(1L, 2L);

        assertNotNull(response);
        assertNotNull(response.resultA());
        assertNotNull(response.resultB());
        assertFalse(response.deltas().isEmpty());

        // Throughput: 50→65, delta=15, improved=true (higher is better)
        MetricDelta throughputDelta = response.deltas().stream()
                .filter(d -> "throughputRps".equals(d.field()))
                .findFirst().orElseThrow();
        assertEquals(50.0, throughputDelta.resultA(), 0.001);
        assertEquals(65.0, throughputDelta.resultB(), 0.001);
        assertEquals(15.0, throughputDelta.delta(), 0.001);
        assertTrue(throughputDelta.improved());

        // Avg latency: 150→120, delta=-30, improved=true (lower is better)
        MetricDelta avgDelta = response.deltas().stream()
                .filter(d -> "avgResponseTimeMs".equals(d.field()))
                .findFirst().orElseThrow();
        assertEquals(-30.0, avgDelta.delta(), 0.001);
        assertTrue(avgDelta.improved());

        // Error rate: 5→3, delta=-2, improved=true (lower is better)
        MetricDelta errorDelta = response.deltas().stream()
                .filter(d -> "errorRate".equals(d.field()))
                .findFirst().orElseThrow();
        assertEquals(-2.0, errorDelta.delta(), 0.001);
        assertTrue(errorDelta.improved());
    }

    @Test
    @DisplayName("compare throws when result A not found")
    void compare_throwsWhenResultANotFound() {
        when(resultRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                comparisonService.compare(99L, 2L));
    }

    @Test
    @DisplayName("compare throws when result B not found")
    void compare_throwsWhenResultBNotFound() {
        TestResult a = buildResult(1L, 100.0, 50.0, 2.0);
        when(resultRepository.findById(1L)).thenReturn(Optional.of(a));
        when(resultRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                comparisonService.compare(1L, 99L));
    }

    @Test
    @DisplayName("compare handles zero values without division by zero")
    void compare_handlesZeroValues() {
        TestResult a = buildResult(1L, 0.0, 0.0, 0.0);
        TestResult b = buildResult(2L, 100.0, 50.0, 5.0);

        when(resultRepository.findById(1L)).thenReturn(Optional.of(a));
        when(resultRepository.findById(2L)).thenReturn(Optional.of(b));

        ComparisonResponse response = comparisonService.compare(1L, 2L);

        assertNotNull(response);
        // Should not throw or produce NaN
        for (MetricDelta delta : response.deltas()) {
            assertFalse(Double.isNaN(delta.deltaPercent()), "Delta percent should not be NaN for field: " + delta.field());
        }
    }
}
