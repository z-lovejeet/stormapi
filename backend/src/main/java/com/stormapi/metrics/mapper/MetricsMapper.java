package com.stormapi.metrics.mapper;

import com.stormapi.metrics.dto.MetricSnapshotResponse;
import com.stormapi.metrics.dto.RequestLogResponse;
import com.stormapi.metrics.model.MetricSnapshot;
import com.stormapi.metrics.model.RequestLog;

import java.util.List;

/**
 * Manual mapper for metrics entities.
 */
public final class MetricsMapper {

    private MetricsMapper() {}

    public static MetricSnapshotResponse toResponse(MetricSnapshot snapshot) {
        return new MetricSnapshotResponse(
                snapshot.getTimestamp(),
                snapshot.getActiveUsers(),
                snapshot.getRequestsPerSecond(),
                snapshot.getAvgResponseTimeMs(),
                snapshot.getErrorRate(),
                snapshot.getP95Ms(),
                snapshot.getCumulativeRequests(),
                snapshot.getCumulativeErrors()
        );
    }

    public static List<MetricSnapshotResponse> toResponseList(List<MetricSnapshot> snapshots) {
        return snapshots.stream()
                .map(MetricsMapper::toResponse)
                .toList();
    }

    public static RequestLogResponse toLogResponse(RequestLog log) {
        return new RequestLogResponse(
                log.getId(),
                log.getTimestamp(),
                log.getUrl(),
                log.getMethod(),
                log.getStatusCode(),
                log.getResponseTimeMs(),
                log.getResponseSize(),
                log.getErrorMessage(),
                log.isSuccess()
        );
    }

}
