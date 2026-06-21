package com.stormapi.metrics.mapper;

import com.stormapi.metrics.dto.MetricSnapshotResponse;
import com.stormapi.metrics.dto.RequestLogResponse;
import com.stormapi.metrics.model.MetricSnapshot;
import com.stormapi.metrics.model.RequestLog;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("MetricsMapper Unit Tests")
class MetricsMapperTest {

    @Test
    @DisplayName("toResponse maps all MetricSnapshot fields")
    void toResponse_mapsAllFields() {
        MetricSnapshot snapshot = MetricSnapshot.builder()
                .timestamp(Instant.now())
                .activeUsers(50)
                .requestsPerSecond(100.5)
                .avgResponseTimeMs(45.2)
                .errorRate(2.5)
                .p95Ms(120.0)
                .cumulativeRequests(5000)
                .cumulativeErrors(125)
                .build();

        MetricSnapshotResponse response = MetricsMapper.toResponse(snapshot);

        assertEquals(snapshot.getTimestamp(), response.timestamp());
        assertEquals(50, response.activeUsers());
        assertEquals(100.5, response.requestsPerSecond());
        assertEquals(45.2, response.avgResponseTimeMs());
        assertEquals(2.5, response.errorRate());
        assertEquals(120.0, response.p95Ms());
        assertEquals(5000, response.cumulativeRequests());
        assertEquals(125, response.cumulativeErrors());
    }

    @Test
    @DisplayName("toResponseList maps all items")
    void toResponseList_mapsAll() {
        List<MetricSnapshot> snapshots = List.of(
                MetricSnapshot.builder().timestamp(Instant.now()).activeUsers(10)
                        .requestsPerSecond(50).avgResponseTimeMs(20).errorRate(0)
                        .p95Ms(30).cumulativeRequests(100).cumulativeErrors(0).build(),
                MetricSnapshot.builder().timestamp(Instant.now()).activeUsers(20)
                        .requestsPerSecond(100).avgResponseTimeMs(25).errorRate(1)
                        .p95Ms(40).cumulativeRequests(200).cumulativeErrors(2).build()
        );

        List<MetricSnapshotResponse> responses = MetricsMapper.toResponseList(snapshots);

        assertEquals(2, responses.size());
        assertEquals(10, responses.get(0).activeUsers());
        assertEquals(20, responses.get(1).activeUsers());
    }

    @Test
    @DisplayName("toLogResponse maps all RequestLog fields")
    void toLogResponse_mapsAllFields() {
        RequestLog log = RequestLog.builder()
                .timestamp(Instant.now())
                .url("https://api.example.com/test")
                .method("GET")
                .statusCode(200)
                .responseTimeMs(45)
                .responseSize(1024)
                .errorMessage(null)
                .success(true)
                .build();
        log.setId(5L);

        RequestLogResponse response = MetricsMapper.toLogResponse(log);

        assertEquals(5L, response.id());
        assertEquals("https://api.example.com/test", response.url());
        assertEquals("GET", response.method());
        assertEquals(200, response.statusCode());
        assertEquals(45, response.responseTimeMs());
        assertEquals(1024, response.responseSize());
        assertNull(response.errorMessage());
        assertTrue(response.success());
    }

}
