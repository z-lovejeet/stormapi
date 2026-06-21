package com.stormapi.metrics.controller;

import com.stormapi.common.exception.ResourceNotFoundException;
import com.stormapi.common.model.ApiResponse;
import com.stormapi.metrics.dto.MetricSnapshotResponse;
import com.stormapi.metrics.dto.RequestLogResponse;
import com.stormapi.metrics.mapper.MetricsMapper;
import com.stormapi.metrics.model.MetricSnapshot;
import com.stormapi.metrics.model.RequestLog;
import com.stormapi.metrics.repository.MetricSnapshotRepository;
import com.stormapi.metrics.repository.RequestLogRepository;
import com.stormapi.test.repository.TestResultRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST controller for metrics data — time-series snapshots and request logs.
 */
@RestController
@RequestMapping("/api/metrics")
@Tag(name = "Metrics", description = "Test execution metrics and request logs")
public class MetricsController {

    private final MetricSnapshotRepository snapshotRepository;
    private final RequestLogRepository requestLogRepository;
    private final TestResultRepository testResultRepository;

    public MetricsController(MetricSnapshotRepository snapshotRepository,
                             RequestLogRepository requestLogRepository,
                             TestResultRepository testResultRepository) {
        this.snapshotRepository = snapshotRepository;
        this.requestLogRepository = requestLogRepository;
        this.testResultRepository = testResultRepository;
    }

    @GetMapping("/{resultId}/snapshots")
    @Operation(summary = "Get time-series metric snapshots for chart visualization")
    public ResponseEntity<ApiResponse<List<MetricSnapshotResponse>>> getSnapshots(
            @PathVariable Long resultId,
            HttpServletRequest httpRequest) {

        verifyResultExists(resultId);
        List<MetricSnapshot> snapshots = snapshotRepository
                .findByTestResultIdOrderByTimestampAsc(resultId);
        return ResponseEntity.ok(ApiResponse.success(
                MetricsMapper.toResponseList(snapshots), httpRequest.getRequestURI()));
    }

    @GetMapping("/{resultId}/request-logs")
    @Operation(summary = "Get paginated request logs for drill-down analysis")
    public ResponseEntity<ApiResponse<Page<RequestLogResponse>>> getRequestLogs(
            @PathVariable Long resultId,
            @RequestParam(required = false) Boolean success,
            @PageableDefault(size = 50) Pageable pageable,
            HttpServletRequest httpRequest) {

        verifyResultExists(resultId);

        Page<RequestLog> logs;
        if (success != null) {
            logs = requestLogRepository.findByTestResultIdAndSuccessOrderByTimestampAsc(
                    resultId, success, pageable);
        } else {
            logs = requestLogRepository.findByTestResultIdOrderByTimestampAsc(resultId, pageable);
        }

        Page<RequestLogResponse> page = logs.map(MetricsMapper::toLogResponse);
        return ResponseEntity.ok(ApiResponse.success(page, httpRequest.getRequestURI()));
    }

    private void verifyResultExists(Long resultId) {
        if (!testResultRepository.existsById(resultId)) {
            throw new ResourceNotFoundException("TestResult", resultId);
        }
    }

}
